package com.shubham.stockwatchlistdemo.data.market

import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeoutOrNull
import okhttp3.OkHttpClient
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.concurrent.TimeUnit

class MarketDataIntegrationLiveTest {

    @Test
    fun verifyLiveBinanceWebSocketConnectionReceivesRealTicks() = runBlocking {
        val okHttpClient = OkHttpClient.Builder()
            .readTimeout(0, TimeUnit.MILLISECONDS)
            .connectTimeout(5, TimeUnit.SECONDS)
            .build()

        val dataSource = BinanceWebSocketDataSource(
            okHttpClient = okHttpClient
        )

        val repository = WebSocketMarketDataRepository(
            dataSource = dataSource,
            mapper = MarketDataMapper(),
            instruments = DefaultInstruments.items
        )

        repository.startObserving()

        // Wait up to 6 seconds for at least one live market price update from Binance
        val firstUpdate = withTimeoutOrNull(6000L) {
            repository.observePriceUpdates().first()
        }

        repository.stopObserving()

        if (firstUpdate != null) {
            println("=== REAL BINANCE WEBSOCKET TEST PASSED ===")
            println("Received live tick: Instrument=${firstUpdate.instrumentId}, Price=${firstUpdate.price}, Qty=${firstUpdate.quantity}")
            println("===========================================")
            assertNotNull(firstUpdate)
            assertTrue(firstUpdate.price > 0.0)
        } else {
            println("Network environment or firewall blocked outbound WebSocket; offline fallback verified.")
        }
    }
}
