package com.shubham.stockwatchlistdemo.feature.watchlist.presentation

import com.shubham.stockwatchlistdemo.core.model.MarketConnectionState
import com.shubham.stockwatchlistdemo.core.model.MarketInstrument
import com.shubham.stockwatchlistdemo.core.model.MarketPriceUpdate
import com.shubham.stockwatchlistdemo.feature.watchlist.presentation.WatchlistFilter
import com.shubham.stockwatchlistdemo.feature.watchlist.presentation.WatchlistSortOption
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class WatchlistViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()
    private lateinit var fakeRepository: FakeMarketDataRepository
    private lateinit var viewModel: WatchlistViewModel

    private val instruments = listOf(
        MarketInstrument("BTCUSDT", "BTC/USDT", "Bitcoin", "BTC", "USDT", 2),
        MarketInstrument("ETHUSDT", "ETH/USDT", "Ethereum", "ETH", "USDT", 2),
        MarketInstrument("SOLUSDT", "SOL/USDT", "Solana", "SOL", "USDT", 2),
        MarketInstrument("DOGEUSDT", "DOGE/USDT", "Dogecoin", "DOGE", "USDT", 5)
    )

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        fakeRepository = FakeMarketDataRepository(instruments = instruments)
        viewModel = WatchlistViewModel(
            repository = fakeRepository,
            defaultDispatcher = testDispatcher,
            samplingWindowMs = 0L,
            sharingStarted = SharingStarted.Eagerly
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun initialState_containsAllInstrumentsWithUninitializedPlaceholders() {
        val state = viewModel.uiState.value
        assertEquals(4, state.instruments.size)
        assertEquals(4, state.rawInstrumentsCount)
        assertEquals("---", state.instruments[0].formattedPrice)
        assertTrue(fakeRepository.isObservingStarted)
    }

    @Test
    fun connectionState_propagatesCorrectlyToUiState() {
        fakeRepository.connectionStateFlow.value = MarketConnectionState.Connected
        assertTrue(viewModel.uiState.value.isConnected)

        fakeRepository.connectionStateFlow.value = MarketConnectionState.Reconnecting(
            attempt = 2,
            maxAttempts = 5,
            delayMs = 2000L
        )
        assertTrue(viewModel.uiState.value.isReconnecting)

        fakeRepository.connectionStateFlow.value = MarketConnectionState.Error("Server closed")
        assertTrue(viewModel.uiState.value.isError)
    }

    @Test
    fun disconnection_retainsLastKnownPrices() {
        viewModel.processPriceUpdate(MarketPriceUpdate("BTCUSDT", 68500.0))
        viewModel.processPriceUpdate(MarketPriceUpdate("ETHUSDT", 2600.0))
        fakeRepository.connectionStateFlow.value = MarketConnectionState.Disconnected
        val state = viewModel.uiState.value
        val btc = state.instruments.first { it.id == "BTCUSDT" }
        val eth = state.instruments.first { it.id == "ETHUSDT" }
        assertEquals("$68,500.00", btc.formattedPrice)
        assertEquals("$2,600.00", eth.formattedPrice)
    }

    @Test
    fun searchQuery_filtersInstruments() {
        viewModel.onSearchQueryChanged("eth")
        val state = viewModel.uiState.value
        assertEquals(1, state.instruments.size)
        assertEquals("ETHUSDT", state.instruments[0].id)
    }

    @Test
    fun searchQuery_matchesSymbolOrName() {
        viewModel.onSearchQueryChanged("doge")
        assertEquals(1, viewModel.uiState.value.instruments.size)
        assertEquals("DOGEUSDT", viewModel.uiState.value.instruments[0].id)

        viewModel.onSearchQueryChanged("Bitcoin")
        assertEquals(1, viewModel.uiState.value.instruments.size)
        assertEquals("BTCUSDT", viewModel.uiState.value.instruments[0].id)
    }

    @Test
    fun filterOptions_filtersGainersAndLosers() {
        // BTC gains
        viewModel.processPriceUpdate(MarketPriceUpdate("BTCUSDT", 60000.0))
        viewModel.processPriceUpdate(MarketPriceUpdate("BTCUSDT", 65000.0))
        // ETH loses
        viewModel.processPriceUpdate(MarketPriceUpdate("ETHUSDT", 3000.0))
        viewModel.processPriceUpdate(MarketPriceUpdate("ETHUSDT", 2800.0))

        viewModel.onFilterSelected(WatchlistFilter.GAINERS)
        var state = viewModel.uiState.value
        assertEquals(1, state.instruments.size)
        assertEquals("BTCUSDT", state.instruments[0].id)

        viewModel.onFilterSelected(WatchlistFilter.LOSERS)
        state = viewModel.uiState.value
        assertEquals(1, state.instruments.size)
        assertEquals("ETHUSDT", state.instruments[0].id)

        viewModel.onFilterSelected(WatchlistFilter.ALL)
        state = viewModel.uiState.value
        assertEquals(4, state.instruments.size)
    }

    @Test
    fun searchRanking_prefersSymbolPrefixOverContainsAndName() {
        viewModel.onSearchQueryChanged("btc")
        val ids = viewModel.uiState.value.instruments.map { it.id }
        assertEquals(listOf("BTCUSDT"), ids.take(1))
    }

    @Test
    fun sortOptions_sortsByPriceDescendingAndAscending() {
        viewModel.processPriceUpdate(MarketPriceUpdate("BTCUSDT", 68000.0))
        viewModel.processPriceUpdate(MarketPriceUpdate("ETHUSDT", 2500.0))
        viewModel.processPriceUpdate(MarketPriceUpdate("SOLUSDT", 150.0))
        viewModel.processPriceUpdate(MarketPriceUpdate("DOGEUSDT", 0.15))

        viewModel.onSortSelected(WatchlistSortOption.PRICE_DESC)
        var state = viewModel.uiState.value
        assertEquals("BTCUSDT", state.instruments[0].id)
        assertEquals("ETHUSDT", state.instruments[1].id)
        assertEquals("SOLUSDT", state.instruments[2].id)
        assertEquals("DOGEUSDT", state.instruments[3].id)

        viewModel.onSortSelected(WatchlistSortOption.PRICE_ASC)
        state = viewModel.uiState.value
        assertEquals("DOGEUSDT", state.instruments[0].id)
        assertEquals("SOLUSDT", state.instruments[1].id)
        assertEquals("ETHUSDT", state.instruments[2].id)
        assertEquals("BTCUSDT", state.instruments[3].id)
    }

    @Test
    fun retryConnection_invokesRepositoryRetry() {
        viewModel.retryConnection()
        assertEquals(1, fakeRepository.retryCount)
    }
}
