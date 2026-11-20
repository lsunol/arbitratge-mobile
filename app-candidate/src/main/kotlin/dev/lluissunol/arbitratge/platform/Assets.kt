package dev.lluissunol.arbitratge.platform

import android.content.Context
import dev.lluissunol.arbitratge.exam.Bank
import dev.lluissunol.arbitratge.exam.BankParseException
import dev.lluissunol.arbitratge.exam.BankParser
import dev.lluissunol.arbitratge.exam.BankValidator
import dev.lluissunol.arbitratge.exam.ValidationMode

/** Banco empaquetado por `packBank` en `assets/bank.json` (03 §5.4). Lo no válido = "build roto" (04 §2.10). */
object BankLoader {
    fun load(context: Context): Result<Bank> = runCatching {
        val text = context.assets.open("bank.json").use { it.readBytes().toString(Charsets.UTF_8) }
        parse(text)
    }

    fun parse(text: String): Bank {
        val bank = try {
            BankParser.parse(text)
        } catch (e: BankParseException) {
            throw IllegalStateException(e.message)
        }
        val report = BankValidator.validate(bank, ValidationMode.RUNTIME)
        check(report.isValid) { report.summary() }
        return bank
    }
}

/** Material de sello incorporado por `packSealKey` en `assets/seal/` (clave en claro por ADR 0001; 05 §4.3.1). */
class SealMaterial(val pkcs8: ByteArray, val kid: String) {
    companion object {
        fun load(context: Context): Result<SealMaterial> = runCatching {
            val key = context.assets.open("seal/seal-key.der").use { it.readBytes() }
            val kid = context.assets.open("seal/kid.txt").use { it.readBytes().toString(Charsets.US_ASCII) }.trim()
            SealMaterial(key, kid)
        }
    }
}
