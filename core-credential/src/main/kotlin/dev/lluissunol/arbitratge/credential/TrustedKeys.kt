package dev.lluissunol.arbitratge.credential

import java.security.MessageDigest
import java.security.interfaces.ECPublicKey

/** Estat d'una entrada de confiança (§3.7.9). */
enum class KeyStatus(val wire: String, val accepted: Boolean) {
    ACTIVE("active", true),
    RETIRED("retired", true),
    COMPROMISED("compromised", false),
    ;

    companion object {
        fun fromWire(s: String): KeyStatus? = entries.firstOrNull { it.wire == s }
    }
}

/** Entrada `trusted-keys/<kid>.json` validada (§3.7.9). */
class TrustedKey internal constructor(
    val kid: String,
    val spki: ByteArray,
    val sha256Spki: String,
    val status: KeyStatus,
    val validFrom: String?,
    internal val publicKey: ECPublicKey,
) {
    val alg: String get() = "ES256"
}

/** Motiu pel qual una entrada s'ha descartat en carregar la llista. */
data class DiscardedKey(val kid: String?, val reason: String)

/**
 * Llista de confiança `kid → clau pública` (§3.7.9). Només conté entrades coherents; les
 * descartades es reporten a [discarded] (la porta G1 de `05` impedeix publicar-ne).
 */
class TrustedKeys private constructor(
    private val byKid: Map<String, TrustedKey>,
    val discarded: List<DiscardedKey>,
) {
    val kids: Set<String> get() = byKid.keys

    operator fun get(kid: String): TrustedKey? = byKid[kid]

    companion object {
        private val KID = Regex("[a-z0-9]{1,16}")
        private val SHA256_HEX = Regex("[0-9a-f]{64}")
        private val DATE = Regex("[0-9]{4}-[0-9]{2}-[0-9]{2}")

        val EMPTY: TrustedKeys = TrustedKeys(emptyMap(), emptyList())

        /**
         * Una entrada per fitxer (`trusted-keys/<kid>.json`). Si [fileKid] no és `null`, el `kid`
         * ha de coincidir amb el nom del fitxer.
         */
        fun parseEntry(json: String, fileKid: String? = null): Result<TrustedKey> =
            runCatching {
                val obj = MiniJson.parse(json) as? Map<*, *> ?: error("l'entrada no és un objecte")
                entryFrom(obj, fileKid)
            }

        /** Combina entrades ja validades. Un `kid` repetit es descarta del tot (no es tria cap). */
        fun of(entries: List<TrustedKey>, discarded: List<DiscardedKey> = emptyList()): TrustedKeys {
            val counts = entries.groupingBy { it.kid }.eachCount()
            val dup = counts.filterValues { it > 1 }.keys
            val ok = entries.filter { it.kid !in dup }.associateBy { it.kid }
            val dupEntries = entries.filter { it.kid in dup }.map { DiscardedKey(it.kid, "kid duplicat") }
            return TrustedKeys(ok, discarded + dupEntries)
        }

        /**
         * Llista en un sol fitxer JSON: un array d'entrades amb l'esquema de §3.7.9 (format de
         * `test-vectors/trusted-keys-test.json`). Les entrades incoherents es descarten; si el
         * fitxer no és un array JSON vàlid, falla.
         */
        fun parseList(json: String): TrustedKeys {
            val arr = MiniJson.parse(json) as? List<*> ?: throw IllegalArgumentException("la llista no és un array")
            val entries = ArrayList<TrustedKey>()
            val discarded = ArrayList<DiscardedKey>()
            for (item in arr) {
                val obj = item as? Map<*, *>
                val kid = obj?.get("kid") as? String
                if (obj == null) {
                    discarded += DiscardedKey(null, "no és un objecte")
                    continue
                }
                runCatching { entryFrom(obj, null) }
                    .onSuccess { entries += it }
                    .onFailure { discarded += DiscardedKey(kid, it.message ?: "invàlida") }
            }
            return of(entries, discarded)
        }

        private fun entryFrom(obj: Map<*, *>, fileKid: String?): TrustedKey {
            val kid = obj["kid"] as? String ?: error("kid absent")
            require(KID.matches(kid)) { "kid fora de patró" }
            if (fileKid != null) require(kid == fileKid) { "kid no coincideix amb el fitxer" }
            require(obj["alg"] == "ES256") { "alg no és ES256" }
            val spkiText = obj["spki"] as? String ?: error("spki absent")
            val spki = Base64Url.decodeCanonical(spkiText) ?: error("spki no és base64url canònic")
            val sha = obj["sha256_spki"] as? String ?: error("sha256_spki absent")
            require(SHA256_HEX.matches(sha)) { "sha256_spki fora de patró" }
            val digest = P256.toHex(MessageDigest.getInstance("SHA-256").digest(spki))
            require(digest == sha) { "sha256_spki no coincideix" }
            val statusText = obj["status"] as? String ?: error("status absent")
            val status = KeyStatus.fromWire(statusText) ?: error("status desconegut")
            val validFrom = obj["valid_from"]
            require(validFrom == null || (validFrom is String && DATE.matches(validFrom))) { "valid_from invàlid" }
            val key = P256.publicKeyFromSpki(spki) ?: error("spki no és una clau P-256 sense comprimir")
            return TrustedKey(kid, spki, sha, status, validFrom as String?, key)
        }

        /** Crea una entrada a partir d'un SPKI (per a eines i proves). */
        fun entryFromSpki(kid: String, spki: ByteArray, status: KeyStatus, validFrom: String? = null): TrustedKey {
            require(KID.matches(kid)) { "kid fora de patró" }
            val key = requireNotNull(P256.publicKeyFromSpki(spki)) { "spki no és P-256" }
            val sha = P256.toHex(MessageDigest.getInstance("SHA-256").digest(spki))
            return TrustedKey(kid, spki.copyOf(), sha, status, validFrom, key)
        }
    }
}
