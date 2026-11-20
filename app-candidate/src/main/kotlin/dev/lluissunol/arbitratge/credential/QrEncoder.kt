package dev.lluissunol.arbitratge.credential

import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel

/** Matriz de módulos del QR (sin zona de silencio; la añade quien dibuja: ≥ 4 módulos, 03 §6.5). */
class QrMatrix(val size: Int, private val bits: BooleanArray) {
    operator fun get(x: Int, y: Int): Boolean = bits[y * size + x]
}

/** ZXing `core`, solo codificación; corrección M (03 §6.5). El mismo objeto servirá al PDF/PNG (hito 2). */
object QrEncoder {
    fun encode(text: String): QrMatrix {
        val hints = mapOf(
            EncodeHintType.ERROR_CORRECTION to ErrorCorrectionLevel.M,
            EncodeHintType.MARGIN to 0,
            EncodeHintType.CHARACTER_SET to "ISO-8859-1", // el token es ASCII (base64url + '.')
        )
        val m = QRCodeWriter().encode(text, BarcodeFormat.QR_CODE, 0, 0, hints)
        val n = m.width
        val bits = BooleanArray(n * n)
        for (y in 0 until n) for (x in 0 until n) bits[y * n + x] = m.get(x, y)
        return QrMatrix(n, bits)
    }
}
