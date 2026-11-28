package dev.lluissunol.arbitratge.credential

import com.google.zxing.BinaryBitmap
import com.google.zxing.DecodeHintType
import com.google.zxing.MultiFormatReader
import com.google.zxing.RGBLuminanceSource
import com.google.zxing.common.HybridBinarizer
import dev.lluissunol.arbitratge.support.TestEnv
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** T-01: la imatge descarregable conté un QR que es decodifica al mateix token i que el verificador dona per VÀLID. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class CredentialImageTest {
    @Test fun renderedImageDecodesToTheStoredTokenAndVerifies() {
        val token = TestEnv.realIssuingToken()
        val qr = QrEncoder.encode(token)
        val bmp = CredentialImage.render(
            "Maria Puig Vila", "APTE", "Data de l'examen", "20/11/2026", "Codi", "7F3A-92C1", "Credencial d'àrbitre · CFBB", qr,
        )
        assertEquals(CredentialImage.WIDTH, bmp.width)
        val px = IntArray(bmp.width * bmp.height).also { bmp.getPixels(it, 0, bmp.width, 0, 0, bmp.width, bmp.height) }
        val src = RGBLuminanceSource(bmp.width, bmp.height, px)
        val text = MultiFormatReader().decode(
            BinaryBitmap(HybridBinarizer(src)), mapOf(DecodeHintType.TRY_HARDER to true),
        ).text
        assertEquals(token, text)
        val trusted = TestEnv.devTrusted()
        assertEquals(VerificationCode.OK, CredentialVerifier(trusted).verify(text).code)
        assertTrue(bmp.height > bmp.width)
    }
}
