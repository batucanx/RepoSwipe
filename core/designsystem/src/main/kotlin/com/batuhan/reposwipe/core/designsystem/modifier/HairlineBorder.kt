package com.batuhan.reposwipe.core.designsystem.modifier

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/** Single-pixel bottom edge used on the glass top bar / floating dock "ghost" borders. */
@Composable
fun Modifier.bottomHairline(color: Color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.15f)): Modifier =
    drawBehind {
        val strokeWidth = 1.dp.toPx()
        drawLine(
            color = color,
            start = Offset(0f, size.height - strokeWidth / 2),
            end = Offset(size.width, size.height - strokeWidth / 2),
            strokeWidth = strokeWidth,
        )
    }
