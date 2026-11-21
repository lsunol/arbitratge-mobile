package dev.lluissunol.arbitratge.exam

enum class ValidationMode {
    /** Carga en ejecución / tests: se admite un banco de muestra. */
    RUNTIME,

    /** Build de release: un banco `sample=true` se rechaza (03 §5.2 punto 5). */
    RELEASE,
}

enum class IssueCode {
    SCHEMA_UNSUPPORTED,
    BANK_VERSION_INVALID,
    EXAM_VERSION_INVALID,
    SAMPLE_IN_RELEASE,
    QUESTION_ID_INVALID,
    QUESTION_ID_DUPLICATE,
    QUESTION_TEXT_EMPTY,
    OPTION_ID_EMPTY,
    OPTION_TEXT_EMPTY,
    OPTION_ID_DUPLICATE,
    OPTIONS_TOO_FEW,
    TRUE_FALSE_OPTIONS_INVALID,
    CORRECT_COUNT_INVALID,
    CORRECT_DUPLICATE,
    CORRECT_NOT_IN_OPTIONS,
    ACTIVE_WITHOUT_CORRECT,
    NUM_QUESTIONS_INVALID,
    NUM_QUESTIONS_ABOVE_SANITY,
    NUM_QUESTIONS_EXCEEDS_ACTIVE,
    TIME_LIMIT_INVALID,
    TIME_LIMIT_ABOVE_SANITY,
    THRESHOLD_INVALID,
}

/**
 * [ref]: id de pregunta si cumple el patrón, o `#n` (posición 1-based) si no.
 * [values]: solo recuentos. Nunca texto del banco.
 */
data class ValidationIssue(val code: IssueCode, val ref: String? = null, val values: List<Int> = emptyList()) {
    override fun toString(): String = buildString {
        append(code.name)
        if (ref != null) append(" [").append(ref).append(']')
        if (values.isNotEmpty()) append(" ").append(values.joinToString(prefix = "(", postfix = ")"))
    }
}

data class ValidationReport(val issues: List<ValidationIssue>) {
    val isValid: Boolean get() = issues.isEmpty()
    fun has(code: IssueCode): Boolean = issues.any { it.code == code }
    fun summary(): String =
        if (isValid) "Banc vàlid" else "${issues.size} error(s): " + issues.joinToString("; ")
}

object BankValidator {
    const val MAX_NUM_QUESTIONS = 200
    const val MAX_TIME_LIMIT_MINUTES = 240
    val QUESTION_ID = Regex("^[a-z0-9-]{1,32}$")
    val EXAM_VERSION = Regex("^[0-9A-Za-z._-]{1,16}$") // exv, 02 §3.7.5
    val BANK_VERSION = Regex("^[0-9A-Za-z._+-]{1,24}$") // bv, 02 §3.7.5
    const val TRUE_TEXT = "Vertader"
    const val FALSE_TEXT = "Fals"

    fun validate(bank: Bank, mode: ValidationMode = ValidationMode.RUNTIME): ValidationReport {
        val out = mutableListOf<ValidationIssue>()
        if (bank.schema != BankParser.SUPPORTED_SCHEMA) out += ValidationIssue(IssueCode.SCHEMA_UNSUPPORTED)
        if (!BANK_VERSION.matches(bank.bankVersion)) out += ValidationIssue(IssueCode.BANK_VERSION_INVALID)
        if (mode == ValidationMode.RELEASE && bank.sample) out += ValidationIssue(IssueCode.SAMPLE_IN_RELEASE)

        val seen = HashSet<String>()
        bank.questions.forEachIndexed { idx, q ->
            val idOk = QUESTION_ID.matches(q.id)
            val ref = if (idOk) q.id else "#${idx + 1}"
            if (!idOk) out += ValidationIssue(IssueCode.QUESTION_ID_INVALID, ref)
            else if (!seen.add(q.id)) out += ValidationIssue(IssueCode.QUESTION_ID_DUPLICATE, ref)
            validateQuestion(q, ref, out)
        }

        val active = bank.questions.count { it.status == QuestionStatus.ACTIVE }
        validateRules(bank.rules, active, out)
        return ValidationReport(out)
    }

    /** Valida solo reglas frente al nº de preguntas activas (lo usa también `ExamEngine.start`). */
    fun validateRules(rules: ExamRules, activeCount: Int): ValidationReport =
        ValidationReport(mutableListOf<ValidationIssue>().also { validateRules(rules, activeCount, it) })

    private fun validateRules(r: ExamRules, active: Int, out: MutableList<ValidationIssue>) {
        // `sequential` quedó obsoleto (navegación libre, 2026-11-20): se acepta cualquier valor y se ignora.
        if (!EXAM_VERSION.matches(r.examVersion)) out += ValidationIssue(IssueCode.EXAM_VERSION_INVALID)
        if (r.numQuestions <= 0) out += ValidationIssue(IssueCode.NUM_QUESTIONS_INVALID, values = listOf(r.numQuestions))
        else {
            if (r.numQuestions > MAX_NUM_QUESTIONS)
                out += ValidationIssue(IssueCode.NUM_QUESTIONS_ABOVE_SANITY, values = listOf(r.numQuestions, MAX_NUM_QUESTIONS))
            if (r.numQuestions > active)
                out += ValidationIssue(IssueCode.NUM_QUESTIONS_EXCEEDS_ACTIVE, values = listOf(r.numQuestions, active))
            if (r.passThresholdCount < 1 || r.passThresholdCount > r.numQuestions)
                out += ValidationIssue(IssueCode.THRESHOLD_INVALID, values = listOf(r.passThresholdCount, r.numQuestions))
        }
        if (r.numQuestions <= 0 && r.passThresholdCount < 1)
            out += ValidationIssue(IssueCode.THRESHOLD_INVALID, values = listOf(r.passThresholdCount, r.numQuestions))
        if (r.timeLimitMinutes <= 0) out += ValidationIssue(IssueCode.TIME_LIMIT_INVALID, values = listOf(r.timeLimitMinutes))
        else if (r.timeLimitMinutes > MAX_TIME_LIMIT_MINUTES)
            out += ValidationIssue(IssueCode.TIME_LIMIT_ABOVE_SANITY, values = listOf(r.timeLimitMinutes, MAX_TIME_LIMIT_MINUTES))
    }

    private fun validateQuestion(q: Question, ref: String, out: MutableList<ValidationIssue>) {
        if (q.text.isBlank()) out += ValidationIssue(IssueCode.QUESTION_TEXT_EMPTY, ref)
        if (q.options.size < 2) out += ValidationIssue(IssueCode.OPTIONS_TOO_FEW, ref, listOf(q.options.size))
        if (q.options.any { it.id.isBlank() }) out += ValidationIssue(IssueCode.OPTION_ID_EMPTY, ref)
        if (q.options.any { it.text.isBlank() }) out += ValidationIssue(IssueCode.OPTION_TEXT_EMPTY, ref)
        val ids = q.options.map { it.id }
        if (ids.toSet().size != ids.size) out += ValidationIssue(IssueCode.OPTION_ID_DUPLICATE, ref)

        if (q.type == QuestionType.TRUE_FALSE &&
            q.options.map { it.text } != listOf(TRUE_TEXT, FALSE_TEXT)
        ) out += ValidationIssue(IssueCode.TRUE_FALSE_OPTIONS_INVALID, ref, listOf(q.options.size))

        if (q.correct.toSet().size != q.correct.size) out += ValidationIssue(IssueCode.CORRECT_DUPLICATE, ref)
        if (!ids.toSet().containsAll(q.correct)) out += ValidationIssue(IssueCode.CORRECT_NOT_IN_OPTIONS, ref)

        // Cardinalidad por tipo (§5.3) para activas y retiradas; "activa sin correct" solo para activas.
        val n = q.correct.toSet().size
        if (q.correct.isEmpty() && q.status == QuestionStatus.ACTIVE) {
            out += ValidationIssue(IssueCode.ACTIVE_WITHOUT_CORRECT, ref)
        } else {
            val ok = if (q.type == QuestionType.MULTIPLE_CHOICE) n >= 2 else n == 1
            if (!ok) out += ValidationIssue(IssueCode.CORRECT_COUNT_INVALID, ref, listOf(n))
        }
    }
}

