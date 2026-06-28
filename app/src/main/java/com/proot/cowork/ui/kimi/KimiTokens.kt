package com.proot.cowork.ui.kimi

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/** Kimi-inspired palette — true black canvas, elevated cards, mint accent. */
object KimiTokens {
    val Bg = Color(0xFF000000)
    val Card = Color(0xFF1C1C1E)
    val CardElevated = Color(0xFF252528)
    val Border = Color(0xFF2C2C2E)
    val BorderSubtle = Color(0xFF3A3A3C)
    val Accent = Color(0xFF5EEAD4)
    val AccentDim = Color(0xFF2DD4BF)
    val TextPrimary = Color(0xFFF5F5F7)
    val TextSecondary = Color(0xFFAEAEB2)
    val TextMuted = Color(0xFF636366)
    val ThinkBg = Color(0xFF141414)
    val TerminalBg = Color(0xFF0A0A0A)
    val Success = Color(0xFF4ADE80)
    val Error = Color(0xFFF87171)
    val DotInactive = Color(0xFF48484A)

    val RadiusCard = 16.dp
    val RadiusComposer = 28.dp
    val RadiusPill = 20.dp
    val ShapeCard = RoundedCornerShape(RadiusCard)
    val ShapeComposer = RoundedCornerShape(RadiusComposer)
    val ShapePill = RoundedCornerShape(RadiusPill)
}
