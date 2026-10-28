package com.shubham.stockwatchlistdemo.feature.watchlist.presentation.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.shubham.stockwatchlistdemo.core.designsystem.theme.AccentBlue
import com.shubham.stockwatchlistdemo.core.designsystem.theme.DarkBackground
import com.shubham.stockwatchlistdemo.core.designsystem.theme.DarkBorder
import com.shubham.stockwatchlistdemo.core.designsystem.theme.DarkSurface
import com.shubham.stockwatchlistdemo.core.designsystem.theme.DarkSurfaceElevated
import com.shubham.stockwatchlistdemo.core.designsystem.theme.MarketDownRed
import com.shubham.stockwatchlistdemo.core.designsystem.theme.MarketUpGreen
import com.shubham.stockwatchlistdemo.core.designsystem.theme.StatusAmber
import com.shubham.stockwatchlistdemo.core.designsystem.theme.StatusAmberSubtle
import com.shubham.stockwatchlistdemo.core.designsystem.theme.StatusGreen
import com.shubham.stockwatchlistdemo.core.designsystem.theme.StatusRed
import com.shubham.stockwatchlistdemo.core.designsystem.theme.TextPrimary
import com.shubham.stockwatchlistdemo.core.designsystem.theme.TextSecondary
import com.shubham.stockwatchlistdemo.core.designsystem.theme.TextTertiary
import com.shubham.stockwatchlistdemo.core.model.MarketConnectionState
import com.shubham.stockwatchlistdemo.feature.watchlist.presentation.WatchlistFilter
import com.shubham.stockwatchlistdemo.feature.watchlist.presentation.WatchlistSortOption

@Composable
fun WatchlistHeader(
    connectionState: MarketConnectionState,
    totalCount: Int,
    gainersCount: Int,
    losersCount: Int,
    searchQuery: String,
    activeFilter: WatchlistFilter,
    activeSort: WatchlistSortOption,
    onSearchQueryChanged: (String) -> Unit,
    onFilterSelected: (WatchlistFilter) -> Unit,
    onSortSelected: (WatchlistSortOption) -> Unit,
    onRetryClicked: () -> Unit,
    modifier: Modifier = Modifier
) {
    var isSearchExpanded by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(DarkBackground)
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        // App Title Row with Connection Status Badge
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "MARKET WATCH",
                    style = androidx.compose.ui.text.TextStyle(
                        fontFamily = FontFamily.Default,
                        fontWeight = FontWeight.Black,
                        fontSize = 20.sp,
                        letterSpacing = 1.sp,
                        color = TextPrimary
                    )
                )
                Text(
                    text = "Live Crypto Pairs Feed",
                    style = androidx.compose.ui.text.TextStyle(
                        fontSize = 12.sp,
                        color = TextSecondary
                    )
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                    onClick = { isSearchExpanded = !isSearchExpanded },
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = if (isSearchExpanded) Icons.Default.Close else Icons.Default.Search,
                        contentDescription = "Toggle Search",
                        tint = TextSecondary,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                ConnectionBadge(
                    connectionState = connectionState,
                    onRetryClicked = onRetryClicked
                )
            }
        }

        // Expandable Search Bar
        AnimatedVisibility(
            visible = isSearchExpanded,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            Column(modifier = Modifier.padding(top = 10.dp)) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = onSearchQueryChanged,
                    placeholder = { Text("Search by symbol (e.g. BTC, ETH)", color = TextTertiary, fontSize = 13.sp) },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = DarkSurface,
                        unfocusedContainerColor = DarkSurface,
                        focusedBorderColor = AccentBlue,
                        unfocusedBorderColor = DarkBorder,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { onSearchQueryChanged("") }) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Clear search",
                                    tint = TextSecondary,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Aggregate Summary Metrics Card
        MarketSummaryCard(
            totalCount = totalCount,
            gainersCount = gainersCount,
            losersCount = losersCount
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Filter and Sort Chips Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Filter options
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                WatchlistFilter.entries.forEach { filter ->
                    val isSelected = filter == activeFilter
                    FilterChip(
                        selected = isSelected,
                        onClick = { onFilterSelected(filter) },
                        label = {
                            Text(
                                text = filter.displayName,
                                style = androidx.compose.ui.text.TextStyle(
                                    fontSize = 11.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                )
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            containerColor = DarkSurface,
                            labelColor = TextSecondary,
                            selectedContainerColor = AccentBlue.copy(alpha = 0.2f),
                            selectedLabelColor = AccentBlue
                        ),
                        border = FilterChipDefaults.filterChipBorder(
                            borderColor = DarkBorder,
                            selectedBorderColor = AccentBlue,
                            enabled = true,
                            selected = isSelected
                        )
                    )
                }
            }

            // Quick Sort Button
            SortMenuPill(
                activeSort = activeSort,
                onSortSelected = onSortSelected
            )
        }
    }
}

@Composable
fun ConnectionBadge(
    connectionState: MarketConnectionState,
    onRetryClicked: () -> Unit,
    modifier: Modifier = Modifier
) {
    val (statusColor, statusText, isClickable) = when (connectionState) {
        is MarketConnectionState.Connected -> Triple(StatusGreen, "LIVE", false)
        is MarketConnectionState.Connecting -> Triple(AccentBlue, "CONNECTING", false)
        is MarketConnectionState.Reconnecting -> Triple(
            StatusAmber,
            "RECONNECTING",
            false
        )
        is MarketConnectionState.Error -> Triple(StatusRed, "FAILED • RETRY", true)
        is MarketConnectionState.Disconnected -> Triple(StatusAmber, "DISCONNECTED", true)
    }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(DarkSurfaceElevated)
            .border(1.dp, statusColor.copy(alpha = 0.4f), RoundedCornerShape(16.dp))
            .then(
                if (isClickable) Modifier.clickable(onClick = onRetryClicked) else Modifier
            )
            .padding(horizontal = 10.dp, vertical = 5.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            if (connectionState is MarketConnectionState.Connecting) {
                CircularProgressIndicator(
                    modifier = Modifier.size(8.dp),
                    color = AccentBlue,
                    strokeWidth = 1.5.dp
                )
            } else {
                Box(
                    modifier = Modifier
                        .size(7.dp)
                        .clip(CircleShape)
                        .background(statusColor)
                )
            }

            Spacer(modifier = Modifier.width(6.dp))

            Text(
                text = statusText,
                style = androidx.compose.ui.text.TextStyle(
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    fontSize = 10.sp,
                    letterSpacing = 0.5.sp,
                    color = statusColor
                )
            )
        }
    }
}

@Composable
fun MarketSummaryCard(
    totalCount: Int,
    gainersCount: Int,
    losersCount: Int,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(DarkSurface)
            .border(1.dp, DarkBorder, RoundedCornerShape(8.dp))
            .padding(horizontal = 12.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceAround,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "INSTRUMENTS",
                style = androidx.compose.ui.text.TextStyle(
                    fontSize = 9.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TextTertiary
                )
            )
            Text(
                text = "$totalCount Pairs",
                style = androidx.compose.ui.text.TextStyle(
                    fontFamily = FontFamily.Monospace,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
            )
        }

        Box(
            modifier = Modifier
                .width(1.dp)
                .height(20.dp)
                .background(DarkBorder)
        )

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "GAINERS",
                style = androidx.compose.ui.text.TextStyle(
                    fontSize = 9.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TextTertiary
                )
            )
            Text(
                text = "$gainersCount",
                style = androidx.compose.ui.text.TextStyle(
                    fontFamily = FontFamily.Monospace,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = MarketUpGreen
                )
            )
        }

        Box(
            modifier = Modifier
                .width(1.dp)
                .height(20.dp)
                .background(DarkBorder)
        )

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "LOSERS",
                style = androidx.compose.ui.text.TextStyle(
                    fontSize = 9.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TextTertiary
                )
            )
            Text(
                text = "$losersCount",
                style = androidx.compose.ui.text.TextStyle(
                    fontFamily = FontFamily.Monospace,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = MarketDownRed
                )
            )
        }
    }
}

@Composable
fun SortMenuPill(
    activeSort: WatchlistSortOption,
    onSortSelected: (WatchlistSortOption) -> Unit,
    modifier: Modifier = Modifier
) {
    // Cycles through sort options on tap
    val nextSort = when (activeSort) {
        WatchlistSortOption.DEFAULT -> WatchlistSortOption.PRICE_DESC
        WatchlistSortOption.PRICE_DESC -> WatchlistSortOption.PRICE_ASC
        WatchlistSortOption.PRICE_ASC -> WatchlistSortOption.CHANGE_DESC
        WatchlistSortOption.CHANGE_DESC -> WatchlistSortOption.DEFAULT
        WatchlistSortOption.CHANGE_ASC -> WatchlistSortOption.DEFAULT
    }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(DarkSurfaceElevated)
            .border(1.dp, DarkBorder, RoundedCornerShape(12.dp))
            .clickable { onSortSelected(nextSort) }
            .padding(horizontal = 8.dp, vertical = 6.dp)
    ) {
        Text(
            text = "Sort: ${activeSort.displayName}",
            style = androidx.compose.ui.text.TextStyle(
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                color = AccentBlue
            )
        )
    }
}
