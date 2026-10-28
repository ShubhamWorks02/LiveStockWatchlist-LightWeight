package com.shubham.stockwatchlistdemo.feature.watchlist.presentation.components

import androidx.compose.animation.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.ArrowDropUp
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.shubham.stockwatchlistdemo.core.designsystem.theme.DarkBorder
import com.shubham.stockwatchlistdemo.core.designsystem.theme.DarkSurface
import com.shubham.stockwatchlistdemo.core.designsystem.theme.DarkSurfaceElevated
import com.shubham.stockwatchlistdemo.core.designsystem.theme.MarketDownRed
import com.shubham.stockwatchlistdemo.core.designsystem.theme.MarketDownRedSubtle
import com.shubham.stockwatchlistdemo.core.designsystem.theme.MarketUpGreen
import com.shubham.stockwatchlistdemo.core.designsystem.theme.MarketUpGreenSubtle
import com.shubham.stockwatchlistdemo.core.designsystem.theme.PriceTextStyle
import com.shubham.stockwatchlistdemo.core.designsystem.theme.TextPrimary
import com.shubham.stockwatchlistdemo.core.designsystem.theme.TextSecondary
import com.shubham.stockwatchlistdemo.core.designsystem.theme.TextTertiary
import com.shubham.stockwatchlistdemo.core.model.InstrumentUiModel
import com.shubham.stockwatchlistdemo.core.model.PriceDirection

/**
 * Pure, stateless composable representing a single instrument row.
 *
 * Designed for Compose skipping and performance:
 * - Narrow parameters: accepts only immutable [InstrumentUiModel].
 * - Localized animation state: price flash animation executes locally without causing
 *   unrelated rows or the parent LazyColumn to recompose.
 * - Semantic accessibility description for screen readers.
 */
@Composable
fun WatchlistRow(
    instrument: InstrumentUiModel,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null
) {
    // Localized color animatable for the subtle row highlight flash
    val flashAnimatable = remember { Animatable(Color.Transparent) }

    // Trigger flash animation exclusively when updateCounter changes (i.e. real price move)
    LaunchedEffect(instrument.updateCounter) {
        if (instrument.updateCounter > 0L) {
            val flashColor = when (instrument.direction) {
                PriceDirection.UP -> MarketUpGreenSubtle
                PriceDirection.DOWN -> MarketDownRedSubtle
                PriceDirection.UNCHANGED -> Color.Transparent
            }
            if (flashColor != Color.Transparent) {
                flashAnimatable.snapTo(flashColor)
                flashAnimatable.animateTo(
                    targetValue = Color.Transparent,
                    animationSpec = tween(durationMillis = 400, easing = FastOutSlowInEasing)
                )
            }
        }
    }

    val accessibilityDesc = "${instrument.name} (${instrument.symbol}), Price ${instrument.formattedPrice}, " +
        "Movement ${instrument.direction.name}, Change ${instrument.formattedChangePercent}"

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(flashAnimatable.value)
            .then(
                if (onClick != null) {
                    Modifier.clickable(onClick = onClick)
                } else {
                    Modifier
                }
            )
            .semantics { contentDescription = accessibilityDesc }
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left Column: Symbol & Instrument Name
            Column(
                modifier = Modifier.weight(1f, fill = false),
                verticalArrangement = Arrangement.Center
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = instrument.baseAsset,
                        style = PriceTextStyle.copy(fontSize = 15.sp),
                        color = TextPrimary
                    )
                    Text(
                        text = "/${instrument.quoteAsset}",
                        style = PriceTextStyle.copy(fontSize = 12.sp, fontWeight = FontWeight.Normal),
                        color = TextTertiary
                    )
                }
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = instrument.name,
                    style = androidx.compose.ui.text.TextStyle(
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Normal,
                        color = TextSecondary
                    ),
                    maxLines = 1
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            // Right Column: Formatted Price & Percent Change Badge
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.Center
            ) {
                // Latest Price with direction indicator
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.End
                ) {
                    Text(
                        text = instrument.formattedPrice,
                        style = PriceTextStyle,
                        color = when (instrument.direction) {
                            PriceDirection.UP -> MarketUpGreen
                            PriceDirection.DOWN -> MarketDownRed
                            PriceDirection.UNCHANGED -> TextPrimary
                        }
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                // Change % badge with arrow icon
                val badgeBgColor = when (instrument.isPositiveChange) {
                    true -> MarketUpGreenSubtle
                    false -> MarketDownRedSubtle
                    null -> DarkSurfaceElevated
                }

                val badgeContentColor = when (instrument.isPositiveChange) {
                    true -> MarketUpGreen
                    false -> MarketDownRed
                    null -> TextSecondary
                }

                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(badgeBgColor)
                        .padding(horizontal = 6.dp, vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    if (instrument.isPositiveChange == true) {
                        Icon(
                            imageVector = Icons.Default.ArrowDropUp,
                            contentDescription = "Price Increased",
                            tint = badgeContentColor,
                            modifier = Modifier.size(16.dp)
                        )
                    } else if (instrument.isPositiveChange == false) {
                        Icon(
                            imageVector = Icons.Default.ArrowDropDown,
                            contentDescription = "Price Decreased",
                            tint = badgeContentColor,
                            modifier = Modifier.size(16.dp)
                        )
                    }

                    Text(
                        text = instrument.formattedChangePercent,
                        style = androidx.compose.ui.text.TextStyle(
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 11.sp,
                            color = badgeContentColor
                        )
                    )
                }
            }
        }
    }
}
