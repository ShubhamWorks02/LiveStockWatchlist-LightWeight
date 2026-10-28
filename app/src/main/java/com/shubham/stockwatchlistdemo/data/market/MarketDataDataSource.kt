package com.shubham.stockwatchlistdemo.data.market

import com.shubham.stockwatchlistdemo.core.model.MarketConnectionState
import com.shubham.stockwatchlistdemo.core.model.MarketInstrument
import kotlinx.coroutines.flow.Flow

/**
 * Low-level data source interface abstracting the market data transport layer (WebSocket, mock, or SSE).
 * Enables easy swapping of WebSocket vendors (e.g. Binance, Alpaca, IEX, or synthetic test sources).
 */
interface MarketDataDataSource {
    /** Stream of raw string messages received from the server. */
    val incomingMessages: Flow<String>

    /** Stream of connection lifecycle states. */
    val connectionState: Flow<MarketConnectionState>

    /** Initiates connection and stream subscription for the provided instruments. */
    fun connect(instruments: List<MarketInstrument>)

    /** Disconnects the socket and releases network resources cleanly. */
    fun disconnect()

    /** Forces a manual reconnection attempt, resetting retry backoff counters. */
    fun reconnect(instruments: List<MarketInstrument>)
}
