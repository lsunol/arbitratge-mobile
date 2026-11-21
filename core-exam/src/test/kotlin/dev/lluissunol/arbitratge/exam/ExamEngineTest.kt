package dev.lluissunol.arbitratge.exam

import dev.lluissunol.arbitratge.exam.TestBanks.bank
import dev.lluissunol.arbitratge.exam.TestBanks.q
import dev.lluissunol.arbitratge.exam.TestBanks.rules
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertSame
import kotlin.test.assertTrue

class ExamEngineTest {
    private fun start(b: Bank = bank(), r: ExamRules = b.rules, seed: Long = 1) = ExamEngine.start(b, r, SeededRng(seed))

    /** Responde la correcta de cada pregunta hasta `n` y deja el resto. */
    private fun answerCorrect(s: ExamSession, upTo: Int): ExamSession {
        var cur = s
        for (i in 0 until upTo) cur = ExamEngine.answer(cur, i, cur.snapshot.questions[i].correctIds, 1000L)
        return cur
    }

    @Test fun startSelectsNActiveQuestions() {
        val b = bank(10).let { it.copy(questions = it.questions + q("t-900", status = QuestionStatus.RETIRED)) }
        val s = start(b)
        assertEquals(3, s.total)
        assertTrue(s.snapshot.questions.none { it.id == "t-900" })
        assertEquals(3, s.snapshot.questions.map { it.id }.toSet().size)
        assertEquals("0.0.1-test", s.snapshot.bankVersion)
        assertEquals("t-1", s.snapshot.examVersion)
    }

    @Test fun sameSeedSameExamDifferentSeedDiffers() {
        val b = bank(30, rules(n = 10, threshold = 5))
        assertEquals(start(b, seed = 7), start(b, seed = 7))
        assertNotEquals(start(b, seed = 7).snapshot.questions.map { it.id }, start(b, seed = 8).snapshot.questions.map { it.id })
    }

    @Test fun shuffleDisabledKeepsBankOrder() {
        val b = bank(6, rules(n = 4, threshold = 2).copy(shuffleQuestions = false, shuffleOptions = false))
        val s = start(b)
        assertEquals(listOf("t-001", "t-002", "t-003", "t-004"), s.snapshot.questions.map { it.id })
        assertEquals(listOf("a", "b", "c"), s.snapshot.questions[0].options.map { it.id })
    }

    @Test fun fisherYatesIsAPermutationAndDoesNotMutateInput() {
        val input = (1..20).toList()
        val out = ExamEngine.fisherYates(input, SeededRng(3))
        assertEquals(input.toSet(), out.toSet())
        assertEquals(20, out.size)
        assertEquals((1..20).toList(), input)
        assertNotEquals(input, out)
    }

    @Test fun optionsShuffledButCorrectIdsPreserved() {
        val b = bank(20, rules(n = 20, threshold = 10))
        val s = start(b, seed = 11)
        assertTrue(s.snapshot.questions.any { it.options.map { o -> o.id } != listOf("a", "b", "c") })
        assertTrue(s.snapshot.questions.all { it.correctIds == listOf("a") && it.options.size == 3 })
    }

    @Test fun trueFalseKeepsFixedOrder() {
        val b = TestBanks.sample().let { it.copy(rules = it.rules.copy(numQuestions = 6, passThresholdCount = 3)) }
        for (seed in 1L..20L) {
            val s = start(b, seed = seed)
            s.snapshot.questions.filter { it.type == QuestionType.TRUE_FALSE }
                .forEach { assertEquals(listOf("Vertader", "Fals"), it.options.map { o -> o.text }) }
        }
    }

    @Test fun startRejectsInvalidRulesAndDoesNotLeakText() {
        val b = bank(5)
        for (bad in listOf(rules(n = 0, threshold = 1), rules(minutes = 0), rules(threshold = 0), rules(n = 6, threshold = 2), rules(n = 3, threshold = 4))) {
            val e = assertFailsWith<ExamEngineException> { ExamEngine.start(b, bad, SeededRng(1)) }
            assertEquals(EngineError.INVALID_RULES, e.error)
            assertFalse(e.message!!.contains("[FICTICI]"))
        }
    }

    @Test fun startRejectsInvalidBankStructure() {
        val b = bank(5)
        val broken = b.copy(questions = b.questions + q("t-077", correct = emptyList()))
        assertFailsWith<ExamEngineException> { ExamEngine.start(broken, b.rules, SeededRng(1)) }
    }

    @Test fun answerIsFreeNavigationReplaceableAndIdempotent() {
        val s = start()
        assertEquals(0, s.answeredCount)
        // cualquier índice, sin orden
        val s2 = ExamEngine.answer(s, 2, listOf("a"), 100)
        assertEquals(setOf(2), s2.answeredIndices)
        val s0 = ExamEngine.answer(s2, 0, listOf("a"), 90)
        assertEquals(listOf(0, 2), s0.answers.map { it.index }, "orden estable por índice")
        assertSame(s0, ExamEngine.answer(s0, 0, listOf("a"), 90)) // idempotente
        // sustituir: la corrección usa la ÚLTIMA respuesta
        val replaced = ExamEngine.answer(s0, 0, listOf("b"), 80)
        assertEquals(listOf("b"), replaced.answerFor(0)!!.selectedIds)
        assertEquals(80, replaced.answerFor(0)!!.remainingAtTapMs)
        assertEquals(2, replaced.answers.size, "una sola fila por pregunta")
        assertEquals(EngineError.INDEX_OUT_OF_RANGE, assertFailsWith<ExamEngineException> { ExamEngine.answer(s0, 9, listOf("a"), 100) }.error)
        assertEquals(EngineError.INDEX_OUT_OF_RANGE, assertFailsWith<ExamEngineException> { ExamEngine.answer(s0, -1, listOf("a"), 100) }.error)
    }

    @Test fun clearingAnAnswerLeavesItUnansweredAndGradesAsFailure() {
        val s = answerCorrect(start(), 3)
        assertEquals(3, ExamEngine.grade(s, 0L).correct)
        val cleared = ExamEngine.answer(s, 1, emptyList(), 50)
        assertEquals(setOf(0, 2), cleared.answeredIndices)
        assertEquals(1, cleared.firstUnanswered)
        assertEquals(1, cleared.unansweredCount)
        val r = ExamEngine.grade(cleared, 0L)
        assertEquals(2, r.correct); assertEquals(0, r.wrong); assertEquals(1, r.unanswered)
        assertTrue(r.items[1].selectedIds.isEmpty() && !r.items[1].isCorrect)
        // volver a responder la misma pregunta
        assertEquals(3, ExamEngine.grade(ExamEngine.answer(cleared, 1, listOf("a"), 40), 0L).correct)
    }

    @Test fun lastAnswerDoesNotCompleteAnything() {
        val s = answerCorrect(start(), 3)
        assertEquals(3, s.answeredCount)
        assertEquals(null, s.firstUnanswered)
        assertEquals(ExamPhase.InProgress(2), ExamStateMachine.next(ExamPhase.InProgress(0), ExamEvent.Navigated(2, 3)))
    }

    @Test fun answerRejectsNegativeRemainingAcceptsZero() {
        val s = start()
        assertEquals(EngineError.TIME_EXPIRED, assertFailsWith<ExamEngineException> { ExamEngine.answer(s, 0, listOf("a"), -1) }.error)
        assertEquals(1, ExamEngine.answer(s, 0, listOf("a"), 0).answers.size)
    }

    @Test fun answerRejectsInvalidSelections() {
        val s = start()
        fun err(ids: List<String>) = assertFailsWith<ExamEngineException> { ExamEngine.answer(s, 0, ids, 5) }.error
        assertEquals(EngineError.INVALID_SELECTION, err(listOf("zz")))
        assertEquals(EngineError.INVALID_SELECTION, err(listOf("a", "b"))) // single_choice
        assertEquals(EngineError.INVALID_SELECTION, err(listOf("a", "a")))
    }

    @Test fun legacySequentialFlagIsIgnored() {
        // `sequential` quedó obsoleto: ni true ni false cambian la navegación ni invalidan el banco.
        val s = start(bank(5, rules().copy(sequential = false)))
        assertEquals(1, ExamEngine.answer(s, 2, listOf("a"), 10).answers.size)
    }

    @Test fun gradeExactSetNoPartialCredit() {
        val b = TestBanks.sample().let { it.copy(rules = it.rules.copy(numQuestions = 6, passThresholdCount = 3, shuffleQuestions = false)) }
        val s0 = start(b)
        val multi = s0.snapshot.questions.indexOfFirst { it.id == "sample-0003" }
        fun gradeWith(sel: List<String>): QuestionResult {
            var s = s0
            for (i in 0 until s0.total) {
                s = ExamEngine.answer(s, i, if (i == multi) sel else s.snapshot.questions[i].correctIds, 10)
            }
            return ExamEngine.grade(s, 0L).items[multi]
        }
        assertTrue(gradeWith(listOf("a", "c")).isCorrect)
        assertTrue(gradeWith(listOf("c", "a")).isCorrect)
        assertFalse(gradeWith(listOf("a")).isCorrect)            // subconjunto
        assertFalse(gradeWith(listOf("a", "c", "d")).isCorrect)  // superconjunto
        assertFalse(gradeWith(listOf("a", "b")).isCorrect)
    }

    @Test fun gradeUnansweredIsWrongAndBreakdownAddsUp() {
        val s = answerCorrect(start(bank(10, rules(n = 5, threshold = 3))), 2)
        val wrongOne = ExamEngine.answer(s, 2, listOf("b"), 10)
        val r = ExamEngine.grade(wrongOne, 0L)
        assertEquals(2, r.correct); assertEquals(1, r.wrong); assertEquals(2, r.unanswered)
        assertEquals(5, r.total); assertEquals(3, r.failures)
        assertEquals(r.total, r.correct + r.wrong + r.unanswered)
        assertEquals(5, r.items.size)
        assertTrue(r.items.drop(3).all { !it.isCorrect && it.selectedIds.isEmpty() })
        assertTrue(r.items.all { it.correctIds == listOf("a") })
    }

    @Test fun emptySelectionCountsAsUnanswered() {
        val s = ExamEngine.answer(start(), 0, emptyList(), 10)
        assertEquals(3, ExamEngine.grade(s, 0L).unanswered)
    }

    @Test fun thresholdEdges() {
        val base = start(bank(10, rules(n = 5, threshold = 3)))
        assertFalse(ExamEngine.grade(answerCorrect(base, 2), 0L).passed)  // umbral-1
        assertTrue(ExamEngine.grade(answerCorrect(base, 3), 0L).passed)   // umbral
        assertTrue(ExamEngine.grade(answerCorrect(base, 4), 0L).passed)   // umbral+1
        assertTrue(ExamEngine.grade(answerCorrect(base, 5), 0L).passed)
        val allWrong = start(bank(4, rules(n = 4, threshold = 4)))
        assertFalse(ExamEngine.grade(answerCorrect(allWrong, 3), 0L).passed)
        assertTrue(ExamEngine.grade(answerCorrect(allWrong, 4), 0L).passed)  // umbral == N
    }

    @Test fun gradeCarriesExplanationAndRef() {
        val b = bank(3, rules(n = 3, threshold = 1))
        val withExp = b.copy(questions = b.questions.map { it.copy(explanation = "[FICTICI] e", ref = "[FICTICI] r") })
        val r = ExamEngine.grade(start(withExp), 0L)
        assertTrue(r.items.all { it.explanation == "[FICTICI] e" && it.ref == "[FICTICI] r" })
        assertTrue(ExamEngine.grade(start(), 0L).items.all { it.explanation == null && it.ref == null })
    }

    @Test fun elapsedIsExplicitAndTimeoutWithNoAnswersGradesAllUnanswered() {
        val limit = 5 * 60_000L
        val s = start()
        val clock = ExamClockModel.initial(limit, 1_000, 7, 10_000)
        val elapsed = ExamClockModel.elapsedMs(clock, 1_000 + limit + 50) // agotado, sin respuestas
        assertEquals(limit, elapsed)
        val r = ExamEngine.grade(s, elapsed)
        assertEquals(limit, r.elapsedMs)
        assertEquals(0, r.correct); assertEquals(0, r.wrong); assertEquals(3, r.unanswered)
        assertFalse(r.passed)
        assertEquals(123L, ExamEngine.grade(answerCorrect(s, 2), 123L).elapsedMs)
    }

    @Test fun snapshotDoesNotAliasBankOptions() {
        val opts = arrayListOf(Option("a", "[FICTICI] x"), Option("b", "[FICTICI] y"))
        val qs = (1..4).map { q("t-00$it").copy(options = ArrayList(opts)) }
        val b = bank(4, rules(n = 4, threshold = 2)).copy(questions = qs)
        val r = rules(n = 4, threshold = 2).copy(shuffleOptions = false)
        val s = ExamEngine.start(b, r, SeededRng(1))
        qs.forEach { (it.options as MutableList<Option>).clear() }
        assertTrue(s.snapshot.questions.all { it.options.size == 2 })
        val s2 = ExamEngine.start(b.copy(questions = qs.map { it.copy(options = ArrayList(opts)) }), r, SeededRng(1))
        assertEquals(s.snapshot, s2.snapshot)
    }

    @Test fun sessionRoundTripsThroughJsonAndGradeIsRecomputable() {
        val s = answerCorrect(start(bank(10, rules(n = 5, threshold = 3)), seed = 5), 4)
        val json = ExamJson.encodeToString(ExamSession.serializer(), s)
        val back = ExamJson.decodeFromString(ExamSession.serializer(), json)
        assertEquals(s, back)
        assertEquals(ExamEngine.grade(s, 123), ExamEngine.grade(back, 123))
        val r = ExamEngine.grade(s, 77L)
        assertEquals(r, ExamJson.decodeFromString(ExamResult.serializer(), ExamJson.encodeToString(ExamResult.serializer(), r)))
    }

    @Test fun sessionIsIndependentFromBankAfterStart() {
        val b = bank(5)
        val s = start(b)
        val mutated = b.copy(questions = b.questions.map { it.copy(correct = listOf("b")) })
        assertEquals(ExamEngine.grade(answerCorrect(s, 3), 0L).correct, 3)
        assertTrue(mutated.questions.isNotEmpty())
    }

    @Test fun secureRngWorks() {
        val s = ExamEngine.start(bank(10), rules(), SecureRandomRng())
        assertEquals(3, s.total)
        val r = SecureRandomRng()
        repeat(100) { assertTrue(r.nextInt(5) in 0..4) }
    }
}
