package com.tmc.mobile.ui.pass

import android.graphics.Bitmap
import android.graphics.Color
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color as ComposeColor
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel
import com.tmc.mobile.ui.theme.TmcColors

@Composable
fun QrImage(payload: String, size: Dp, modifier: Modifier = Modifier) {
    val bitmap = remember(payload) { encode(payload).asImageBitmap() }
    Image(
        bitmap = bitmap,
        contentDescription = "Access pass QR code",
        filterQuality = FilterQuality.None,
        modifier = modifier.size(size),
    )
}

@Composable
fun CountdownRing(progress: Float, modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    Box(modifier.size(236.dp), contentAlignment = Alignment.Center) {
        Canvas(Modifier.size(236.dp).padding(3.dp)) {
            val stroke = Stroke(width = 6.dp.toPx(), cap = StrokeCap.Round)
            drawArc(TmcColors.Track, startAngle = 0f, sweepAngle = 360f, useCenter = false, style = stroke)
            drawArc(TmcColors.Primary, startAngle = -90f, sweepAngle = 360f * progress, useCenter = false, style = stroke)
        }
        Box(Modifier.background(ComposeColor.White)) {
            content()
        }
    }
}

private fun encode(payload: String): Bitmap {
    val hints = mapOf(EncodeHintType.ERROR_CORRECTION to ErrorCorrectionLevel.M, EncodeHintType.MARGIN to 1)
    val matrix = QRCodeWriter().encode(payload, BarcodeFormat.QR_CODE, 0, 0, hints)
    val pixels = IntArray(matrix.width * matrix.height) { index ->
        if (matrix.get(index % matrix.width, index / matrix.width)) Color.BLACK else Color.WHITE
    }
    return Bitmap.createBitmap(pixels, matrix.width, matrix.height, Bitmap.Config.ARGB_8888)
}
