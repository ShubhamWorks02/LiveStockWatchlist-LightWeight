package com.shubham.stockwatchlistdemo.feature.watchlist.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.shubham.stockwatchlistdemo.core.model.InstrumentUiModel
import com.shubham.stockwatchlistdemo.core.model.MarketConnectionState
import com.shubham.stockwatchlistdemo.core.model.MarketInstrument
import com.shubham.stockwatchlistdemo.core.model.MarketPriceUpdate
import com.shubham.stockwatchlistdemo.core.model.PriceDirection
import com.shubham.stockwatchlistdemo.data.market.MarketDataMapper
import com.shubham.stockwatchlistdemo.data.market.MarketDataRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.sample
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap

/**
 * ViewModel orchestrating watchlist market data, user interactions (search, filter, sort),
 * and high-frequency real-time update coalescing.
 */
@OptIn(FlowPreview::class)
class WatchlistViewModel(
    private val repository: MarketDataRepository,
    private val defaultDispatcher: CoroutineDispatcher = Dispatchers.Default,
    private val samplingWindowMs: Long = 50L,
    private val sharingStarted: SharingStarted = SharingStarted.WhileSubscribed(stopTimeoutMillis = 5_000)
) : ViewModel() {

    // Internal state container holding raw metrics per instrument
    private data class InternalInstrumentRecord(
        val instrument: MarketInstrument,
        val currentPrice: Double? = null,
        val previousPrice: Double? = null,
        val baselinePrice: Double? = null,
        val highPrice: Double = 0.0,
        val lowPrice: Double = Double.MAX_VALUE,
        val direction: PriceDirection = PriceDirection.UNCHANGED,
        val updateCounter: Long = 0L,
        val lastUpdatedTimestamp: Long = 0L
    )

    private val initialRecords: Map<String, InternalInstrumentRecord> =
        repository.getInstruments().associate { instrument ->
            instrument.id to InternalInstrumentRecord(instrument = instrument)
        }

    private val internalRecordsFlow = MutableStateFlow(initialRecords)
    private val searchQueryFlow = MutableStateFlow("")
    private val filterFlow = MutableStateFlow(WatchlistFilter.ALL)
    private val sortFlow = MutableStateFlow(WatchlistSortOption.DEFAULT)

    /**
     * UI StateFlow sampled at [samplingWindowMs] to protect the Compose UI thread
     * from high-frequency packet bursts while always preserving the absolute latest price.
     */
    val uiState: StateFlow<WatchlistUiState> = run {
        val recordsSource = if (samplingWindowMs > 0L) {
            internalRecordsFlow.sample(samplingWindowMs)
        } else {
            internalRecordsFlow
        }

        combine(
            repository.connectionState,
            recordsSource,
            searchQueryFlow,
            filterFlow,
            sortFlow
        ) { connectionState, records, query, filter, sort ->
            buildUiState(
                connectionState = connectionState,
                records = records.values.toList(),
                query = query,
                filter = filter,
                sort = sort
            )
        }
        .flowOn(defaultDispatcher)
        .stateIn(
            scope = viewModelScope,
            started = sharingStarted,
            initialValue = WatchlistUiState(
                connectionState = MarketConnectionState.Connecting,
                instruments = initialRecords.values.map { it.toUiModel() },
                rawInstrumentsCount = initialRecords.size
            )
        )
    }

    init {
        startObservingMarketData()
    }

    private fun startObservingMarketData() {
        repository.startObserving()

        viewModelScope.launch(defaultDispatcher) {
            repository.observePriceUpdates().collect { update ->
                processPriceUpdate(update)
            }
        }
    }

    /**
     * Processes an incoming price tick. Thread-safe state mutation on [defaultDispatcher].
     *
     * Handles:
     * 1. Baseline initialization without triggering false price flash.
     * 2. Price direction detection (UP/DOWN/UNCHANGED).
     * 3. Animation trigger counter increment on genuine movements.
     * 4. Session high and low tracking.
     */
    internal fun processPriceUpdate(update: MarketPriceUpdate) {
        val currentMap = internalRecordsFlow.value
        val existing = currentMap[update.instrumentId] ?: return

        val newPrice = update.price
        val oldPrice = existing.currentPrice

        if (oldPrice == null) {
            // First tick: establish initial baseline without directional flash
            val updated = existing.copy(
                currentPrice = newPrice,
                previousPrice = null,
                baselinePrice = newPrice,
                highPrice = newPrice,
                lowPrice = newPrice,
                direction = PriceDirection.UNCHANGED,
                updateCounter = 0L,
                lastUpdatedTimestamp = update.timestamp
            )
            val newMap = HashMap(currentMap)
            newMap[update.instrumentId] = updated
            internalRecordsFlow.value = newMap
            return
        }

        if (newPrice == oldPrice) {
            // Identical price: update timestamp only, no animation flash
            val updated = existing.copy(
                lastUpdatedTimestamp = update.timestamp
            )
            val newMap = HashMap(currentMap)
            newMap[update.instrumentId] = updated
            internalRecordsFlow.value = newMap
            return
        }

        val direction = if (newPrice > oldPrice) PriceDirection.UP else PriceDirection.DOWN
        val high = maxOf(existing.highPrice, newPrice)
        val low = if (existing.lowPrice == Double.MAX_VALUE) newPrice else minOf(existing.lowPrice, newPrice)

        val updated = existing.copy(
            currentPrice = newPrice,
            previousPrice = oldPrice,
            direction = direction,
            highPrice = high,
            lowPrice = low,
            updateCounter = existing.updateCounter + 1,
            lastUpdatedTimestamp = update.timestamp
        )

        val newMap = HashMap(currentMap)
        newMap[update.instrumentId] = updated
        internalRecordsFlow.value = newMap
    }

    private fun buildUiState(
        connectionState: MarketConnectionState,
        records: List<InternalInstrumentRecord>,
        query: String,
        filter: WatchlistFilter,
        sort: WatchlistSortOption
    ): WatchlistUiState {
        var gainers = 0
        var losers = 0

        val mappedList = records.map { record ->
            val uiModel = record.toUiModel()
            if (uiModel.isPositiveChange == true) gainers++
            if (uiModel.isPositiveChange == false) losers++
            uiModel
        }

        // Apply search query filter with ranking
        val filteredByQuery = if (query.isBlank()) {
            mappedList.map { it to 0 }
        } else {
            val normalized = query.trim().lowercase(Locale.US)
            mappedList.map { uiModel ->
                var score = 0
                val symbol = uiModel.symbol.lowercase(Locale.US)
                val name = uiModel.name.lowercase(Locale.US)
                val base = uiModel.baseAsset.lowercase(Locale.US)
                if (symbol.startsWith(normalized)) score = 3
                else if (symbol.contains(normalized)) score = 2
                else if (name.contains(normalized)) score = 1
                else if (base.contains(normalized)) score = 0
                uiModel to score
            }.filter { it.second > 0 }
        }

        // Sort by score descending before applying category filter
        val scoredList = filteredByQuery.sortedByDescending { it.second }.map { it.first }

        // Apply category / gainer-loser filter
        val filteredByCategory = when (filter) {
            WatchlistFilter.ALL -> scoredList
            WatchlistFilter.GAINERS -> scoredList.filter { it.isPositiveChange == true }
            WatchlistFilter.LOSERS -> scoredList.filter { it.isPositiveChange == false }
        }

        // Apply sorting
        val sortedList = when (sort) {
            WatchlistSortOption.DEFAULT -> filteredByCategory
            WatchlistSortOption.PRICE_DESC -> filteredByCategory.sortedByDescending { it.rawPrice }
            WatchlistSortOption.PRICE_ASC -> filteredByCategory.sortedBy { it.rawPrice }
            WatchlistSortOption.CHANGE_DESC -> filteredByCategory.sortedByDescending {
                val change = it.rawPrice - (it.previousPrice ?: it.rawPrice)
                change
            }
            WatchlistSortOption.CHANGE_ASC -> filteredByCategory.sortedBy {
                val change = it.rawPrice - (it.previousPrice ?: it.rawPrice)
                change
            }
        }

        return WatchlistUiState(
            connectionState = connectionState,
            instruments = sortedList,
            rawInstrumentsCount = records.size,
            searchQuery = query,
            activeFilter = filter,
            sortOption = sort,
            gainersCount = gainers,
            losersCount = losers,
            lastStateUpdateTimeMs = System.currentTimeMillis()
        )
    }

    private fun InternalInstrumentRecord.toUiModel(): InstrumentUiModel {
        val price = currentPrice ?: 0.0
        val formattedPrice = if (currentPrice != null) {
            MarketDataMapper.formatPrice(price, instrument.decimalPlaces)
        } else {
            "---"
        }

        val base = baselinePrice
        val (changePercentStr, isPositive) = if (currentPrice != null && base != null && base > 0.0) {
            val changePercent = ((price - base) / base) * 100.0
            Pair(
                MarketDataMapper.formatPercentChange(changePercent),
                changePercent >= 0.0
            )
        } else {
            Pair("0.00%", null)
        }

        val highFormatted = if (highPrice > 0.0) MarketDataMapper.formatPrice(highPrice, instrument.decimalPlaces) else ""
        val lowFormatted = if (lowPrice < Double.MAX_VALUE) MarketDataMapper.formatPrice(lowPrice, instrument.decimalPlaces) else ""

        return InstrumentUiModel(
            id = instrument.id,
            symbol = instrument.symbol,
            name = instrument.name,
            baseAsset = instrument.baseAsset,
            quoteAsset = instrument.quoteAsset,
            formattedPrice = formattedPrice,
            rawPrice = price,
            previousPrice = previousPrice,
            direction = direction,
            formattedChangePercent = changePercentStr,
            isPositiveChange = isPositive,
            highPriceFormatted = highFormatted,
            lowPriceFormatted = lowFormatted,
            updateCounter = updateCounter,
            lastUpdatedTimestamp = lastUpdatedTimestamp
        )
    }

    // User actions
    fun onSearchQueryChanged(query: String) {
        searchQueryFlow.value = query
    }

    fun onFilterSelected(filter: WatchlistFilter) {
        filterFlow.value = filter
    }

    fun onSortSelected(sort: WatchlistSortOption) {
        sortFlow.value = sort
    }

    fun retryConnection() {
        repository.retryConnection()
    }

    override fun onCleared() {
        super.onCleared()
        repository.stopObserving()
    }
}
