package com.shubham.stockwatchlistdemo

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.shubham.stockwatchlistdemo.core.designsystem.theme.StockWatchListTheme
import com.shubham.stockwatchlistdemo.core.model.InstrumentUiModel
import com.shubham.stockwatchlistdemo.core.model.MarketConnectionState
import com.shubham.stockwatchlistdemo.core.model.PriceDirection
import com.shubham.stockwatchlistdemo.feature.watchlist.presentation.WatchlistFilter
import com.shubham.stockwatchlistdemo.feature.watchlist.presentation.WatchlistScreen
import com.shubham.stockwatchlistdemo.feature.watchlist.presentation.WatchlistSortOption
import com.shubham.stockwatchlistdemo.feature.watchlist.presentation.WatchlistUiState
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class WatchlistScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val sampleInstruments = listOf(
        InstrumentUiModel(
            id = "BTCUSDT",
            symbol = "BTC/USDT",
            name = "Bitcoin",
            baseAsset = "BTC",
            quoteAsset = "USDT",
            formattedPrice = "$68,542.10",
            rawPrice = 68542.10,
            direction = PriceDirection.UP,
            formattedChangePercent = "+2.35%",
            isPositiveChange = true,
            updateCounter = 1L
        ),
        InstrumentUiModel(
            id = "ETHUSDT",
            symbol = "ETH/USDT",
            name = "Ethereum",
            baseAsset = "ETH",
            quoteAsset = "USDT",
            formattedPrice = "$2,540.80",
            rawPrice = 2540.80,
            direction = PriceDirection.DOWN,
            formattedChangePercent = "-1.20%",
            isPositiveChange = false,
            updateCounter = 1L
        )
    )

    @Test
    fun watchlistScreen_displaysBrandingAndConnectionStatus() {
        val state = WatchlistUiState(
            connectionState = MarketConnectionState.Connected,
            instruments = sampleInstruments,
            rawInstrumentsCount = sampleInstruments.size,
            gainersCount = 1,
            losersCount = 1
        )

        composeTestRule.setContent {
            StockWatchListTheme {
                WatchlistScreen(
                    uiState = state,
                    onSearchQueryChanged = {},
                    onFilterSelected = {},
                    onSortSelected = {},
                    onRetryClicked = {}
                )
            }
        }

        composeTestRule.onNodeWithText("MARKET WATCH").assertIsDisplayed()
        composeTestRule.onNodeWithText("LIVE").assertIsDisplayed()
        composeTestRule.onNodeWithText("2 Pairs").assertIsDisplayed()
    }

    @Test
    fun watchlistScreen_displaysInstrumentRowsWithFormattedPrices() {
        val state = WatchlistUiState(
            connectionState = MarketConnectionState.Connected,
            instruments = sampleInstruments,
            rawInstrumentsCount = sampleInstruments.size
        )

        composeTestRule.setContent {
            StockWatchListTheme {
                WatchlistScreen(
                    uiState = state,
                    onSearchQueryChanged = {},
                    onFilterSelected = {},
                    onSortSelected = {},
                    onRetryClicked = {}
                )
            }
        }

        composeTestRule.onNodeWithText("BTC").assertIsDisplayed()
        composeTestRule.onNodeWithText("Bitcoin").assertIsDisplayed()
        composeTestRule.onNodeWithText("$68,542.10").assertIsDisplayed()
        composeTestRule.onNodeWithText("+2.35%").assertIsDisplayed()

        composeTestRule.onNodeWithText("ETH").assertIsDisplayed()
        composeTestRule.onNodeWithText("Ethereum").assertIsDisplayed()
        composeTestRule.onNodeWithText("$2,540.80").assertIsDisplayed()
        composeTestRule.onNodeWithText("-1.20%").assertIsDisplayed()
    }

    @Test
    fun watchlistScreen_errorStateDisplaysRetryButtonAndTriggersAction() {
        var retryTriggered = false
        val state = WatchlistUiState(
            connectionState = MarketConnectionState.Error("Network offline"),
            instruments = emptyList()
        )

        composeTestRule.setContent {
            StockWatchListTheme {
                WatchlistScreen(
                    uiState = state,
                    onSearchQueryChanged = {},
                    onFilterSelected = {},
                    onSortSelected = {},
                    onRetryClicked = { retryTriggered = true }
                )
            }
        }

        composeTestRule.onNodeWithText("Connection Error").assertIsDisplayed()
        composeTestRule.onNodeWithText("Retry Connection").performClick()
        assertTrue(retryTriggered)
    }

    @Test
    fun watchlistScreen_showsNoResultsViewWhenSearchYieldsEmptyList() {
        val query = "XYZ"
        val state = WatchlistUiState(
            connectionState = MarketConnectionState.Connected,
            instruments = emptyList(),
            searchQuery = query,
            rawInstrumentsCount = 0
        )

        composeTestRule.setContent {
            StockWatchListTheme {
                WatchlistScreen(
                    uiState = state,
                    onSearchQueryChanged = {},
                    onFilterSelected = {},
                    onSortSelected = {},
                    onRetryClicked = {}
                )
            }
        }

        // Verify the placeholder texts from EmptyWatchlistView are displayed
        composeTestRule.onNodeWithText("No matching instruments").assertIsDisplayed()
        composeTestRule.onNodeWithText("No pairs found matching \"$query\". Check for typos or clear your search filter.").assertIsDisplayed()
    }
}
