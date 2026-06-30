package com.example.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.example.R

val TimesNewRoman = FontFamily(
    Font(resId = R.font.times_new_roman_regular, weight = FontWeight.Normal),
    Font(resId = R.font.times_new_roman_bold, weight = FontWeight.Bold)
)

// Set of Material typography styles to use Times New Roman
val Typography = Typography(
    displayLarge = TextStyle(fontFamily = TimesNewRoman),
    displayMedium = TextStyle(fontFamily = TimesNewRoman),
    displaySmall = TextStyle(fontFamily = TimesNewRoman),
    headlineLarge = TextStyle(fontFamily = TimesNewRoman),
    headlineMedium = TextStyle(fontFamily = TimesNewRoman),
    headlineSmall = TextStyle(fontFamily = TimesNewRoman),
    titleLarge = TextStyle(fontFamily = TimesNewRoman),
    titleMedium = TextStyle(fontFamily = TimesNewRoman),
    titleSmall = TextStyle(fontFamily = TimesNewRoman),
    bodyLarge = TextStyle(
        fontFamily = TimesNewRoman,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.5.sp,
    ),
    bodyMedium = TextStyle(fontFamily = TimesNewRoman),
    bodySmall = TextStyle(fontFamily = TimesNewRoman),
    labelLarge = TextStyle(fontFamily = TimesNewRoman),
    labelMedium = TextStyle(fontFamily = TimesNewRoman),
    labelSmall = TextStyle(fontFamily = TimesNewRoman)
)


