package dev.lluissunol.arbitratge.exam

import java.io.File

object TestBanks {
    fun sampleJson(): String {
        val path = checkNotNull(System.getProperty("bank.sample.path")) { "bank.sample.path no definida (Gradle test)" }
        return File(path).readText(Charsets.UTF_8)
    }

    fun sample(): Bank = BankParser.parse(sampleJson())

    fun q(
        id: String,
        type: QuestionType = QuestionType.SINGLE_CHOICE,
        status: QuestionStatus = QuestionStatus.ACTIVE,
        optionIds: List<String> = listOf("a", "b", "c"),
        correct: List<String> = listOf("a"),
    ) = Question(
        id = id, type = type, status = status, text = "[FICTICI] $id",
        options = optionIds.map { Option(it, "[FICTICI] opt $it") },
        correct = correct,
    )

    fun tf(id: String, correct: String = "t") = Question(
        id, QuestionType.TRUE_FALSE, QuestionStatus.ACTIVE, "[FICTICI] $id",
        listOf(Option("t", "Vertader"), Option("f", "Fals")), listOf(correct),
    )

    fun rules(n: Int = 3, minutes: Int = 5, threshold: Int = 2) = ExamRules(
        examVersion = "t-1", numQuestions = n, timeLimitMinutes = minutes, passThresholdCount = threshold,
    )

    /** Banco ficticio válido con `count` preguntas activas single_choice. */
    fun bank(count: Int = 10, rules: ExamRules = rules()) = Bank(
        schema = 1, bankVersion = "0.0.1-test", sample = true, rules = rules,
        questions = (1..count).map { q("t-%03d".format(it)) },
    )
}
