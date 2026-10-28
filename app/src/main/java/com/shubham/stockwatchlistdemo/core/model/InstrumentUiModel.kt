package com.shubham.stockwatchlistdemo.core.model

import androidx.compose.runtime.Immutable

/**
 * Immutable, presentation-ready model representing an instrument row in the watchlist.
 *
 * Pre-computes formatted strings to prevent repetitive allocations and string manipulations
 * on the UI thread during Compose recomposition passes.
 *
 * @property id Stable unique identifier (e.g., "BTCUSDT").
 * @property symbol Formatted trading pair symbol (e.g., "BTC/USDT").
 * @property name Human-friendly instrument name (e.g., "Bitcoin").
 * @property baseAsset Base cryptocurrency ticker (e.g., "BTC").
 * @property quoteAsset Quote currency ticker (e.g., "USDT").
 * @property formattedPrice Pre-formatted localized price string (e.g., "$68,542.10").
 * @property rawPrice Exact numeric price for comparisons and calculations.
 * @property previousPrice Price immediately prior to the current update; null if first baseline.
 * @property direction Latest price movement direction ([PriceDirection.UP], [PriceDirection.DOWN], or [PriceDirection.UNCHANGED]).
 * @property formattedChangePercent Pre-formatted percentage change since initial baseline (e.g., "+1.42%").
 * @property isPositiveChange True if net change >= 0, false if negative, null if no baseline yet.
 * @property highPriceFormatted Formatted highest price observed during session.
 * @property lowPriceFormatted Formatted lowest price observed during session.
 * @property updateCounter Monotonically increasing counter incremented solely when a price movement occurs.
 *                         Used as a Compose animation trigger key for consecutive movements in the same direction.
 * @property lastUpdatedTimestamp Epoch millisecond timestamp of the latest received trade.
 */
@Immutable
data class InstrumentUiModel(
    val id: String,
    val symbol: String,
    val name: String,
    val baseAsset: String,
    val quoteAsset: String = "USDT",
    val formattedPrice: String,
    val rawPrice: Double,
    val previousPrice: Double? = null,
    val direction: PriceDirection = PriceDirection.UNCHANGED,
    val formattedChangePercent: String = "0.00%",
    val isPositiveChange: Boolean? = null,
    val highPriceFormatted: String = "",
    val lowPriceFormatted: String = "",
    val updateCounter: Long = 0L,
    val lastUpdatedTimestamp: Long = 0L
)
