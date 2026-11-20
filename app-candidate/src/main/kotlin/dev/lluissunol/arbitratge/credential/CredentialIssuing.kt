package dev.lluissunol.arbitratge.credential

import dev.lluissunol.arbitratge.platform.SealMaterial

/**
 * Puerto de emisión (03 §4.3, §6.1). El `cid` lo reserva la sesión: el emisor lo recibe y no genera otro,
 * lo que hace idempotente el reintento (misma sesión ⇒ mismo `cid` y mismo nombre).
 */
fun interface CredentialIssuing {
    fun issue(cid: String, request: IssueRequest): IssueResult
}

/** Emisión real con `core-credential`: sello JCA + autoverificación con SOLO la entrada del propio `kid`. */
class ArbCredentialIssuing(private val material: SealMaterial) : CredentialIssuing {
    private val sealer = JcaEs256Sealer(material.pkcs8)
    private val trusted: TrustedKeys by lazy {
        val spki = requireNotNull(P256.spkiFromPkcs8(material.pkcs8)) { "clau de sello invàlida" }
        TrustedKeys.of(listOf(TrustedKeys.entryFromSpki(material.kid, spki, KeyStatus.ACTIVE)))
    }

    override fun issue(cid: String, request: IssueRequest): IssueResult =
        CredentialIssuer(sealer, material.kid, trusted) { cid }.issue(request)
}
