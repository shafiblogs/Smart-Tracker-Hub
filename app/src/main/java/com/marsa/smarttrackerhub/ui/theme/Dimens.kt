package com.marsa.smarttracker.ui.theme

import androidx.compose.ui.unit.dp

/**
 * Unified spacing and shape scale for TrackerHub.
 * One scale across all screens and cards, so rhythms are consistent and future tweaks
 * are centralized.
 */

// Spacing values — used in verticalArrangement.spacedBy() and padding()
val spaceXs = 4.dp     // small gaps within a metric pair
val spaceSm = 8.dp     // gap between row items (label→value)
val spaceMd = 12.dp    // list item gap, section→section inside a card
val spaceLg = 16.dp    // card internal padding, screen padding
val spaceXl = 24.dp    // major divisions inside tall cards

// Shape
val cardRadius = 16.dp  // all five card families

// Bottom nav inset — cards need this much bottom space so they don't hide behind nav
val navBarInset = 88.dp
