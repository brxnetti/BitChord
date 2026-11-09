package com.music.bitchord.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.music.bitchord.R
import com.music.bitchord.data.settings.AppFont

/*
 * Material 3 Expressive theme (branch material3-expressive).
 *
 * Stable androidx material3 1.4.0 keeps MaterialExpressiveTheme,
 * MotionScheme.expressive() and the expressive colour schemes `internal`
 * (verified against the published 1.4.0 sources jar; the full component set
 * only goes public in the 1.5.0-alpha line), so this file is the Expressive
 * theme by hand, on the public API: a complete expressive colour scheme
 * (seeded from the app's own red rather than the stock purple, with the full
 * surface-container tier), the expressive shape scale, and the expressive
 * type scale — bigger, heavier display type and more open body text. The
 * matching component set (ExpressiveButton, ExpressiveButtonGroup,
 * ExpressiveSplitButton, ExpressiveLoadingIndicator, ExpressiveSlider,
 * expressive motion springs) lives in sharedUi's ui/theme/Expressive.kt.
 * See M3X-NOTES.md for what stable 1.4.0 does and does not provide.
 */

// Apple Music's signature red. No longer the primary accent, but kept for the
// spots (Replay's rank badge) that want that specific red regardless of theme.
val AccentRed = Color(0xFFFA2D48)

/**
 * The Expressive seed: the app's own red, as a tonal scheme. Dark mode takes
 * the light, high-chroma cut so it stays vivid on black; light mode takes the
 * deep cut so it holds contrast on white. Secondary is a dusty rose, tertiary
 * a warm amber — the two supporting hues an expressive scheme asks for.
 */
private val DarkColors = darkColorScheme(
    primary = Color(0xFFFF8A94),
    onPrimary = Color(0xFF4A0010),
    primaryContainer = Color(0xFF8E1B30),
    onPrimaryContainer = Color(0xFFFFDAD9),
    secondary = Color(0xFFE8B4BC),
    onSecondary = Color(0xFF44262C),
    secondaryContainer = Color(0xFF5C333A),
    onSecondaryContainer = Color(0xFFFFDAD9),
    tertiary = Color(0xFFF2C48D),
    onTertiary = Color(0xFF452A00),
    tertiaryContainer = Color(0xFF5F3D16),
    onTertiaryContainer = Color(0xFFFFDDB3),
    background = Color(0xFF0A0A0C),
    onBackground = Color(0xFFF5F1F1),
    surface = Color(0xFF121214),
    onSurface = Color(0xFFF5F1F1),
    surfaceVariant = Color(0xFF26262C),
    onSurfaceVariant = Color(0xFFC9C4CC),
    surfaceDim = Color(0xFF0A0A0C),
    surfaceBright = Color(0xFF3A3A41),
    surfaceContainerLowest = Color(0xFF000000),
    surfaceContainerLow = Color(0xFF16161A),
    surfaceContainer = Color(0xFF1E1E23),
    surfaceContainerHigh = Color(0xFF2A2A31),
    surfaceContainerHighest = Color(0xFF36363F),
    outline = Color(0xFF3A3A42),
    outlineVariant = Color(0xFF2A2A31),
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005),
    errorContainer = Color(0xFF93000A),
    onErrorContainer = Color(0xFFFFDAD6),
    scrim = Color.Black,
    inverseSurface = Color(0xFFEDE8EA),
    inverseOnSurface = Color(0xFF1B1B1F),
    inversePrimary = Color(0xFFB81E35),
)

private val LightColors = lightColorScheme(
    primary = Color(0xFFC8102E),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFFFDAD9),
    onPrimaryContainer = Color(0xFF41000D),
    secondary = Color(0xFF7A4A52),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFF3D9DC),
    onSecondaryContainer = Color(0xFF3A1F24),
    tertiary = Color(0xFF7A5410),
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFFFDDB3),
    onTertiaryContainer = Color(0xFF2E1D00),
    background = Color(0xFFFCF8F8),
    onBackground = Color(0xFF1C1B1C),
    surface = Color(0xFFFCF8F8),
    onSurface = Color(0xFF1C1B1C),
    surfaceVariant = Color(0xFFF0E6E9),
    onSurfaceVariant = Color(0xFF5B5658),
    surfaceDim = Color(0xFFE0D8D9),
    surfaceBright = Color(0xFFFCF8F8),
    surfaceContainerLowest = Color.White,
    surfaceContainerLow = Color(0xFFF7F2F3),
    surfaceContainer = Color(0xFFF0EAEA),
    surfaceContainerHigh = Color(0xFFE9E1E3),
    surfaceContainerHighest = Color(0xFFDFD6D8),
    outline = Color(0xFFB9B0B3),
    outlineVariant = Color(0xFFD8CFD1),
    error = Color(0xFFBA1A1A),
    onError = Color.White,
    errorContainer = Color(0xFFFFDAD6),
    onErrorContainer = Color(0xFF410002),
    scrim = Color.Black,
    inverseSurface = Color(0xFF2B292A),
    inverseOnSurface = Color(0xFFF5F0F1),
    inversePrimary = Color(0xFFFFB3B8),
)

/**
 * The Expressive shape scale: every step one notch rounder than classic M3,
 * up to a 40dp "extra large increased" for hero containers (sheets, Now
 * Playing artwork, large cards). Buttons and chips read their shapes from
 * here through MaterialTheme, so the whole app rounds up together.
 */
val BitChordExpressiveShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(32.dp),
    extraLargeIncreased = RoundedCornerShape(40.dp),
)

/**
 * SF Pro Display, the face Apple Music itself is set in, drawn one weight
 * lighter than it is asked for.
 *
 * Measured against Apple Music's own screens, every weight here came out a
 * cut heavier than the same text there: its titles are Semibold where these
 * asked for Bold, its metadata Medium where these asked for Semibold. The
 * shift is made once, in the family, so it reaches the type scale and every
 * weight set in place alike and keeps the steps between them: Heavy draws
 * Bold, Bold draws Semibold, Semibold draws Medium, Medium draws Regular.
 * Regular stays Regular — no lighter cut is bundled, and Apple's body text is
 * Regular too. A FontWeight in this app therefore names the step it sits at in
 * the scale, not the cut it draws.
 *
 * Compose synthesises nothing, so a weight outside these falls back to the
 * nearest one listed.
 */
val SFProDisplay = FontFamily(
    Font(R.font.sf_pro_display_regular, FontWeight.W400),
    Font(R.font.sf_pro_display_regular, FontWeight.W500),
    Font(R.font.sf_pro_display_medium, FontWeight.W600),
    Font(R.font.sf_pro_display_semibold, FontWeight.W700),
    Font(R.font.sf_pro_display_bold, FontWeight.W800),
)

/*
 * Letter spacing measured off Apple Music's own screens: the spacing the
 * bundled Display cut needs, at the weight it draws, to set a line to the
 * width Apple sets it. Apple sets text under 20pt in SF Pro Text, which is
 * spaced looser than Display, so the small sizes open up — about +0.25 to
 * +0.4 at 12 to 14 — and the spacing closes as the size grows, to just under
 * nothing for semibold titles at 18 to 24. The large titles had nothing to
 * measure against and follow the same trend.
 *
 * Expressive (branch material3-expressive): the display and headline steps
 * grow — 44/38/32sp display, 32/28/24sp headlines — and titles open a step,
 * so the hierarchy reads at a glance. Body and label sizes stay where they
 * were; Expressive is loud at the top, calm in the reading text.
 */
private val BitChordTypography = Typography(
    displayLarge = TextStyle(fontWeight = FontWeight.W800, fontSize = 44.sp, letterSpacing = (-0.5).sp),
    displayMedium = TextStyle(fontWeight = FontWeight.W800, fontSize = 38.sp, letterSpacing = (-0.5).sp),
    displaySmall = TextStyle(fontWeight = FontWeight.W800, fontSize = 32.sp, letterSpacing = (-0.4).sp),
    headlineLarge = TextStyle(fontWeight = FontWeight.W800, fontSize = 32.sp, letterSpacing = (-0.4).sp),
    headlineMedium = TextStyle(fontWeight = FontWeight.W700, fontSize = 28.sp, letterSpacing = (-0.25).sp),
    headlineSmall = TextStyle(fontWeight = FontWeight.W700, fontSize = 24.sp, letterSpacing = (-0.2).sp),
    titleLarge = TextStyle(fontWeight = FontWeight.W700, fontSize = 24.sp, letterSpacing = (-0.2).sp),
    titleMedium = TextStyle(fontWeight = FontWeight.W600, fontSize = 18.sp, letterSpacing = 0.05.sp),
    titleSmall = TextStyle(fontWeight = FontWeight.W600, fontSize = 16.sp, letterSpacing = 0.05.sp),
    bodyLarge = TextStyle(fontWeight = FontWeight.W400, fontSize = 17.sp, letterSpacing = 0.2.sp),
    bodyMedium = TextStyle(fontWeight = FontWeight.W400, fontSize = 15.sp, letterSpacing = 0.3.sp),
    bodySmall = TextStyle(fontWeight = FontWeight.W400, fontSize = 13.sp, letterSpacing = 0.3.sp),
    labelLarge = TextStyle(fontWeight = FontWeight.W600, fontSize = 15.sp, letterSpacing = 0.1.sp),
    labelMedium = TextStyle(fontWeight = FontWeight.W600, fontSize = 13.sp, letterSpacing = 0.25.sp),
    labelSmall = TextStyle(fontWeight = FontWeight.W600, fontSize = 12.sp, letterSpacing = 0.3.sp),
).withFamily(SFProDisplay)

/**
 * SF Pro Display as the app first shipped it, before [SFProDisplay] moved every
 * weight a cut lighter: each weight draws its own cut, Heavy included. Kept for
 * [AppFont.LEGACY].
 */
val SFProDisplayLegacy = FontFamily(
    Font(R.font.sf_pro_display_regular, FontWeight.W400),
    Font(R.font.sf_pro_display_medium, FontWeight.W500),
    Font(R.font.sf_pro_display_semibold, FontWeight.W600),
    Font(R.font.sf_pro_display_bold, FontWeight.W700),
    Font(R.font.sf_pro_display_heavy, FontWeight.W800),
)

// Heavy, tight typography — the original scale [AppFont.LEGACY] restores,
// with the same Expressive display/headline growth as the default scale.
private val LegacyTypography = Typography(
    displayLarge = TextStyle(fontWeight = FontWeight.W800, fontSize = 44.sp, letterSpacing = (-0.8).sp),
    displayMedium = TextStyle(fontWeight = FontWeight.W800, fontSize = 38.sp, letterSpacing = (-0.8).sp),
    displaySmall = TextStyle(fontWeight = FontWeight.W800, fontSize = 32.sp, letterSpacing = (-0.7).sp),
    headlineLarge = TextStyle(fontWeight = FontWeight.W800, fontSize = 32.sp, letterSpacing = (-0.7).sp),
    headlineMedium = TextStyle(fontWeight = FontWeight.W700, fontSize = 28.sp, letterSpacing = (-0.4).sp),
    headlineSmall = TextStyle(fontWeight = FontWeight.W700, fontSize = 24.sp, letterSpacing = (-0.3).sp),
    titleLarge = TextStyle(fontWeight = FontWeight.W700, fontSize = 24.sp, letterSpacing = (-0.3).sp),
    titleMedium = TextStyle(fontWeight = FontWeight.W600, fontSize = 18.sp, letterSpacing = (-0.2).sp),
    titleSmall = TextStyle(fontWeight = FontWeight.W600, fontSize = 16.sp, letterSpacing = (-0.2).sp),
    bodyLarge = TextStyle(fontWeight = FontWeight.W400, fontSize = 17.sp),
    bodyMedium = TextStyle(fontWeight = FontWeight.W400, fontSize = 15.sp),
    bodySmall = TextStyle(fontWeight = FontWeight.W400, fontSize = 13.sp),
    labelLarge = TextStyle(fontWeight = FontWeight.W600, fontSize = 15.sp),
    labelMedium = TextStyle(fontWeight = FontWeight.W600, fontSize = 13.sp),
    labelSmall = TextStyle(fontWeight = FontWeight.W600, fontSize = 12.sp),
).withFamily(SFProDisplayLegacy)

/** Applies [family] to every style in the scale, so nothing is left on Roboto. */
private fun Typography.withFamily(family: FontFamily) = Typography(
    displayLarge = displayLarge.copy(fontFamily = family),
    displayMedium = displayMedium.copy(fontFamily = family),
    displaySmall = displaySmall.copy(fontFamily = family),
    headlineLarge = headlineLarge.copy(fontFamily = family),
    headlineMedium = headlineMedium.copy(fontFamily = family),
    headlineSmall = headlineSmall.copy(fontFamily = family),
    titleLarge = titleLarge.copy(fontFamily = family),
    titleMedium = titleMedium.copy(fontFamily = family),
    titleSmall = titleSmall.copy(fontFamily = family),
    bodyLarge = bodyLarge.copy(fontFamily = family),
    bodyMedium = bodyMedium.copy(fontFamily = family),
    bodySmall = bodySmall.copy(fontFamily = family),
    labelLarge = labelLarge.copy(fontFamily = family),
    labelMedium = labelMedium.copy(fontFamily = family),
    labelSmall = labelSmall.copy(fontFamily = family),
)

/**
 * The app's theme, now Material 3 Expressive: expressive colour scheme,
 * expressive shapes and expressive type, through the public
 * [MaterialTheme] — the only theme call site in the app, so every screen
 * inherits it. Kept under the old name and signature so no call site moves.
 */
@Composable
fun BitChordTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    font: AppFont = AppFont.DEFAULT,
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        shapes = BitChordExpressiveShapes,
        typography = when (font) {
            AppFont.DEFAULT -> BitChordTypography
            AppFont.LEGACY -> LegacyTypography
        },
        content = content,
    )
}

/** Alias that says what [BitChordTheme] now is, for new call sites. */
@Composable
fun BitChordExpressiveTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    font: AppFont = AppFont.DEFAULT,
    content: @Composable () -> Unit,
) = BitChordTheme(darkTheme = darkTheme, font = font, content = content)

/**
 * Draws the status and navigation bar glyphs dark or light.
 *
 * `enableEdgeToEdge()` decides this from the *system* dark-mode setting, which
 * is the wrong input the moment the in-app theme disagrees with it: Light theme
 * on a phone in dark mode left white icons on a white bar, invisible. The bars
 * have to follow the content the app is actually painting. The player supplies
 * its own stable light-icon value and paints contrast behind it; every other
 * surface follows the theme. Hence a parameter rather than reading it here.
 */
@Composable
fun SystemBarIcons(dark: Boolean) {
    val view = LocalView.current
    if (view.isInEditMode) return
    val window = findWindow(view) ?: return
    SideEffect {
        WindowCompat.getInsetsController(window, view).apply {
            isAppearanceLightStatusBars = dark
            isAppearanceLightNavigationBars = dark
        }
    }
}
