package com.shubham.stockwatchlistdemo.core.model

/**
 * Indicates the directional price movement relative to the immediately preceding price.
 */
enum class PriceDirection {
    /** Price increased relative to the previous observation. */
    UP,

    /** Price decreased relative to the previous observation. */
    DOWN,

    /** Price remained unchanged, or initial baseline is being established. */
    UNCHANGED
}
