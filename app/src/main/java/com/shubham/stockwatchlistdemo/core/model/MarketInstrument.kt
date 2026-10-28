package com.shubham.stockwatchlistdemo.core.model

/**
 * Represents a traded market instrument in the watchlist.
 *
 * @property id Unique stable identifier (e.g., "BTCUSDT").
 * @property symbol Display symbol (e.g., "BTC/USDT").
 * @property name Human-readable asset name (e.g., "Bitcoin").
 * @property baseAsset Base cryptocurrency/stock asset (e.g., "BTC").
 * @property quoteAsset Quote currency (e.g., "USDT").
 * @property decimalPlaces Number of decimal digits appropriate for this instrument's price scale.
 */
data class MarketInstrument(
    val id: String,
    val symbol: String,
    val name: String,
    val baseAsset: String,
    val quoteAsset: String = "USDT",
    val decimalPlaces: Int = 2
)
