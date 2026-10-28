package com.shubham.stockwatchlistdemo.core.model

/**
 * Domain-level immutable price tick received from the market-data feed.
 *
 * @property instrumentId Target instrument ID (e.g. "BTCUSDT").
 * @property price Current executed trade price.
 * @property quantity Executed trade quantity.
 * @property timestamp Epoch timestamp (milliseconds) of the trade execution.
 */
data class MarketPriceUpdate(
    val instrumentId: String,
    val price: Double,
    val quantity: Double = 0.0,
    val timestamp: Long = System.currentTimeMillis()
)
