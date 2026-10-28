package com.shubham.stockwatchlistdemo.data.market

import com.shubham.stockwatchlistdemo.core.model.MarketPriceUpdate
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale

@Serializable
internal data class BinanceCombinedPayload(
    val stream: String? = null,
    val data: BinanceTradeData? = null
)

@Serializable
internal data class BinanceTradeData(
    @SerialName("e") val eventType: String? = null, // event type: "trade"
    @SerialName("E") val eventTime: Long = 0L,      // event time
    @SerialName("s") val symbol: String? = null,    // symbol: "BTCUSDT"
    @SerialName("t") val tradeId: Long = 0L,        // trade ID
    @SerialName("p") val price: String? = null,     // price
    @SerialName("q") val quantity: String? = null,  // quantity
    @SerialName("T") val tradeTime: Long = 0L       // trade time
)

/**
 * Handles JSON parsing and mapping from raw WebSocket messages to domain [MarketPriceUpdate].
 * Isolated strictly within the data layer.
 */
class MarketDataMapper(
    private val json: Json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }
) {

    /**
     * Parses an incoming raw WebSocket text payload.
     * Returns null if the payload is an ACK, ping/pong, heartbeat, malformed, or missing required price fields.
     */
    fun parsePriceUpdate(rawText: String): MarketPriceUpdate? {
        if (rawText.isBlank()) return null

        return try {
            // Check for combined stream wrapper first: {"stream":"...","data":{...}}
            val combined = json.decodeFromString<BinanceCombinedPayload>(rawText)
            val trade = combined.data ?: json.decodeFromString<BinanceTradeData>(rawText)

            val symbol = trade.symbol?.uppercase(Locale.US)?.trim() ?: return null
            val priceStr = trade.price?.trim() ?: return null
            val price = priceStr.toDoubleOrNull() ?: return null

            // Prices in financial markets cannot be negative or zero
            if (price <= 0.0 || price.isNaN() || price.isInfinite()) {
                return null
            }

            val quantity = trade.quantity?.toDoubleOrNull() ?: 0.0
            val timestamp = if (trade.tradeTime > 0L) trade.tradeTime else System.currentTimeMillis()

            MarketPriceUpdate(
                instrumentId = symbol,
                price = price,
                quantity = quantity,
                timestamp = timestamp
            )
        } catch (_: Exception) {
            // Malformed JSON, subscription acks, or other non-trade payloads safely return null
            null
        }
    }

    companion object {
        /**
         * Cached DecimalFormats to prevent garbage collector pressure during high frequency updates.
         */
        private val formatters = (0..6).associateWith { decimals ->
            val pattern = if (decimals == 0) "#,##0" else "#,##0." + "0".repeat(decimals)
            DecimalFormat(pattern, DecimalFormatSymbols(Locale.US)).apply {
                isGroupingUsed = true
            }
        }

        fun formatPrice(price: Double, decimals: Int): String {
            val formatter = formatters[decimals] ?: formatters[2]!!
            return synchronized(formatter) {
                "$" + formatter.format(price)
            }
        }

        fun formatPercentChange(changePercent: Double): String {
            val prefix = if (changePercent > 0) "+" else ""
            val formatted = String.format(Locale.US, "%.2f%%", changePercent)
            return "$prefix$formatted"
        }
    }
}
