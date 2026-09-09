package com.corbymaupin.jobsitespanish.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val JobsiteBg = Color(0xFF0E0F12)
val JobsiteSurface = Color(0xFF16181D)
val JobsiteCard = Color(0xFF1C1F26)
val JobsiteAccent = Color(0xFF3DDC97)
val JobsiteAccentDim = Color(0xFF2A9B6A)
val JobsiteBad = Color(0xFFE85D5D)
val JobsiteMuted = Color(0xFF9AA0A6)
val JobsiteText = Color(0xFFF2F3F5)

private val DarkColors = darkColorScheme(
    primary = JobsiteAccent,
    onPrimary = Color(0xFF003822),
    secondary = JobsiteAccentDim,
    background = JobsiteBg,
    onBackground = JobsiteText,
    surface = JobsiteSurface,
    onSurface = JobsiteText,
    surfaceVariant = JobsiteCard,
    onSurfaceVariant = JobsiteMuted,
    error = JobsiteBad,
    onError = Color.White
)

@Composable
fun JobsiteAppTheme(content: @Composable () -> Unit) {
    // Always dark — matches web #0E0F12 vibe
    MaterialTheme(
        colorScheme = DarkColors,
        content = content
    )
}
