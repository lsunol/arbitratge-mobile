package dev.lluissunol.arbitratge.credential

import java.security.SecureRandom
import java.security.Signature
import java.security.interfaces.ECPrivateKey

/**
 * Segellador (`03` §6.4): rep **els bytes de §3.7.2** (ASCII de `ARB1.<payload-seg>`) i retorna la
 * firma ES256 ja en `R‖S` de 64 bytes (§3.7.3). Interfície per a testabilitat i rotació de `kid`.
 */
fun interface CredentialSealer {
    /** @throws SealException si no es pot produir una firma conforme. */
    fun seal(signedInput: ByteArray): ByteArray
}

class SealException(message: String, cause: Throwable? = null) : Exception(message, cause)

/**
 * Segellador JCA: `Signature.getInstance("SHA256withECDSA")` (sense pre-hash) i conversió
 * DER → `R‖S`. **No** depèn de `SHA256withECDSAinP1363Format` (§3.7.3).
 *
 * La clau és PKCS#8 DER de P-256. El mecanisme d'injecció a l'APK és de `05` §4.3.1; aquesta
 * classe mai imprimeix ni registra el material.
 *
 * @param random font d'aleatorietat opcional (només per a generar vectors reproduïbles; a
 *   producció, `null` ⇒ la del proveïdor).
 */
class JcaEs256Sealer(pkcs8: ByteArray, private val random: SecureRandom? = null) : CredentialSealer {
    private val key: ECPrivateKey = P256.privateKeyFromPkcs8(pkcs8)
        ?: throw IllegalArgumentException("la clau no és una PKCS#8 P-256 vàlida")

    override fun seal(signedInput: ByteArray): ByteArray {
        val der = try {
            val signer = Signature.getInstance("SHA256withECDSA")
            if (random != null) signer.initSign(key, random) else signer.initSign(key)
            signer.update(signedInput)
            signer.sign()
        } catch (e: Exception) {
            throw SealException("error del proveïdor en signar", e)
        }
        return P256.derToRaw(der) ?: throw SealException("firma DER amb forma no admesa")
    }
}
