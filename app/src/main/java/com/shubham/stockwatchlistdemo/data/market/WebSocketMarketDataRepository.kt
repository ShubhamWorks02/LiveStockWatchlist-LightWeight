package com.shubham.stockwatchlistdemo.data.market

import com.shubham.stockwatchlistdemo.core.model.MarketConnectionState
import com.shubham.stockwatchlistdemo.core.model.MarketInstrument
import com.shubham.stockwatchlistdemo.core.model.MarketPriceUpdate
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.mapNotNull

/**
 * Production implementation of [MarketDataRepository].
 * Coordinates low-level [MarketDataDataSource] and [MarketDataMapper] off the main thread.
 */
class WebSocketMarketDataRepository(
    private val dataSource: MarketDataDataSource,
    private val mapper: MarketDataMapper = MarketDataMapper(),
    private val instruments: List<MarketInstrument> = DefaultInstruments.items,
    private val defaultDispatcher: CoroutineDispatcher = Dispatchers.Default
) : MarketDataRepository {

    override val connectionState: Flow<MarketConnectionState> = dataSource.connectionState

    override fun getInstruments(): List<MarketInstrument> = instruments

    override fun observePriceUpdates(): Flow<MarketPriceUpdate> {
        return dataSource.incomingMessages
            .mapNotNull { rawText ->
                mapper.parsePriceUpdate(rawText)
            }
            .flowOn(defaultDispatcher)
    }

    override fun startObserving() {
        dataSource.connect(instruments)
    }

    override fun stopObserving() {
        dataSource.disconnect()
    }

    override fun retryConnection() {
        dataSource.reconnect(instruments)
    }
}
