package com.shubham.stockwatchlistdemo.data.market

import com.shubham.stockwatchlistdemo.core.model.MarketConnectionState
import com.shubham.stockwatchlistdemo.core.model.MarketInstrument
import com.shubham.stockwatchlistdemo.core.model.MarketPriceUpdate
import kotlinx.coroutines.flow.Flow

/**
 * Repository interface exposing market data streams and connection state.
 * Encapsulates data source specifics, parsing, and caching.
 */
interface MarketDataRepository {
    /** Reactive stream of connection lifecycle states. */
    val connectionState: Flow<MarketConnectionState>

    /** Returns the static or configured watchlist instruments. */
    fun getInstruments(): List<MarketInstrument>

    /** Reactive stream of parsed and validated market price updates. */
    fun observePriceUpdates(): Flow<MarketPriceUpdate>

    /** Initiates connection to the market data feed. */
    fun startObserving()

    /** Shuts down the feed connection when observation ends. */
    fun stopObserving()

    /** User-invoked retry when the connection encounters an error. */
    fun retryConnection()
}
