package com.plutusoft.livepulse.ui.theme

import androidx.compose.ui.graphics.Color

// Earth-Tone Palette (Terracotta, Sage, Honey, Warm Stone Surfaces)
val PrimaryLight = Color(0xFFC2410C)              // Terracotta 700
val OnPrimaryLight = Color(0xFFFFFFFF)
val PrimaryContainerLight = Color(0xFFFFEDD5)     // Clay 100
val OnPrimaryContainerLight = Color(0xFF7C2D12)   // Clay 900

val PrimaryDark = Color(0xFFFB923C)               // Terracotta 400
val OnPrimaryDark = Color(0xFF431407)
val PrimaryContainerDark = Color(0xFF7C2D12)
val OnPrimaryContainerDark = Color(0xFFFFEDD5)

// Secondary Tones (Sage Green)
val SecondaryLight = Color(0xFF4A6B4E)            // Sage 700
val OnSecondaryLight = Color(0xFFFFFFFF)
val SecondaryContainerLight = Color(0xFFE4EBE3)   // Sage 100
val OnSecondaryContainerLight = Color(0xFF14532D) // Green 900

val SecondaryDark = Color(0xFFA3C4A8)             // Sage 400
val OnSecondaryDark = Color(0xFF0F2415)
val SecondaryContainerDark = Color(0xFF14532D)
val OnSecondaryContainerDark = Color(0xFFDCFCE7)

// Tertiary Tones (Honey Amber)
val TertiaryLight = Color(0xFFB45309)             // Honey 700
val OnTertiaryLight = Color(0xFFFFFFFF)
val TertiaryContainerLight = Color(0xFFFEF3C7)    // Honey 100
val OnTertiaryContainerLight = Color(0xFF78350F)  // Honey 900

val TertiaryDark = Color(0xFFFBBF24)              // Honey 400
val OnTertiaryDark = Color(0xFF451A03)
val TertiaryContainerDark = Color(0xFF78350F)
val OnTertiaryContainerDark = Color(0xFFFEF3C7)

// Warm Stone Background & Material 3 Surface Hierarchy (friendly neutrals, earth brand colors)
val BackgroundLight = Color(0xFFFAF9F7)           // Stone 50
val OnBackgroundLight = Color(0xFF1C1917)          // Stone 900
val SurfaceLight = Color(0xFFFEFDFB)              // Warm White
val OnSurfaceLight = Color(0xFF1C1917)             // Stone 900
val SurfaceVariantLight = Color(0xFFF5F4F0)       // Stone 100
val OnSurfaceVariantLight = Color(0xFF57534E)      // Stone 600
val OutlineLight = Color(0xFFA8A29E)              // Stone 400 - prominent boundary
val OutlineVariantLight = Color(0xFFD6D3D1)       // Stone 300 - clear crisp border

// Material 3 Surface Containers (Light)
val SurfaceContainerLowestLight = Color(0xFFFEFDFB)
val SurfaceContainerLowLight = Color(0xFFFAF9F7)
val SurfaceContainerLight = Color(0xFFF5F4F0)
val SurfaceContainerHighLight = Color(0xFFE7E5E4)
val SurfaceContainerHighestLight = Color(0xFFD6D3D1)

// Dark fallback scheme (warm stone)
val BackgroundDark = Color(0xFF0C0A09)
val SurfaceDark = Color(0xFF292524)
val OnBackgroundDark = Color(0xFFFAF9F7)
val OnSurfaceDark = Color(0xFFFAF9F7)
val SurfaceVariantDark = Color(0xFF44403C)
val OnSurfaceVariantDark = Color(0xFFA8A29E)
val OutlineDark = Color(0xFF57534E)
val OutlineVariantDark = Color(0xFF57534E)

// Material 3 Surface Containers (Dark)
val SurfaceContainerLowestDark = Color(0xFF0C0A09)
val SurfaceContainerLowDark = Color(0xFF292524)
val SurfaceContainerDark = Color(0xFF44403C)
val SurfaceContainerHighDark = Color(0xFF57534E)
val SurfaceContainerHighestDark = Color(0xFF78716C)

// Error Tokens
val ErrorLight = Color(0xFFDC2626)
val OnErrorLight = Color(0xFFFFFFFF)
val ErrorContainerLight = Color(0xFFFEE2E2)
val OnErrorContainerLight = Color(0xFF991B1B)

val ErrorDark = Color(0xFFF87171)
val OnErrorDark = Color(0xFF450A0A)
val ErrorContainerDark = Color(0xFF7F1D1D)
val OnErrorContainerDark = Color(0xFFFEE2E2)

// Category & Status Colors (Bright, friendly, modern)
val StatusActive = Color(0xFF4D7C0F)              // Moss 800 — growth on earth
val StatusMaybe = Color(0xFFF59E0B)               // Amber 500 — RSVP "Maybe"
val StatusEndingSoon = Color(0xFFC2410C)          // Terracotta — deadline urgency
val StatusClosed = Color(0xFFA8A29E)              // Stone 400

val CategorySoccer = Color(0xFF15803D)            // Pitch Green 700 — distinct from moss
val CategoryDrinks = Color(0xFF0284C7)            // Sky — kept for the six-crayon spread
val CategoryFood = Color(0xFFF97316)              // Warm Orange
val CategoryDecision = Color(0xFF8B5CF6)          // Purple
val CategoryEvent = Color(0xFFEC4899)             // Pink
val CategoryFeedback = Color(0xFF14B8A6)          // Teal

val GeometricBarFill = Color(0xFFFED7AA)          // Clay 200
val GeometricBarFillDark = Color(0xFF7C2D12)
val GeometricAccent = Color(0xFF9A3412)           // Clay 800
val TopWinnerGold = Color(0xFF92400E)             // Bronze 800

/** Maps a poll category to its brand color; unknown categories fall back to Decision purple. */
fun categoryColor(category: String): Color = when (category.trim().uppercase()) {
    "SOCCER" -> CategorySoccer
    "DRINKS" -> CategoryDrinks
    "FOOD" -> CategoryFood
    "DECISION" -> CategoryDecision
    "EVENT" -> CategoryEvent
    "FEEDBACK" -> CategoryFeedback
    else -> CategoryDecision
}
