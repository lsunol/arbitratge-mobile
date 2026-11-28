package dev.lluissunol.arbitratge.credential

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface

/**
 * Imatge de la credencial per descarregar/compartir (T-01). És una imatge PNG amb el nom, «APTE», la data, el codi curt
 * i el QR ARB1 (el token desat, mai reconstruït). El verificador la llegeix des de «Comprovar una imatge» (jsQR) i, per
 * això, el QR va gran, en negre sobre blanc i amb zona de silenci ≥ 4 mòduls. Cap dada del banc ni secrets.
 */
object CredentialImage {
    const val WIDTH = 1080
    private const val PAD = 72
    private const val BAND = 150
    private val GREEN = Color.rgb(0x1D, 0x6F, 0x42)
    private val INK = Color.rgb(0x1B, 0x26, 0x20)
    private val MUTED = Color.rgb(0x3F, 0x4B, 0x44)

    /** [subtitle] és la línia de resultat («APTE»); [dateLabel]/[dateText] i [codeLabel]/[code] es mostren sota el nom. */
    fun render(
        name: String, subtitle: String, dateLabel: String, dateText: String, codeLabel: String, code: String,
        header: String, qr: QrMatrix,
    ): Bitmap {
        val quiet = 4
        val qrSide = WIDTH - 2 * PAD
        val module = qrSide / (qr.size + 2 * quiet)
        val qrPx = module * (qr.size + 2 * quiet)
        val nameLines = wrap(name, namePaint(), (WIDTH - 2 * PAD).toFloat())
        val height = BAND + PAD + 80 * nameLines.size + 90 + 110 + 40 + qrPx + PAD
        val bmp = Bitmap.createBitmap(WIDTH, height, Bitmap.Config.ARGB_8888)
        val c = Canvas(bmp)
        c.drawColor(Color.WHITE)
        c.drawRect(0f, 0f, WIDTH.toFloat(), BAND.toFloat(), Paint().apply { color = GREEN })
        c.drawText(header, PAD.toFloat(), BAND / 2f + 16f, text(48f, Color.WHITE, bold = true))
        var y = BAND + PAD + 40f
        val np = namePaint()
        for (l in nameLines) { c.drawText(l, PAD.toFloat(), y, np); y += 80f }
        y += 10f
        c.drawText(subtitle, PAD.toFloat(), y + 30f, text(60f, GREEN, bold = true))
        y += 110f
        c.drawText("$dateLabel: $dateText", PAD.toFloat(), y, text(38f, MUTED))
        c.drawText("$codeLabel: $code", PAD.toFloat(), y + 52f, text(38f, MUTED, mono = true))
        y += 40f + 52f
        val left = (WIDTH - qrPx) / 2
        val top = y.toInt()
        val white = Paint().apply { color = Color.WHITE }
        val black = Paint().apply { color = Color.BLACK; isAntiAlias = false }
        c.drawRect(left.toFloat(), top.toFloat(), (left + qrPx).toFloat(), (top + qrPx).toFloat(), white)
        for (yy in 0 until qr.size) for (xx in 0 until qr.size) if (qr[xx, yy]) {
            val x0 = left + (xx + quiet) * module
            val y0 = top + (yy + quiet) * module
            c.drawRect(x0.toFloat(), y0.toFloat(), (x0 + module).toFloat(), (y0 + module).toFloat(), black)
        }
        return bmp
    }

    private fun namePaint() = text(64f, INK, bold = true)

    private fun text(size: Float, color: Int, bold: Boolean = false, mono: Boolean = false) = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textSize = size
        this.color = color
        typeface = Typeface.create(if (mono) Typeface.MONOSPACE else Typeface.SANS_SERIF, if (bold) Typeface.BOLD else Typeface.NORMAL)
    }

    /** Parteix per paraules (i per caràcters si una paraula no hi cap). */
    internal fun wrap(s: String, p: Paint, max: Float): List<String> {
        val out = ArrayList<String>()
        var line = ""
        for (w in s.split(' ')) {
            val t = if (line.isEmpty()) w else "$line $w"
            if (p.measureText(t) <= max) { line = t; continue }
            if (line.isNotEmpty()) out += line
            line = w
            while (p.measureText(line) > max && line.length > 1) {
                var n = line.length - 1
                while (n > 1 && p.measureText(line.substring(0, n)) > max) n--
                out += line.substring(0, n); line = line.substring(n)
            }
        }
        if (line.isNotEmpty()) out += line
        return out
    }
}
