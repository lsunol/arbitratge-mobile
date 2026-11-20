package dev.lluissunol.arbitratge.credential

import java.math.BigInteger
import java.security.KeyFactory
import java.security.interfaces.ECPrivateKey
import java.security.interfaces.ECPublicKey
import java.security.spec.ECParameterSpec
import java.security.spec.PKCS8EncodedKeySpec
import java.security.spec.X509EncodedKeySpec

/**
 * Paràmetres de P-256 (secp256r1), conversió de firmes DER ↔ R‖S (§3.7.3) i SPKI (§3.7.9).
 */
object P256 {
    val P: BigInteger = BigInteger("ffffffff00000001000000000000000000000000ffffffffffffffffffffffff", 16)
    val A: BigInteger = P - BigInteger.valueOf(3)
    val B: BigInteger = BigInteger("5ac635d8aa3a93e7b3ebbd55769886bc651d06b0cc53b0f63bce3c3e27d2604b", 16)
    val N: BigInteger = BigInteger("ffffffff00000000ffffffffffffffffbce6faada7179e84f3b9cac2fc632551", 16)
    val GX: BigInteger = BigInteger("6b17d1f2e12c4247f8bce6e563a440f277037d812deb33a0f4a13945d898c296", 16)
    val GY: BigInteger = BigInteger("4fe342e2fe1a7f9b8ee7eb4a7c0f9e162bce33576b315ececbb6406837bf51f5", 16)

    /** Prefix DER fix d'un SPKI `id-ecPublicKey` + `secp256r1` amb punt sense comprimir (91 bytes). */
    private val SPKI_PREFIX: ByteArray = hex("3059301306072a8648ce3d020106082a8648ce3d03010703420004")
    const val SPKI_LENGTH = 91

    // ---------------------------------------------------------------- DER ↔ R‖S

    /**
     * DER `SEQUENCE { r INTEGER, s INTEGER }` → 64 bytes `R‖S` (§3.7.3). Cada `INTEGER` ha de tenir
     * 1–33 bytes de contingut; si en té 33, el primer ha de ser `0x00` i es descarta; després es
     * farceix a 32 amb zeros a l'esquerra. Qualsevol altra forma ⇒ `null` (error d'emissió: no
     * s'emet): valors negatius (bit alt del primer byte), codificació no mínima (un `0x00` inicial
     * només s'admet si el byte següent té el bit alt; per tant, 33 bytes exigeixen `0x00` + byte
     * ≥ `0x80`), longituds fora de 1–33, tipus o longituds DER incoherents i bytes sobrants.
     */
    fun derToRaw(der: ByteArray): ByteArray? {
        if (der.size < 2 || der[0] != 0x30.toByte()) return null
        val seqLen = der[1].toInt() and 0xFF
        // Contingut màxim 2·(2+33) = 70 < 128: sempre forma curta.
        if (seqLen >= 0x80 || seqLen != der.size - 2) return null
        val out = ByteArray(64)
        var p = 2
        for (half in 0..1) {
            if (p + 2 > der.size || der[p] != 0x02.toByte()) return null
            val len = der[p + 1].toInt() and 0xFF
            p += 2
            if (len !in 1..33 || p + len > der.size) return null
            val first = der[p].toInt() and 0xFF
            if (first and 0x80 != 0) return null // enter negatiu
            if (len > 1 && first == 0x00 && der[p + 1].toInt() and 0x80 == 0) return null // no mínim
            if (len == 33 && first != 0x00) return null // > 256 bits
            var start = p
            var contentLen = len
            if (len == 33) {
                start++
                contentLen--
            }
            System.arraycopy(der, start, out, half * 32 + (32 - contentLen), contentLen)
            p += len
        }
        return if (p == der.size) out else null
    }

    /** 64 bytes `R‖S` → DER mínim, per a la verificació amb JCA. */
    fun rawToDer(raw: ByteArray): ByteArray {
        require(raw.size == 64)
        val r = derInteger(raw.copyOfRange(0, 32))
        val s = derInteger(raw.copyOfRange(32, 64))
        return byteArrayOf(0x30, (r.size + s.size).toByte()) + r + s
    }

    private fun derInteger(unsigned32: ByteArray): ByteArray {
        var i = 0
        while (i < unsigned32.size - 1 && unsigned32[i] == 0.toByte()) i++
        var content = unsigned32.copyOfRange(i, unsigned32.size)
        if (content[0].toInt() and 0x80 != 0) content = byteArrayOf(0) + content
        return byteArrayOf(0x02, content.size.toByte()) + content
    }

    fun rOf(raw: ByteArray): BigInteger = BigInteger(1, raw.copyOfRange(0, 32))
    fun sOf(raw: ByteArray): BigInteger = BigInteger(1, raw.copyOfRange(32, 64))

    fun to32(v: BigInteger): ByteArray {
        val b = v.toByteArray()
        return when {
            b.size == 32 -> b
            b.size == 33 && b[0] == 0.toByte() -> b.copyOfRange(1, 33)
            b.size < 32 -> ByteArray(32 - b.size) + b
            else -> throw IllegalArgumentException("valor > 256 bits")
        }
    }

    // ---------------------------------------------------------------- SPKI

    /**
     * Comprova que [spki] és exactament un SPKI P-256 sense comprimir (prefix fix + 64 bytes) i
     * que el punt és a la corba. Retorna la clau JCA o `null`.
     */
    fun publicKeyFromSpki(spki: ByteArray): ECPublicKey? {
        if (spki.size != SPKI_LENGTH) return null
        for (i in SPKI_PREFIX.indices) if (spki[i] != SPKI_PREFIX[i]) return null
        val x = BigInteger(1, spki.copyOfRange(27, 59))
        val y = BigInteger(1, spki.copyOfRange(59, 91))
        if (!isOnCurve(x, y)) return null
        return try {
            val key = KeyFactory.getInstance("EC").generatePublic(X509EncodedKeySpec(spki)) as ECPublicKey
            if (!isP256(key.params)) null else key
        } catch (e: Exception) {
            null
        }
    }

    fun spkiFromPoint(x: BigInteger, y: BigInteger): ByteArray = SPKI_PREFIX + to32(x) + to32(y)

    /** Clau privada PKCS#8 DER → clau JCA (només P-256). */
    fun privateKeyFromPkcs8(pkcs8: ByteArray): ECPrivateKey? =
        try {
            val key = KeyFactory.getInstance("EC").generatePrivate(PKCS8EncodedKeySpec(pkcs8)) as ECPrivateKey
            if (!isP256(key.params) || key.s.signum() <= 0 || key.s >= N) null else key
        } catch (e: Exception) {
            null
        }

    /**
     * SPKI de la clau pública corresponent a una privada PKCS#8 (Q = d·G), per a la
     * correspondència privada↔pública de §3.7.9 (porta G2 de `05` §5.8). No imprimeix res.
     */
    fun spkiFromPkcs8(pkcs8: ByteArray): ByteArray? {
        val key = privateKeyFromPkcs8(pkcs8) ?: return null
        val q = multiplyG(key.s) ?: return null
        return spkiFromPoint(q.first, q.second)
    }

    internal fun isP256(params: ECParameterSpec): Boolean =
        params.order == N && params.generator.affineX == GX && params.generator.affineY == GY &&
            params.curve.a == A && params.curve.b == B && params.cofactor == 1

    fun isOnCurve(x: BigInteger, y: BigInteger): Boolean {
        if (x.signum() < 0 || x >= P || y.signum() < 0 || y >= P) return false
        val lhs = y.multiply(y).mod(P)
        val rhs = x.multiply(x).multiply(x).add(A.multiply(x)).add(B).mod(P)
        return lhs == rhs
    }

    // Aritmètica afí simple (només per a derivar claus públiques; no és camí calent).
    private fun add(p1: Pair<BigInteger, BigInteger>?, p2: Pair<BigInteger, BigInteger>?): Pair<BigInteger, BigInteger>? {
        if (p1 == null) return p2
        if (p2 == null) return p1
        val (x1, y1) = p1
        val (x2, y2) = p2
        val lambda = if (x1 == x2) {
            if ((y1 + y2).mod(P).signum() == 0) return null
            BigInteger.valueOf(3).multiply(x1).multiply(x1).add(A)
                .multiply(BigInteger.valueOf(2).multiply(y1).modInverse(P)).mod(P)
        } else {
            (y2 - y1).multiply((x2 - x1).mod(P).modInverse(P)).mod(P)
        }
        val x3 = (lambda.multiply(lambda) - x1 - x2).mod(P)
        val y3 = (lambda.multiply(x1 - x3) - y1).mod(P)
        return x3 to y3
    }

    internal fun multiplyG(k: BigInteger): Pair<BigInteger, BigInteger>? {
        var result: Pair<BigInteger, BigInteger>? = null
        var addend: Pair<BigInteger, BigInteger>? = GX to GY
        for (i in 0 until k.bitLength()) {
            if (k.testBit(i)) result = add(result, addend)
            addend = add(addend, addend)
        }
        return result
    }

    internal fun hex(s: String): ByteArray = ByteArray(s.length / 2) { s.substring(it * 2, it * 2 + 2).toInt(16).toByte() }

    private const val HEX = "0123456789abcdef"

    internal fun toHex(b: ByteArray): String {
        val sb = StringBuilder(b.size * 2)
        for (x in b) sb.append(HEX[x.toInt() shr 4 and 0xF]).append(HEX[x.toInt() and 0xF])
        return sb.toString()
    }
}
