package com.homegame.mahjong.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel
import com.homegame.mahjong.update.Updater

private const val SHARE_MESSAGE =
    "Play mahjong with me! Open this on your Android phone to download the app:\n${Updater.SHARE_URL}\n\n" +
        "When it finishes, tap the file and allow the install. It updates itself after that."

/** Settings section: a QR code to scan in person, plus buttons to text or copy the download link. */
@Composable
fun ShareSection() {
    val context = LocalContext.current
    Section("Share Mahjong") {
        Text("Friends scan this with their phone camera to download the game.", color = Soft, fontSize = 13.sp)
        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            QrCode(Updater.SHARE_URL)
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            BigButton("Copy link", Modifier.weight(1f)) { copyLink(context) }
            BigButton("Send link", Modifier.weight(1f), primary = true) { sendLink(context) }
        }
        Text("Android phones only.", color = Soft, fontSize = 13.sp)
    }
}

private fun sendLink(context: Context) {
    val send = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_SUBJECT, "Mahjong")
        putExtra(Intent.EXTRA_TEXT, SHARE_MESSAGE)
    }
    context.startActivity(Intent.createChooser(send, "Send Mahjong to…"))
}

private fun copyLink(context: Context) {
    val clipboard = context.getSystemService(ClipboardManager::class.java)
    clipboard.setPrimaryClip(ClipData.newPlainText("Mahjong download link", Updater.SHARE_URL))
    Toast.makeText(context, "Link copied", Toast.LENGTH_SHORT).show()
}

/** Black-on-white QR code drawn square by square, with the white margin scanners need. */
@Composable
private fun QrCode(text: String, modifier: Modifier = Modifier) {
    val matrix = remember(text) {
        QRCodeWriter().encode(
            text, BarcodeFormat.QR_CODE, 0, 0,
            mapOf(EncodeHintType.ERROR_CORRECTION to ErrorCorrectionLevel.M, EncodeHintType.MARGIN to 0),
        )
    }
    Box(modifier.background(Color.White, RoundedCornerShape(12.dp)).padding(14.dp)) {
        Canvas(Modifier.size(172.dp)) {
            val cell = size.width / matrix.width
            for (y in 0 until matrix.height) for (x in 0 until matrix.width) {
                if (matrix.get(x, y)) {
                    // Slight overlap hides hairline gaps between neighbouring squares.
                    drawRect(Color.Black, Offset(x * cell, y * cell), Size(cell + 0.5f, cell + 0.5f))
                }
            }
        }
    }
}
