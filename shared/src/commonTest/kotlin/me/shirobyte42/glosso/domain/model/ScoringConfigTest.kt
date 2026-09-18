package me.shirobyte42.glosso.domain.model

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ScoringConfigTest {

    @Test
    fun `mastery band starts at the mastery threshold`() {
        assertTrue(ScoringConfig.isMasteryBand(ScoringConfig.MASTERY_THRESHOLD))
        assertTrue(ScoringConfig.isMasteryBand(100))
        assertFalse(ScoringConfig.isMasteryBand(ScoringConfig.MASTERY_THRESHOLD - 1))
    }

    @Test
    fun `close band starts at the close threshold`() {
        assertTrue(ScoringConfig.isCloseBand(ScoringConfig.CLOSE_THRESHOLD))
        assertFalse(ScoringConfig.isCloseBand(ScoringConfig.CLOSE_THRESHOLD - 1))
        assertFalse(ScoringConfig.isCloseBand(0))
    }

    @Test
    fun `bands are ordered so mastery is always also close`() {
        assertTrue(ScoringConfig.CLOSE_THRESHOLD < ScoringConfig.MASTERY_THRESHOLD)
        for (score in 0..100) {
            if (ScoringConfig.isMasteryBand(score)) {
                assertTrue(ScoringConfig.isCloseBand(score), "mastery at $score should also be close")
            }
        }
    }

    @Test
    fun `a strong attempt with every word intact qualifies`() {
        assertTrue(
            ScoringConfig.qualifiesForMastery(
                score = 100,
                completeness = 100,
                wordScores = listOf(100, 95, 88)
            )
        )
    }

    @Test
    fun `low completeness does not qualify`() {
        assertFalse(
            ScoringConfig.qualifiesForMastery(
                score = 95,
                completeness = ScoringConfig.COMPLETENESS_FLOOR - 1,
                wordScores = listOf(100, 100)
            )
        )
    }

    @Test
    fun `one destroyed word does not qualify however good the average`() {
        assertFalse(
            ScoringConfig.qualifiesForMastery(
                score = 100,
                completeness = 100,
                wordScores = listOf(100, 100, ScoringConfig.WORD_FLOOR - 1)
            )
        )
    }

    @Test
    fun `weakest word index points at the lowest score`() {
        assertEquals(2, ScoringConfig.weakestWordIndex(listOf(90, 80, 10)))
        assertEquals(null, ScoringConfig.weakestWordIndex(emptyList()))
    }

    @Test
    fun `levelFor maps score bands and perfection`() {
        assertEquals(MasteryLevel.PERFECT, ScoringConfig.levelFor(100, perfect = true))
        // Flawless wins even when the score is merely mastery-high.
        assertEquals(MasteryLevel.PERFECT, ScoringConfig.levelFor(92, perfect = true))

        assertEquals(MasteryLevel.MASTERED, ScoringConfig.levelFor(ScoringConfig.MASTERY_THRESHOLD))
        assertEquals(MasteryLevel.ALMOST, ScoringConfig.levelFor(ScoringConfig.MASTERY_THRESHOLD - 1))
        assertEquals(MasteryLevel.ALMOST, ScoringConfig.levelFor(ScoringConfig.CLOSE_THRESHOLD))
        assertEquals(MasteryLevel.ROUGH, ScoringConfig.levelFor(ScoringConfig.CLOSE_THRESHOLD - 1))
        assertEquals(MasteryLevel.ROUGH, ScoringConfig.levelFor(ScoringConfig.WORD_FLOOR))
        assertEquals(MasteryLevel.NOT_YET, ScoringConfig.levelFor(ScoringConfig.WORD_FLOOR - 1))
        assertEquals(MasteryLevel.NOT_YET, ScoringConfig.levelFor(0))
    }

    @Test
    fun `sentenceLevel respects the mastery gate`() {
        // A band-high score that failed the gate (skipped words or a destroyed
        // word) must not be reported as MASTERED.
        assertEquals(
            MasteryLevel.ALMOST,
            ScoringConfig.sentenceLevel(92, isMastery = false)
        )
        assertEquals(
            MasteryLevel.MASTERED,
            ScoringConfig.sentenceLevel(92, isMastery = true)
        )
        assertEquals(
            MasteryLevel.PERFECT,
            ScoringConfig.sentenceLevel(100, isMastery = true, perfect = true)
        )
        assertEquals(
            MasteryLevel.NOT_YET,
            ScoringConfig.sentenceLevel(10, isMastery = false)
        )
    }

    @Test
    fun `levels are ordered best to worst`() {
        assertTrue(MasteryLevel.PERFECT < MasteryLevel.MASTERED)
        assertTrue(MasteryLevel.MASTERED < MasteryLevel.ALMOST)
        assertTrue(MasteryLevel.ALMOST < MasteryLevel.ROUGH)
        assertTrue(MasteryLevel.ROUGH < MasteryLevel.NOT_YET)
    }
}
