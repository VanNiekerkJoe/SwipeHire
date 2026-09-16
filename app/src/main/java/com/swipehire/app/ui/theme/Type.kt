package com.swipehire.app.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

// Weight extremes (Black display type, tight negative tracking on large
// sizes) do a lot of the "premium" heavy lifting here without needing a
// bundled font file — swap fontFamily below for a custom typeface if one
// gets added to res/font later.
val SwipeHireTypography = Typography(
    displaySmall = TextStyle(fontWeight = FontWeight.Black, fontSize = 38.sp, lineHeight = 42.sp, letterSpacing = (-1.2).sp),
    headlineMedium = TextStyle(fontWeight = FontWeight.ExtraBold, fontSize = 28.sp, lineHeight = 32.sp, letterSpacing = (-0.6).sp),
    headlineSmall = TextStyle(fontWeight = FontWeight.ExtraBold, fontSize = 22.sp, lineHeight = 27.sp, letterSpacing = (-0.4).sp),
    titleLarge = TextStyle(fontWeight = FontWeight.Bold, fontSize = 19.sp, letterSpacing = (-0.2).sp),
    titleMedium = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 16.sp, letterSpacing = (-0.1).sp),
    titleSmall = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 14.sp),
    bodyLarge = TextStyle(fontWeight = FontWeight.Normal, fontSize = 16.sp, lineHeight = 23.sp),
    bodyMedium = TextStyle(fontWeight = FontWeight.Normal, fontSize = 14.sp, lineHeight = 20.sp),
    bodySmall = TextStyle(fontWeight = FontWeight.Normal, fontSize = 12.5.sp, lineHeight = 17.sp),
    labelLarge = TextStyle(fontWeight = FontWeight.Bold, fontSize = 13.sp, letterSpacing = 0.2.sp),
    labelMedium = TextStyle(fontWeight = FontWeight.Bold, fontSize = 11.5.sp, letterSpacing = 0.3.sp),
    labelSmall = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 10.5.sp, letterSpacing = 0.3.sp),
)
