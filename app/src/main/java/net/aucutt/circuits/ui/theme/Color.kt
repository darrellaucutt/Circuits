package net.aucutt.circuits.ui.theme

import androidx.compose.ui.graphics.Color

// Icon palette — athletic robot with exposed circuits and runner headband

// Background & surfaces — matched to banner_running_robot.png
val BannerBlack = Color(0xFF0A0A0A)
val BannerCharcoal = Color(0xFF111111)
val BannerSurface = Color(0xFF141414)
val BannerSurfaceVariant = Color(0xFF1C1C1C)
val BannerGlowCyan = Color(0xFF0A1520)
val BannerGlowOrange = Color(0xFF1A0E08)

// Circuit cyan — visor, circuit traces, teal glow
val CircuitCyan = Color(0xFF00E5FF)
val CircuitCyanBright = Color(0xFF4DF0FF)
val CircuitCyanDim = Color(0xFF00BCD4)
val CircuitTeal = Color(0xFF14B8A6)
val CircuitTealGlow = Color(0xFF0D9488)

// Runner orange — headband, pulse nodes, shoulder accents
val RunnerOrange = Color(0xFFFF7043)
val RunnerOrangeBright = Color(0xFFFF8C42)
val RunnerOrangeDim = Color(0xFFE65100)
val PulseOrange = Color(0xFFFF6B35)

// Robot metal — silver plating
val RobotSilver = Color(0xFFB0BEC5)
val RobotSilverLight = Color(0xFFCFD8DC)
val RobotSilverDark = Color(0xFF78909C)
val RobotMetal = Color(0xFF90A4AE)

// Interval training semantics
val WorkInterval = RunnerOrange
val RestInterval = CircuitCyan
val HeartbeatPulse = PulseOrange
val CircuitTrace = CircuitCyanDim

// Material 3 — dark theme (primary app look)
val PrimaryDark = CircuitCyan
val OnPrimaryDark = BannerBlack
val PrimaryContainerDark = Color(0xFF004D56)
val OnPrimaryContainerDark = CircuitCyanBright

val SecondaryDark = RunnerOrange
val OnSecondaryDark = BannerBlack
val SecondaryContainerDark = Color(0xFF5C2E12)
val OnSecondaryContainerDark = RunnerOrangeBright

val TertiaryDark = CircuitTeal
val OnTertiaryDark = BannerBlack
val TertiaryContainerDark = Color(0xFF0F3D38)
val OnTertiaryContainerDark = Color(0xFF5EEAD4)

val BackgroundDark = BannerBlack
val OnBackgroundDark = RobotSilverLight
val SurfaceDark = BannerCharcoal
val OnSurfaceDark = RobotSilverLight
val SurfaceVariantDark = BannerSurfaceVariant
val OnSurfaceVariantDark = RobotSilver
val OutlineDark = RobotSilverDark
val OutlineVariantDark = Color(0xFF2A2A2A)

val ErrorDark = Color(0xFFFF5252)
val OnErrorDark = Color(0xFF1A0000)

// Material 3 — light theme
val PrimaryLight = Color(0xFF006874)
val OnPrimaryLight = Color(0xFFFFFFFF)
val PrimaryContainerLight = Color(0xFF97F0FF)
val OnPrimaryContainerLight = Color(0xFF001F24)

val SecondaryLight = Color(0xFFB04A1C)
val OnSecondaryLight = Color(0xFFFFFFFF)
val SecondaryContainerLight = Color(0xFFFFDBC9)
val OnSecondaryContainerLight = Color(0xFF3A0B00)

val TertiaryLight = Color(0xFF006B5F)
val OnTertiaryLight = Color(0xFFFFFFFF)
val TertiaryContainerLight = Color(0xFF7AF8E4)
val OnTertiaryContainerLight = Color(0xFF00201B)

val BackgroundLight = Color(0xFFF5FAFB)
val OnBackgroundLight = Color(0xFF171D1E)
val SurfaceLight = Color(0xFFF5FAFB)
val OnSurfaceLight = Color(0xFF171D1E)
val SurfaceVariantLight = Color(0xFFDBE4E6)
val OnSurfaceVariantLight = Color(0xFF3F484A)
val OutlineLight = Color(0xFF6F797B)
val OutlineVariantLight = Color(0xFFBFC8CA)

val ErrorLight = Color(0xFFBA1A1A)
val OnErrorLight = Color(0xFFFFFFFF)
