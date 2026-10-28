package com.shubham.stockwatchlistdemo.data.market

import com.shubham.stockwatchlistdemo.core.model.MarketConnectionState
import com.shubham.stockwatchlistdemo.core.model.MarketInstrument
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class WebSocketMarketDataRepositoryTest {

    private val testDispatcher = UnconfinedTestDispatcher()
    private lateinit var fakeDataSource: FakeMarketDataDataSource
    private lateinit var repository: WebSocketMarketDataRepository

    private val instruments = listOf(
        MarketInstrument("BTCUSDT", "BTC/USDT", "Bitcoin", "BTC", "USDT", 2)
    )

    @Before
    fun setUp() {
        fakeDataSource = FakeMarketDataDataSource()
        repository = WebSocketMarketDataRepository(
            dataSource = fakeDataSource,
            mapper = MarketDataMapper(),
            instruments = instruments,
            defaultDispatcher = testDispatcher
        )
    }

    @Test
    fun startObserving_connectsDataSourceWithConfiguredInstruments() {
        repository.startObserving()
        assertTrue(fakeDataSource.isConnected)
        assertEquals(instruments, fakeDataSource.connectedInstruments)
    }

    @Test
    fun stopObserving_disconnectsDataSource() {
        repository.startObserving()
        repository.stopObserving()
        assertTrue(fakeDataSource.isDisconnected)
    }

    @Test
    fun retryConnection_reconnectsDataSource() {
        repository.retryConnection()
        assertTrue(fakeDataSource.isReconnected)
    }

    @Test
    fun observePriceUpdates_parsesValidIncomingWebSocketMessages() = runTest(testDispatcher) {
        val collectedUpdates = mutableListOf<com.shubham.stockwatchlistdemo.core.model.MarketPriceUpdate>()
        val job = launch {
            repository.observePriceUpdates().collect { update ->
                collectedUpdates.add(update)
            }
        }

        // Simulate incoming trade payload
        val rawJson = """
            {
                "stream": "btcusdt@trade",
                "data": {
                    "e": "trade",
                    "s": "BTCUSDT",
                    "p": "68500.00",
                    "q": "0.1",
                    "T": 1729837200000
                }
            }
        """.trimIndent()

        fakeDataSource.messagesFlow.emit(rawJson)

        assertEquals(1, collectedUpdates.size)
        assertEquals("BTCUSDT", collectedUpdates[0].instrumentId)
        assertEquals(68500.0, collectedUpdates[0].price, 0.001)

        job.cancel()
    }

    @Test
    fun connectionState_propagatesDirectlyFromDataSource() = runTest(testDispatcher) {
        fakeDataSource.connectionStateFlow.value = MarketConnectionState.Connected
        assertEquals(MarketConnectionState.Connected, repository.connectionState.first())

        fakeDataSource.connectionStateFlow.value = MarketConnectionState.Disconnected
        assertEquals(MarketConnectionState.Disconnected, repository.connectionState.first())
    }
}

class FakeMarketDataDataSource : MarketDataDataSource {
    val messagesFlow = MutableSharedFlow<String>()
    override val incomingMessages: Flow<String> = messagesFlow

    val connectionStateFlow = MutableStateFlow<MarketConnectionState>(MarketConnectionState.Disconnected)
    override val connectionState: Flow<MarketConnectionState> = connectionStateFlow

    var isConnected = false
    var isDisconnected = false
    var isReconnected = false
    var connectedInstruments: List<MarketInstrument> = emptyList()

    override fun connect(instruments: List<MarketInstrument>) {
        isConnected = true
        isDisconnected = false
        connectedInstruments = instruments
    }

    override fun disconnect() {
        isDisconnected = true
        isConnected = false
    }

    override fun reconnect(instruments: List<MarketInstrument>) {
        isReconnected = true
        connectedInstruments = instruments
    }
}
