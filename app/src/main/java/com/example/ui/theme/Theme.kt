package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp

private val LightColorScheme = lightColorScheme(
  primary = AccentCharcoal,
  onPrimary = SurfaceCardLight,
  primaryContainer = SurfaceVariantLight,
  onPrimaryContainer = TextPrimaryLight,
  secondary = TextSecondaryLight,
  onSecondary = SurfaceCardLight,
  background = CanvasBackgroundLight,
  onBackground = TextPrimaryLight,
  surface = SurfaceCardLight,
  onSurface = TextPrimaryLight,
  surfaceVariant = SurfaceVariantLight,
  onSurfaceVariant = TextSecondaryLight,
  outline = BorderLight,
  error = AlertTextLight,
  onError = SurfaceCardLight
)

private val DarkColorScheme = darkColorScheme(
  primary = AccentCharcoalDark,
  onPrimary = CanvasBackgroundDark,
  primaryContainer = SurfaceVariantDark,
  onPrimaryContainer = TextPrimaryDark,
  secondary = TextSecondaryDark,
  onSecondary = CanvasBackgroundDark,
  background = CanvasBackgroundDark,
  onBackground = TextPrimaryDark,
  surface = SurfaceCardDark,
  onSurface = TextPrimaryDark,
  surfaceVariant = SurfaceVariantDark,
  onSurfaceVariant = TextSecondaryDark,
  outline = BorderDark,
  error = AlertTextDark,
  onError = CanvasBackgroundDark
)

@Immutable
data class EquilibrioColors(
  val canvasBackground: Color,
  val surfaceCard: Color,
  val surfaceVariant: Color,
  val border: Color,
  val textPrimary: Color,
  val textSecondary: Color,
  val textTertiary: Color,
  val accentPrimary: Color,
  val onAccentPrimary: Color,
  val heroSurface: Color,
  val onHeroSurface: Color,
  val heroBorder: Color,
  val completedCard: Color,
  val offDayCard: Color,
  val completedBorder: Color,
  val completedBadge: Color,
  val completedContent: Color,
  val completedTitle: Color,
  val completedSubtitle: Color,
  val successContainer: Color,
  val successBadge: Color,
  val successBorder: Color,
  val successText: Color,
  val successStrongText: Color,
  val successBodyText: Color,
  val warningContainer: Color,
  val warningBorder: Color,
  val warningText: Color,
  val warningAccent: Color,
  val streakFlame: Color,
  val infoContainer: Color,
  val infoBorder: Color,
  val infoIcon: Color,
  val infoText: Color,
  val alertContainer: Color,
  val alertBadge: Color,
  val alertBorder: Color,
  val alertText: Color,
  val alertStrongText: Color,
  val alertSubtitle: Color,
  val radarDashed: Color,
  val radarLegendDashed: Color,
  val radarVertexRing: Color,
  val scrim: Color,
  val iconChipBg: Color,
  val avatarWarmBg: Color,
  val avatarWarmFg: Color,
  val heroGold: Color,
  val heroFrost: Color,
  val loginCanvas: Color,
  val loginSquare: Color,
  val loginInk: Color,
  val loginOnInk: Color,
  val loginMuted: Color
)

private val LightEquilibrioColors = EquilibrioColors(
  canvasBackground = CanvasBackgroundLight,
  surfaceCard = SurfaceCardLight,
  surfaceVariant = SurfaceVariantLight,
  border = BorderLight,
  textPrimary = TextPrimaryLight,
  textSecondary = TextSecondaryLight,
  textTertiary = TextTertiaryLight,
  accentPrimary = AccentCharcoal,
  onAccentPrimary = SurfaceCardLight,
  heroSurface = HeroSurfaceLight,
  onHeroSurface = OnHeroSurfaceLight,
  heroBorder = Color.Transparent,
  completedCard = CompletedCardLight,
  offDayCard = OffDayCardLight,
  completedBorder = CompletedBorderLight,
  completedBadge = CompletedBadgeBgLight,
  completedContent = CompletedContentLight,
  completedTitle = CompletedTitleLight,
  completedSubtitle = CompletedSubtitleLight,
  successContainer = SuccessContainerLight,
  successBadge = SuccessBadgeLight,
  successBorder = SuccessBorderLight,
  successText = SuccessTextLight,
  successStrongText = SuccessStrongTextLight,
  successBodyText = SuccessBodyTextLight,
  warningContainer = WarningContainerLight,
  warningBorder = WarningBorderLight,
  warningText = WarningTextLight,
  warningAccent = WarningAccentLight,
  streakFlame = StreakFlameLight,
  infoContainer = InfoContainerLight,
  infoBorder = InfoBorderLight,
  infoIcon = InfoIconLight,
  infoText = InfoTextLight,
  alertContainer = AlertContainerLight,
  alertBadge = AlertBadgeLight,
  alertBorder = AlertBorderLight,
  alertText = AlertTextLight,
  alertStrongText = AlertStrongTextLight,
  alertSubtitle = AlertSubtitleLight,
  radarDashed = RadarDashedLight,
  radarLegendDashed = RadarLegendDashedLight,
  radarVertexRing = RadarVertexRingLight,
  scrim = ScrimLight,
  iconChipBg = IconChipBgLight,
  avatarWarmBg = AvatarWarmBgLight,
  avatarWarmFg = AvatarWarmFgLight,
  heroGold = HeroGold,
  heroFrost = HeroFrost,
  loginCanvas = LoginEditorialCanvas,
  loginSquare = LoginEditorialSquare,
  loginInk = LoginEditorialInk,
  loginOnInk = LoginEditorialOnInk,
  loginMuted = LoginEditorialMuted
)

private val DarkEquilibrioColors = EquilibrioColors(
  canvasBackground = CanvasBackgroundDark,
  surfaceCard = SurfaceCardDark,
  surfaceVariant = SurfaceVariantDark,
  border = BorderDark,
  textPrimary = TextPrimaryDark,
  textSecondary = TextSecondaryDark,
  textTertiary = TextTertiaryDark,
  accentPrimary = AccentCharcoalDark,
  onAccentPrimary = CanvasBackgroundDark,
  heroSurface = HeroSurfaceDark,
  onHeroSurface = OnHeroSurfaceDark,
  heroBorder = BorderDark,
  completedCard = CompletedCardDark,
  offDayCard = OffDayCardDark,
  completedBorder = CompletedBorderDark,
  completedBadge = CompletedBadgeBgDark,
  completedContent = CompletedContentDark,
  completedTitle = CompletedTitleDark,
  completedSubtitle = CompletedSubtitleDark,
  successContainer = SuccessContainerDark,
  successBadge = SuccessBadgeDark,
  successBorder = SuccessBorderDark,
  successText = SuccessTextDark,
  successStrongText = SuccessStrongTextDark,
  successBodyText = SuccessBodyTextDark,
  warningContainer = WarningContainerDark,
  warningBorder = WarningBorderDark,
  warningText = WarningTextDark,
  warningAccent = WarningAccentDark,
  streakFlame = StreakFlameDark,
  infoContainer = InfoContainerDark,
  infoBorder = InfoBorderDark,
  infoIcon = InfoIconDark,
  infoText = InfoTextDark,
  alertContainer = AlertContainerDark,
  alertBadge = AlertBadgeDark,
  alertBorder = AlertBorderDark,
  alertText = AlertTextDark,
  alertStrongText = AlertStrongTextDark,
  alertSubtitle = AlertSubtitleDark,
  radarDashed = RadarDashedDark,
  radarLegendDashed = RadarLegendDashedDark,
  radarVertexRing = RadarVertexRingDark,
  scrim = ScrimDark,
  iconChipBg = IconChipBgDark,
  avatarWarmBg = AvatarWarmBgDark,
  avatarWarmFg = AvatarWarmFgDark,
  heroGold = HeroGold,
  heroFrost = HeroFrost,
  loginCanvas = LoginEditorialCanvas,
  loginSquare = LoginEditorialSquare,
  loginInk = LoginEditorialInk,
  loginOnInk = LoginEditorialOnInk,
  loginMuted = LoginEditorialMuted
)

val LocalEquilibrioColors = staticCompositionLocalOf { LightEquilibrioColors }

val MaterialTheme.equilibrioColors: EquilibrioColors
  @Composable
  @ReadOnlyComposable
  get() = LocalEquilibrioColors.current

val AppShapes = Shapes(
  small = RoundedCornerShape(12.dp),
  medium = RoundedCornerShape(18.dp),
  large = RoundedCornerShape(26.dp),
  extraLarge = RoundedCornerShape(32.dp)
)

@Composable
fun MyApplicationTheme(
  darkTheme: Boolean = isSystemInDarkTheme(),
  dynamicColor: Boolean = false, // Keep clean minimal palette consistent
  content: @Composable () -> Unit,
) {
  val colorScheme = when {
    dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
      val context = LocalContext.current
      if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
    }
    darkTheme -> DarkColorScheme
    else -> LightColorScheme
  }
  val semanticColors = if (darkTheme) DarkEquilibrioColors else LightEquilibrioColors

  CompositionLocalProvider(LocalEquilibrioColors provides semanticColors) {
    MaterialTheme(
      colorScheme = colorScheme,
      typography = Typography,
      shapes = AppShapes,
      content = content
    )
  }
}
