package com.shubham.stockwatchlistdemo.feature.watchlist.presentation

import com.shubham.stockwatchlistdemo.core.model.MarketInstrument
import com.shubham.stockwatchlistdemo.core.model.MarketPriceUpdate
import com.shubham.stockwatchlistdemo.data.market.DefaultInstruments
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import kotlin.system.measureNanoTime

/**
 * Repeatable synthetic benchmark testing high-frequency market update ingestion and processing.
 *
 * Simulates real-world market volatility bursts (10,000 successive price updates across all 8 instruments)
 * to measure processing throughput and verify consistency of aggregated prices.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class SyntheticBurstPerformanceTest {

    private val testDispatcher = UnconfinedTestDispatcher()
    private lateinit var fakeRepository: FakeMarketDataRepository
    private lateinit var viewModel: WatchlistViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        fakeRepository = FakeMarketDataRepository(instruments = DefaultInstruments.items)
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
    fun highFrequencyUpdateBurst_processes10000TicksEfficientlyAndAccurately() {
        val totalTicks = 10_000
        val instruments = DefaultInstruments.items
        val instrumentCount = instruments.size

        // Pre-generate deterministic synthetic price sequence
        val updates = (0 until totalTicks).map { i ->
            val instrument = instruments[i % instrumentCount]
            val basePrice = when (instrument.id) {
                "BTCUSDT" -> 68000.0
                "ETHUSDT" -> 2500.0
                "SOLUSDT" -> 150.0
                "BNBUSDT" -> 580.0
                "XRPUSDT" -> 0.52
                "ADAUSDT" -> 0.35
                "DOGEUSDT" -> 0.14
                "LINKUSDT" -> 11.5
                else -> 100.0
            }
            // Oscillating fractional price delta
            val delta = ((i % 100) - 50) * 0.05
            MarketPriceUpdate(
                instrumentId = instrument.id,
                price = basePrice + delta,
                timestamp = System.currentTimeMillis() + i
            )
        }

        // Measure execution time
        val elapsedNanos = measureNanoTime {
            for (update in updates) {
                viewModel.processPriceUpdate(update)
            }
        }

        val elapsedMillis = elapsedNanos / 1_000_000.0
        val throughput = (totalTicks / (elapsedNanos / 1_000_000_000.0)).toLong()

        println("=== HIGH-FREQUENCY UPDATE BURST BENCHMARK ===")
        println("Processed $totalTicks price updates across $instrumentCount instruments.")
        println("Elapsed time: ${String.format(java.util.Locale.US, "%.2f ms", elapsedMillis)}")
        println("Throughput: $throughput updates/sec on JVM.")
        println("==============================================")

        // Verify state consistency after the burst
        val finalState = viewModel.uiState.value
        assertEquals(8, finalState.instruments.size)

        // Verify each instrument received updates and has valid non-zero prices
        for (instrument in finalState.instruments) {
            assertTrue(instrument.rawPrice > 0.0)
            assertTrue(instrument.updateCounter > 0L)
            assertTrue(instrument.formattedPrice.startsWith("$"))
        }

        // Must process 10,000 updates comfortably in under 500ms on modern hardware
        assertTrue("Burst benchmark exceeded reasonable threshold: ${elapsedMillis}ms", elapsedMillis < 500.0)
    }
}
