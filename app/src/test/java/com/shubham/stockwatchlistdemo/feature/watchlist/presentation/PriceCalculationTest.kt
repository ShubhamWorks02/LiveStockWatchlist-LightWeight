package com.shubham.stockwatchlistdemo.feature.watchlist.presentation

import com.shubham.stockwatchlistdemo.core.model.MarketConnectionState
import com.shubham.stockwatchlistdemo.core.model.MarketInstrument
import com.shubham.stockwatchlistdemo.core.model.MarketPriceUpdate
import com.shubham.stockwatchlistdemo.core.model.PriceDirection
import com.shubham.stockwatchlistdemo.data.market.MarketDataRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class PriceCalculationTest {

    private val testDispatcher = UnconfinedTestDispatcher()
    private lateinit var fakeRepository: FakeMarketDataRepository
    private lateinit var viewModel: WatchlistViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        fakeRepository = FakeMarketDataRepository()
        viewModel = WatchlistViewModel(
            repository = fakeRepository,
            defaultDispatcher = testDispatcher,
            samplingWindowMs = 0L, // Immediate passthrough for deterministic testing
            sharingStarted = SharingStarted.Eagerly
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun initialPrice_establishesBaselineWithoutMisleadingFlash() {
        viewModel.processPriceUpdate(
            MarketPriceUpdate(instrumentId = "BTCUSDT", price = 68000.0)
        )

        val btc = viewModel.uiState.value.instruments.first { it.id == "BTCUSDT" }
        assertEquals(68000.0, btc.rawPrice, 0.001)
        assertNull(btc.previousPrice)
        assertEquals(PriceDirection.UNCHANGED, btc.direction)
        assertEquals(0L, btc.updateCounter)
        assertEquals("$68,000.00", btc.formattedPrice)
        assertEquals("0.00%", btc.formattedChangePercent)
    }

    @Test
    fun priceIncrease_setsDirectionUpAndIncrementsCounter() {
        // First baseline
        viewModel.processPriceUpdate(MarketPriceUpdate(instrumentId = "BTCUSDT", price = 68000.0))
        // Price increase
        viewModel.processPriceUpdate(MarketPriceUpdate(instrumentId = "BTCUSDT", price = 68500.0))

        val btc = viewModel.uiState.value.instruments.first { it.id == "BTCUSDT" }
        assertEquals(68500.0, btc.rawPrice, 0.001)
        assertEquals(68000.0, btc.previousPrice ?: 0.0, 0.001)
        assertEquals(PriceDirection.UP, btc.direction)
        assertEquals(1L, btc.updateCounter)
    }

    @Test
    fun successiveIncreases_incrementCounterForAnimationTrigger() {
        viewModel.processPriceUpdate(MarketPriceUpdate(instrumentId = "BTCUSDT", price = 68000.0))
        viewModel.processPriceUpdate(MarketPriceUpdate(instrumentId = "BTCUSDT", price = 68100.0))
        viewModel.processPriceUpdate(MarketPriceUpdate(instrumentId = "BTCUSDT", price = 68200.0))

        val btc = viewModel.uiState.value.instruments.first { it.id == "BTCUSDT" }
        assertEquals(PriceDirection.UP, btc.direction)
        assertEquals(2L, btc.updateCounter) // Incremented for both movements
        assertEquals(68200.0, btc.rawPrice, 0.001)
    }

    @Test
    fun priceDecrease_setsDirectionDownAndIncrementsCounter() {
        viewModel.processPriceUpdate(MarketPriceUpdate(instrumentId = "ETHUSDT", price = 2500.0))
        viewModel.processPriceUpdate(MarketPriceUpdate(instrumentId = "ETHUSDT", price = 2450.0))

        val eth = viewModel.uiState.value.instruments.first { it.id == "ETHUSDT" }
        assertEquals(2450.0, eth.rawPrice, 0.001)
        assertEquals(2500.0, eth.previousPrice ?: 0.0, 0.001)
        assertEquals(PriceDirection.DOWN, eth.direction)
        assertEquals(1L, eth.updateCounter)
        assertEquals("-2.00%", eth.formattedChangePercent)
    }

    @Test
    fun unchangedPrice_doesNotIncrementCounter() {
        viewModel.processPriceUpdate(MarketPriceUpdate(instrumentId = "BTCUSDT", price = 68000.0))
        viewModel.processPriceUpdate(MarketPriceUpdate(instrumentId = "BTCUSDT", price = 68500.0)) // counter = 1
        viewModel.processPriceUpdate(MarketPriceUpdate(instrumentId = "BTCUSDT", price = 68500.0)) // same price

        val btc = viewModel.uiState.value.instruments.first { it.id == "BTCUSDT" }
        assertEquals(1L, btc.updateCounter) // Remains 1, no false trigger
    }

    @Test
    fun highAndLowTracking_preservesExtremePrices() {
        viewModel.processPriceUpdate(MarketPriceUpdate(instrumentId = "SOLUSDT", price = 150.0))
        viewModel.processPriceUpdate(MarketPriceUpdate(instrumentId = "SOLUSDT", price = 155.0))
        viewModel.processPriceUpdate(MarketPriceUpdate(instrumentId = "SOLUSDT", price = 145.0))
        viewModel.processPriceUpdate(MarketPriceUpdate(instrumentId = "SOLUSDT", price = 152.0))

        val sol = viewModel.uiState.value.instruments.first { it.id == "SOLUSDT" }
        assertEquals("$155.00", sol.highPriceFormatted)
        assertEquals("$145.00", sol.lowPriceFormatted)
    }
}

class FakeMarketDataRepository(
    private val instruments: List<MarketInstrument> = listOf(
        MarketInstrument("BTCUSDT", "BTC/USDT", "Bitcoin", "BTC", "USDT", 2),
        MarketInstrument("ETHUSDT", "ETH/USDT", "Ethereum", "ETH", "USDT", 2),
        MarketInstrument("SOLUSDT", "SOL/USDT", "Solana", "SOL", "USDT", 2)
    )
) : MarketDataRepository {

    val connectionStateFlow = MutableStateFlow<MarketConnectionState>(MarketConnectionState.Connected)
    override val connectionState: Flow<MarketConnectionState> = connectionStateFlow

    val priceUpdatesFlow = MutableSharedFlow<MarketPriceUpdate>()
    override fun observePriceUpdates(): Flow<MarketPriceUpdate> = priceUpdatesFlow

    override fun getInstruments(): List<MarketInstrument> = instruments

    var isObservingStarted = false
    var isObservingStopped = false
    var retryCount = 0

    override fun startObserving() {
        isObservingStarted = true
    }

    override fun stopObserving() {
        isObservingStopped = true
    }

    override fun retryConnection() {
        retryCount++
    }
}
