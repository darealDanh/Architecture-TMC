package com.tmc.mobile.ui.components

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathBuilder
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

object TmcIcons {

    val Pass: ImageVector by lazy {
        outline("pass") {
            rect(4f, 4f, 6f, 6f)
            rect(14f, 4f, 6f, 6f)
            rect(4f, 14f, 6f, 6f)
            rect(14f, 14f, 2f, 2f)
            rect(18f, 18f, 2f, 2f)
            moveTo(14f, 19f)
            lineTo(16f, 19f)
        }
    }

    val Attendance: ImageVector by lazy {
        outline("attendance") {
            moveTo(12f, 4f)
            arcTo(8f, 8f, 0f, isMoreThanHalf = true, isPositiveArc = true, x1 = 11.99f, y1 = 4f)
            moveTo(12f, 8f)
            lineTo(12f, 12f)
            lineTo(15f, 14f)
        }
    }

    val Device: ImageVector by lazy {
        outline("device") {
            moveTo(9f, 3f)
            lineTo(15f, 3f)
            arcTo(2f, 2f, 0f, isMoreThanHalf = false, isPositiveArc = true, x1 = 17f, y1 = 5f)
            lineTo(17f, 19f)
            arcTo(2f, 2f, 0f, isMoreThanHalf = false, isPositiveArc = true, x1 = 15f, y1 = 21f)
            lineTo(9f, 21f)
            arcTo(2f, 2f, 0f, isMoreThanHalf = false, isPositiveArc = true, x1 = 7f, y1 = 19f)
            lineTo(7f, 5f)
            arcTo(2f, 2f, 0f, isMoreThanHalf = false, isPositiveArc = true, x1 = 9f, y1 = 3f)
            close()
            moveTo(11f, 18f)
            lineTo(13f, 18f)
        }
    }

    private fun outline(name: String, block: PathBuilder.() -> Unit): ImageVector =
        ImageVector.Builder(name, 24.dp, 24.dp, 24f, 24f)
            .path(
                fill = null,
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 1.8f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
                pathBuilder = block,
            )
            .build()

    private fun PathBuilder.rect(x: Float, y: Float, width: Float, height: Float) {
        moveTo(x, y)
        lineTo(x + width, y)
        lineTo(x + width, y + height)
        lineTo(x, y + height)
        close()
    }
}
