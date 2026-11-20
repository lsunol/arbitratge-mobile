package cat.futbolbotons.arbitratge.exam

import cat.futbolbotons.arbitratge.exam.TestBanks.bank
import cat.futbolbotons.arbitratge.exam.TestBanks.q
import cat.futbolbotons.arbitratge.exam.TestBanks.rules
import cat.futbolbotons.arbitratge.exam.TestBanks.tf
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class BankTest {
    private fun codes(b: Bank, mode: ValidationMode = ValidationMode.RUNTIME) =
        BankValidator.validate(b, mode).issues.map { it.code }

    @Test fun sampleBankLoadsAndValidates() {
        val bank = TestBanks.sample()
        assertTrue(bank.sample)
        assertEquals(7, bank.questions.size)
        assertEquals(6, bank.questions.count { it.status == QuestionStatus.ACTIVE })
        assertEquals(QuestionType.entries.toSet(), bank.questions.map { it.type }.toSet())
        assertTrue(bank.questions.any { it.type == QuestionType.MULTIPLE_CHOICE && it.correct.size == 2 })
        val report = BankValidator.validate(bank)
        assertTrue(report.isValid, report.summary())
    }

    @Test fun sampleBankStringsAreFictitious() {
        val bank = TestBanks.sample()
        val fixed = setOf("Vertader", "Fals")
        val texts = bank.questions.flatMap {
            listOf(it.text) + it.options.map { o -> o.text } + listOfNotNull(it.explanation, it.ref) + it.tags
        }.filter { it !in fixed }
        assertTrue(texts.all { it.startsWith("[FICTICI]") })
    }

    @Test fun sampleBankIsRejectedInRelease() {
        assertTrue(BankValidator.validate(TestBanks.sample(), ValidationMode.RELEASE).has(IssueCode.SAMPLE_IN_RELEASE))
    }

    @Test fun canaryIsIgnoredAndNeverReserialized() {
        val bank = TestBanks.sample()
        val out = kotlinx.serialization.json.Json.encodeToString(Bank.serializer(), bank)
        assertFalse(out.contains("canary"))
    }

    @Test fun parseErrorsDoNotLeakContent() {
        val e = assertFailsWith<BankParseException> {
            BankParser.parse("""{"schema":1,"bank_version":"x","rules":{},"questions":[{"text":"SECRET-TEXT"}]}""")
        }
        assertFalse(e.message!!.contains("SECRET"))
        val e2 = assertFailsWith<BankParseException> { BankParser.parse("""{"schema":2}""") }
        assertTrue(e2.message!!.contains("schema"))
        assertFailsWith<BankParseException> { BankParser.parse("no és json SECRET") }
    }

    @Test fun validBankIsValid() = assertTrue(BankValidator.validate(bank()).isValid)

    @Test fun structureErrors() {
        assertTrue(IssueCode.SCHEMA_UNSUPPORTED in codes(bank().copy(schema = 2)))
        assertTrue(IssueCode.BANK_VERSION_INVALID in codes(bank().copy(bankVersion = "")))
        assertTrue(IssueCode.BANK_VERSION_INVALID in codes(bank().copy(bankVersion = "a b")))
        assertTrue(IssueCode.BANK_VERSION_INVALID in codes(bank().copy(bankVersion = "x".repeat(25))))
        assertTrue(IssueCode.EXAM_VERSION_INVALID in codes(bank(rules = rules().copy(examVersion = ""))))
        assertTrue(IssueCode.EXAM_VERSION_INVALID in codes(bank(rules = rules().copy(examVersion = "1+2"))))
        assertTrue(IssueCode.EXAM_VERSION_INVALID in codes(bank(rules = rules().copy(examVersion = "x".repeat(17)))))
    }

    @Test fun questionIdErrors() {
        val b = bank(5)
        assertTrue(IssueCode.QUESTION_ID_INVALID in codes(b.copy(questions = b.questions + q("Mal Id"))))
        assertTrue(IssueCode.QUESTION_ID_INVALID in codes(b.copy(questions = b.questions + q("x".repeat(33)))))
        val dup = codes(b.copy(questions = b.questions + b.questions.first()))
        assertTrue(IssueCode.QUESTION_ID_DUPLICATE in dup)
    }

    @Test fun reportContainsIdsButNeverText() {
        val bad = q("t-001").copy(text = "SECRET-TEXT", options = listOf(Option("a", "SECRET-OPT")), correct = listOf("zz"))
        val report = BankValidator.validate(bank(5).copy(questions = listOf(bad) + bank(5).questions.drop(1)))
        assertFalse(report.isValid)
        assertFalse(report.summary().contains("SECRET"))
        assertTrue(report.summary().contains("t-001"))
    }

    @Test fun textAndOptionErrors() {
        fun with(question: Question) = codes(bank(5).copy(questions = listOf(question) + bank(5).questions.drop(1)))
        assertTrue(IssueCode.QUESTION_TEXT_EMPTY in with(q("t-001").copy(text = "  ")))
        assertTrue(IssueCode.OPTION_TEXT_EMPTY in with(q("t-001").copy(options = listOf(Option("a", "x"), Option("b", "")))))
        assertTrue(IssueCode.OPTIONS_TOO_FEW in with(q("t-001", optionIds = listOf("a"))))
        assertTrue(IssueCode.OPTION_ID_DUPLICATE in with(q("t-001", optionIds = listOf("a", "a", "b"))))
        assertTrue(IssueCode.OPTION_ID_EMPTY in with(q("t-001", optionIds = listOf("a", ""))))
    }

    @Test fun correctAnswerErrors() {
        fun with(question: Question) = codes(bank(5).copy(questions = listOf(question) + bank(5).questions.drop(1)))
        assertTrue(IssueCode.CORRECT_COUNT_INVALID in with(q("t-001", correct = listOf("a", "b"))))
        assertTrue(IssueCode.ACTIVE_WITHOUT_CORRECT in with(q("t-001", correct = emptyList())))
        assertTrue(IssueCode.CORRECT_COUNT_INVALID in with(q("t-001", QuestionType.MULTIPLE_CHOICE, correct = listOf("a"))))
        assertTrue(IssueCode.CORRECT_DUPLICATE in with(q("t-001", QuestionType.MULTIPLE_CHOICE, correct = listOf("a", "a"))))
        assertTrue(IssueCode.CORRECT_NOT_IN_OPTIONS in with(q("t-001", correct = listOf("zz"))))
        assertTrue(with(q("t-001", QuestionType.MULTIPLE_CHOICE, correct = listOf("a", "b"))).isEmpty())
    }

    @Test fun retiredQuestionsAreHeldToTypeCardinality() {
        fun with(question: Question) = codes(bank(5).copy(questions = listOf(question) + bank(5).questions.drop(1)))
        val r = QuestionStatus.RETIRED
        assertTrue(with(q("t-001", status = r)).isEmpty())
        assertTrue(IssueCode.CORRECT_COUNT_INVALID in with(q("t-001", status = r, correct = emptyList())))
        assertTrue(IssueCode.CORRECT_COUNT_INVALID in with(q("t-001", status = r, correct = listOf("a", "b"))))
        assertTrue(IssueCode.CORRECT_COUNT_INVALID in with(q("t-001", QuestionType.MULTIPLE_CHOICE, r, correct = listOf("a"))))
        assertTrue(with(q("t-001", QuestionType.MULTIPLE_CHOICE, r, correct = listOf("a", "b"))).isEmpty())
        assertTrue(IssueCode.ACTIVE_WITHOUT_CORRECT in with(q("t-001", correct = emptyList())))
    }

    @Test fun nonSequentialRulesAreRejected() {
        assertTrue(IssueCode.SEQUENTIAL_REQUIRED in codes(bank(5, rules().copy(sequential = false))))
    }

    @Test fun trueFalseErrors() {
        fun with(question: Question) = codes(bank(5).copy(questions = listOf(question) + bank(5).questions.drop(1)))
        assertTrue(with(tf("t-001")).isEmpty())
        val threeOpts = tf("t-001").copy(options = listOf(Option("t", "Vertader"), Option("f", "Fals"), Option("x", "Potser")))
        assertTrue(IssueCode.TRUE_FALSE_OPTIONS_INVALID in with(threeOpts))
        val renamed = tf("t-001").copy(options = listOf(Option("t", "Sí"), Option("f", "No")))
        assertTrue(IssueCode.TRUE_FALSE_OPTIONS_INVALID in with(renamed))
        assertTrue(IssueCode.CORRECT_COUNT_INVALID in with(tf("t-001").copy(correct = listOf("t", "f"))))
    }

    @Test fun ruleErrors() {
        fun r(rl: ExamRules, count: Int = 10) = codes(bank(count, rl))
        assertTrue(IssueCode.NUM_QUESTIONS_INVALID in r(rules(n = 0, threshold = 1)))
        assertTrue(IssueCode.NUM_QUESTIONS_INVALID in r(rules(n = -1, threshold = 1)))
        assertTrue(IssueCode.TIME_LIMIT_INVALID in r(rules(minutes = 0)))
        assertTrue(IssueCode.TIME_LIMIT_INVALID in r(rules(minutes = -3)))
        assertTrue(IssueCode.THRESHOLD_INVALID in r(rules(threshold = 0)))
        assertTrue(IssueCode.THRESHOLD_INVALID in r(rules(n = 3, threshold = 4)))
        assertTrue(IssueCode.NUM_QUESTIONS_EXCEEDS_ACTIVE in r(rules(n = 11, threshold = 2)))
        assertTrue(IssueCode.NUM_QUESTIONS_ABOVE_SANITY in r(rules(n = 201, threshold = 2), count = 250))
        assertTrue(IssueCode.TIME_LIMIT_ABOVE_SANITY in r(rules(minutes = 241)))
        assertTrue(r(rules(n = 10, threshold = 10)).isEmpty()) // N == activas y umbral == N son válidos
    }

    @Test fun retiredQuestionsDoNotCountAsActive() {
        val b = bank(3, rules(n = 3, threshold = 2))
        val withRetired = b.copy(questions = b.questions.dropLast(1) + q("t-009", status = QuestionStatus.RETIRED))
        assertTrue(IssueCode.NUM_QUESTIONS_EXCEEDS_ACTIVE in codes(withRetired))
    }
}
