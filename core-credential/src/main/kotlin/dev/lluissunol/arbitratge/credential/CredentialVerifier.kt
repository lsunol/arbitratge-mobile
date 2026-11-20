package dev.lluissunol.arbitratge.credential

import java.math.BigInteger
import java.security.Signature

/**
 * Resultat de verificar un text (§3.7.8).
 *
 * - [credential]: només si [code] és [VerificationCode.OK] (dades del segell).
 * - [untrustedMembers]: membres del payload ja analitzats quan el codi és posterior al pas 6
 *   (`UNKNOWN_KID`, `KID_NOT_ACCEPTED`, `BAD_SIGNATURE`, `BAD_FIELDS`); són **dades NO fiables**
 *   (`04` decideix si es mostren). L'app del candidat mai mostra `VÀLIDA` (T5).
 */
data class VerificationResult(
    val code: VerificationCode,
    val credential: Arb1Credential? = null,
    val untrustedMembers: Map<String, PayloadValue>? = null,
) {
    val outcome: VerificationOutcome get() = code.outcome
}

/**
 * Verificador ARB1 (§3.7.8, passos 1–11, ordre fix; el primer error decideix el codi). Ha de
 * retornar el mateix codi que la PWA per al mateix text.
 */
class CredentialVerifier(private val trustedKeys: TrustedKeys) {

    fun verify(rawInput: String): VerificationResult {
        // Pas 1: normalització de l'entrada (§3.7.7).
        val text = normalizeInput(rawInput) ?: return VerificationResult(VerificationCode.BAD_FORMAT)

        // Pas 2: és una URL (prefix, majúscules/minúscules ASCII).
        if (startsWithAsciiIgnoreCase(text, "http://") || startsWithAsciiIgnoreCase(text, "https://")) {
            return VerificationResult(VerificationCode.IS_URL)
        }

        // Pas 3: "ARB" + [1-9][0-9]{0,2} + ".".
        val version = parsePrefixVersion(text) ?: return VerificationResult(VerificationCode.BAD_FORMAT)

        // Pas 4: versió suportada (sense analitzar la resta).
        if (version != Arb1.VERSION) return VerificationResult(VerificationCode.UNKNOWN_VERSION)

        // Pas 5: envoltori.
        if (text.length > Arb1.MAX_TOKEN) return VerificationResult(VerificationCode.BAD_FORMAT)
        val parts = text.split('.')
        if (parts.size != 3) return VerificationResult(VerificationCode.BAD_FORMAT)
        val payloadSeg = parts[1]
        val sigSeg = parts[2]
        if (payloadSeg.length !in Arb1.MIN_PAYLOAD_SEG..Arb1.MAX_PAYLOAD_SEG || sigSeg.length != Arb1.SIGNATURE_SEG) {
            return VerificationResult(VerificationCode.BAD_FORMAT)
        }
        val payload = Base64Url.decodeCanonical(payloadSeg) ?: return VerificationResult(VerificationCode.BAD_FORMAT)
        val sig = Base64Url.decodeCanonical(sigSeg) ?: return VerificationResult(VerificationCode.BAD_FORMAT)
        if (payload.size !in Arb1.MIN_PAYLOAD_BYTES..Arb1.MAX_PAYLOAD_BYTES || sig.size != Arb1.SIGNATURE_BYTES) {
            return VerificationResult(VerificationCode.BAD_FORMAT)
        }

        // Pas 6: payload abans de la firma.
        val json = StrictUtf8.decode(payload) ?: return VerificationResult(VerificationCode.BAD_PAYLOAD)
        val members = RestrictedJson.parseObject(json) ?: return VerificationResult(VerificationCode.BAD_PAYLOAD)
        val v = members["v"] as? PayloadValue.Integer
        if (v == null || v.value != version) return VerificationResult(VerificationCode.BAD_PAYLOAD)
        val kid = (members["kid"] as? PayloadValue.Text)?.value
        if (kid == null || !Fields.KID.matches(kid)) return VerificationResult(VerificationCode.BAD_PAYLOAD)

        // Pas 7 i 8: clau.
        val key = trustedKeys[kid] ?: return VerificationResult(VerificationCode.UNKNOWN_KID, untrustedMembers = members)
        if (!key.status.accepted) return VerificationResult(VerificationCode.KID_NOT_ACCEPTED, untrustedMembers = members)

        // Pas 9: firma ES256 sobre l'ASCII de "ARB1.<payload-seg>" (§3.7.2).
        val signedInput = text.substring(0, text.lastIndexOf('.')).toByteArray(Charsets.US_ASCII)
        if (!verifyEs256(key, signedInput, sig)) {
            return VerificationResult(VerificationCode.BAD_SIGNATURE, untrustedMembers = members)
        }

        // Pas 10: camps després de la firma.
        val credential = Fields.validate(members)
            ?: return VerificationResult(VerificationCode.BAD_FIELDS, untrustedMembers = members)

        // Pas 11.
        return VerificationResult(VerificationCode.OK, credential = credential)
    }

    companion object {
        /** Caràcters eliminats en qualsevol posició (§3.7.7, pas 2). */
        private fun isRemovable(c: Char): Boolean =
            c == '\u0009' || c == '\u000A' || c == '\u000D' || c == '\u0020' || c == '\u00A0' ||
                c == '\u200B' || c == '\uFEFF'

        /** §3.7.7: `null` ⇒ `BAD_FORMAT` (entrada > 2048 unitats UTF-16 o resultat buit). */
        fun normalizeInput(raw: String): String? {
            if (raw.length > Arb1.MAX_RAW_INPUT) return null
            val sb = StringBuilder(raw.length)
            for (c in raw) if (!isRemovable(c)) sb.append(c)
            return if (sb.isEmpty()) null else sb.toString()
        }

        private fun startsWithAsciiIgnoreCase(text: String, prefix: String): Boolean {
            if (text.length < prefix.length) return false
            for (i in prefix.indices) {
                var c = text[i]
                if (c in 'A'..'Z') c += 32
                if (c != prefix[i]) return false
            }
            return true
        }

        /** Pas 3: retorna la versió o `null`. */
        private fun parsePrefixVersion(text: String): Int? {
            if (!text.startsWith("ARB")) return null
            var i = 3
            if (i >= text.length || text[i] !in '1'..'9') return null
            val start = i
            while (i < text.length && text[i] in '0'..'9' && i - start < 3) i++
            if (i >= text.length || text[i] != '.') return null
            return text.substring(start, i).toInt()
        }

        private val ONE = BigInteger.ONE

        internal fun verifyEs256(key: TrustedKey, signedInput: ByteArray, rawSig: ByteArray): Boolean {
            val r = P256.rOf(rawSig)
            val s = P256.sOf(rawSig)
            // Fora de [1, n−1] ⇒ fals, sense dependre del proveïdor (S04).
            if (r < ONE || r >= P256.N || s < ONE || s >= P256.N) return false
            return try {
                val verifier = Signature.getInstance("SHA256withECDSA")
                verifier.initVerify(key.publicKey)
                verifier.update(signedInput)
                verifier.verify(P256.rawToDer(rawSig))
            } catch (e: Exception) {
                false
            }
        }
    }
}
