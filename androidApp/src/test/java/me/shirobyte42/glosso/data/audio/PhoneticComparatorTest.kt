package me.shirobyte42.glosso.data.audio

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import me.shirobyte42.glosso.domain.model.MasteryLevel

class PhoneticComparatorTest {

    // region normalize

    @Test
    fun `normalize lowercases and keeps simple phones`() {
        assertEquals("kæt", PhoneticComparator.normalize("KÆT"))
    }

    @Test
    fun `normalize strips stress but keeps vowel length`() {
        assertEquals("kætiː", PhoneticComparator.normalize("ˈkætˌiː"))
    }

    @Test
    fun `normalize folds near-identical allophones`() {
        assertEquals("əbət", PhoneticComparator.normalize("ɐbət"))
        assertEquals("ər", PhoneticComparator.normalize("ɚ"))
        assertEquals("ɪ", PhoneticComparator.normalize("ᵻ"))
    }

    @Test
    fun `normalize maps ascii g to ipa g`() {
        assertEquals("ɡ", PhoneticComparator.normalize("g"))
    }

    @Test
    fun `normalize keeps affricates and nasals as single phones`() {
        assertEquals(listOf("tʃ"), phoneList("tʃ"))
        assertEquals(listOf("t͡ʃ"), phoneList("t͡ʃ"))
        assertEquals(listOf("dʒ"), phoneList("dʒ"))
        assertEquals(listOf("ɑ̃"), phoneList("ɑ̃"))
    }

    @Test
    fun `normalize splits on word boundaries`() {
        assertEquals(listOf("k", "æ", "t", "d", "ɒ", "ɡ"), phoneList("kæt dɒg"))
    }

    private fun phoneList(ipa: String): List<String> {
        val result = PhoneticComparator.calculateScoringResult("x", ipa, ipa)
        return result.alignment.map { it.expected }
    }

    // endregion

    // region calculateScoringResult

    @Test
    fun `identical phoneme strings score 100 with all perfect`() {
        val result = PhoneticComparator.calculateScoringResult("cat", "kæt", "kæt")
        assertEquals(100, result.score)
        assertEquals(3, result.alignment.size)
        assertTrue(result.alignment.all { it.status == MatchStatus.PERFECT })
    }

    @Test
    fun `completely different phones score 0`() {
        val result = PhoneticComparator.calculateScoringResult("cat", "kæt", "ʃuːz")
        assertEquals(0, result.score)
        assertTrue(result.alignment.all { it.status == MatchStatus.MISSED })
    }

    @Test
    fun `empty expected and empty actual score 100`() {
        val result = PhoneticComparator.calculateScoringResult("x", "", "")
        assertEquals(100, result.score)
        assertEquals("", result.normalizedExpected)
    }

    @Test
    fun `empty expected with non-empty actual scores 0`() {
        val result = PhoneticComparator.calculateScoringResult("x", "", "k")
        assertEquals(0, result.score)
    }

    @Test
    fun `known near-identical pair counts as perfect`() {
        // r ↔ ɹ has similarity 0.98 in the EN matrix (>= 0.85 threshold)
        val result = PhoneticComparator.calculateScoringResult("right", "raɪt", "ɹaɪt")
        assertEquals(100, result.score)
        assertEquals(MatchStatus.PERFECT, result.alignment[0].status)
    }

    @Test
    fun `known similar pair earns partial credit`() {
        // l ↔ r has similarity 0.8 in the EN matrix -> CLOSE with 0.2 cost; a, ɪ, t match
        val result = PhoneticComparator.calculateScoringResult("light", "laɪt", "raɪt")
        assertEquals(MatchStatus.CLOSE, result.alignment[0].status)
        assertEquals(95, result.score)
    }

    @Test
    fun `unmapped pair earns no credit`() {
        // θ ↔ s has no EN matrix entry; ɪ, ŋ, k match -> 3/4 = 75
        val result = PhoneticComparator.calculateScoringResult("think", "θɪŋk", "sɪŋk")
        assertEquals(MatchStatus.MISSED, result.alignment[0].status)
        assertEquals(75, result.score)
    }

    @Test
    fun `mixed alignment computes weighted score`() {
        // l↔r costs 0.2, ɪ and t match -> 1 - 0.2/3 = 93
        val result = PhoneticComparator.calculateScoringResult("lit", "lɪt", "rɪt")
        assertEquals(93, result.score)
        assertEquals(MatchStatus.CLOSE, result.alignment[0].status)
        assertEquals(MatchStatus.PERFECT, result.alignment[1].status)
    }

    @Test
    fun `language matrices change similarity`() {
        // ʃ ↔ ʒ is 0.8 in FR matrix -> CLOSE there
        val frResult = PhoneticComparator.calculateScoringResult("jour", "ʒuʁ", "ʃuʁ", "fr")
        assertEquals(MatchStatus.CLOSE, frResult.alignment[0].status)
        // but has no EN entry -> MISSED with default matrix
        val enResult = PhoneticComparator.calculateScoringResult("x", "ʒ", "ʃ")
        assertEquals(MatchStatus.MISSED, enResult.alignment[0].status)
    }

    @Test
    fun `unknown language falls back to english matrix`() {
        // e ↔ ɛ is 0.9 in EN matrix -> PERFECT
        val result = PhoneticComparator.calculateScoringResult("x", "e", "ɛ", "xx")
        assertEquals(MatchStatus.PERFECT, result.alignment[0].status)
        assertEquals(100, result.score)
    }

    @Test
    fun `normalized output strips stress but keeps vowel length`() {
        val result = PhoneticComparator.calculateScoringResult("sheep", "ˈʃiːp", "ʃiːp")
        assertEquals("ʃiːp", result.normalizedExpected)
        assertEquals("ʃiːp", result.normalizedActual)
    }

    // region sequence sensitivity (regression: scoring used to be order-insensitive)

    @Test
    fun `word order matters`() {
        // Regression: the old bag-of-phonemes search scored reversed phones 100.
        val result = PhoneticComparator.calculateScoringResult("cat", "kæt", "tæk")
        assertTrue("reordering should be penalised, got ${result.score}", result.score < 50)
    }

    @Test
    fun `repeating the utterance does not inflate the score`() {
        // Regression: extra produced phonemes used to be completely free.
        val result = PhoneticComparator.calculateScoringResult("cat", "kæt", "kætkæt")
        assertTrue("duplication should be penalised, got ${result.score}", result.score < 85)
    }

    @Test
    fun `dropping a phoneme is penalised`() {
        val result = PhoneticComparator.calculateScoringResult("cat", "kæt", "kæ")
        assertTrue("omission should be penalised, got ${result.score}", result.score < 70)
    }

    @Test
    fun `extra phonemes cost something but less than a drop`() {
        val clean = PhoneticComparator.calculateScoringResult("cat", "kæt", "kæt")
        val extra = PhoneticComparator.calculateScoringResult("cat", "kæt", "kætə")
        val dropped = PhoneticComparator.calculateScoringResult("cat", "kæt", "kæ")
        assertEquals(100, clean.score)
        assertTrue("extra phone should cost something", extra.score < clean.score)
        assertTrue("an extra phone should be cheaper than a dropped one", extra.score > dropped.score)
    }

    @Test
    fun `vowel length is meaningful`() {
        // German Stadt /ʃtat/ vs Staat /ʃtaːt/ differ only in vowel length.
        val wrongLength = PhoneticComparator.calculateScoringResult("Staat", "ʃtaːt", "ʃtat", "de")
        assertEquals(MatchStatus.CLOSE, wrongLength.alignment[2].status)
        assertTrue("short vowel for a long one should not be perfect", wrongLength.score < 100)

        val rightLength = PhoneticComparator.calculateScoringResult("Staat", "ʃtaːt", "ʃtaːt", "de")
        assertEquals(100, rightLength.score)
    }

    // endregion

    @Test
    fun `alignment length matches expected phone count`() {
        val result = PhoneticComparator.calculateScoringResult("cat", "kæt", "ʃuːz")
        assertEquals(3, result.alignment.size)
        assertEquals(listOf("k", "æ", "t"), result.alignment.map { it.expected })
    }

    // endregion

    // region word scores, axes and mastery gates

    @Test
    fun `word scores are reported per reference word`() {
        val result = PhoneticComparator.calculateScoringResult("cat dog", "kæt dɒɡ", "kæt dɒɡ")
        assertEquals(2, result.words.size)
        assertTrue(result.words.all { it.score == 100 })
        assertEquals(3, result.words[0].phoneCount)
        assertEquals(listOf(0, 1), result.words.map { it.index })
    }

    @Test
    fun `accuracy and completeness separate sound quality from skipped words`() {
        // The second word is never attempted: every sound produced was correct,
        // but only half the sentence was said. These must not collapse into one
        // number, or the feedback cannot explain what to fix.
        val result = PhoneticComparator.calculateScoringResult("cat dog", "kæt dɒɡ", "kæt")

        assertEquals(100, result.accuracy)
        assertEquals(50, result.completeness)
        assertEquals(50, result.score)
        assertEquals(100, result.words[0].score)
        assertEquals(0, result.words[1].score)
        assertFalse(result.isMastery)
    }

    @Test
    fun `saying nothing scores zero accuracy and completeness`() {
        val result = PhoneticComparator.calculateScoringResult("cat", "kæt", "")
        assertEquals(0, result.accuracy)
        assertEquals(0, result.completeness)
        assertEquals(0, result.score)
        assertFalse(result.isMastery)
    }

    @Test
    fun `a perfect attempt masters`() {
        val result = PhoneticComparator.calculateScoringResult("cat dog", "kæt dɒɡ", "kæt dɒɡ")
        assertEquals(100, result.accuracy)
        assertEquals(100, result.completeness)
        assertTrue(result.isMastery)
    }

    @Test
    fun `a flawless take is PERFECT at sentence and word level`() {
        val result = PhoneticComparator.calculateScoringResult("cat dog", "kæt dɒɡ", "kæt dɒɡ")
        assertEquals(MasteryLevel.PERFECT, result.level)
        assertTrue(result.words.all { it.level == MasteryLevel.PERFECT })
    }

    @Test
    fun `mastery-band score blocked by a missing word is ALMOST, not MASTERED`() {
        // The level must follow the mastery gate, not the raw band: a great
        // average with one unsaid word is not "Mastered".
        val reference = List(7) { "kæt" }.joinToString(" ")
        val produced = List(6) { "kæt" }.joinToString(" ")
        val result = PhoneticComparator.calculateScoringResult("x", reference, produced)

        assertTrue("expected a mastery-band score, got ${result.score}", result.score >= 85)
        assertFalse(result.isMastery)
        assertEquals(MasteryLevel.ALMOST, result.level)
        // The unsaid word is somewhere in the sentence, not necessarily last.
        assertTrue(result.words.any { it.level == MasteryLevel.NOT_YET })
    }

    @Test
    fun `one destroyed word blocks mastery even when the average is high`() {
        // Six of seven words perfect, one word never said. The averaged score
        // clears the bar, but the learner did not say the sentence.
        val reference = List(7) { "kæt" }.joinToString(" ")
        val produced = List(6) { "kæt" }.joinToString(" ")
        val result = PhoneticComparator.calculateScoringResult("x", reference, produced)

        assertTrue("expected a mastery-band score, got ${result.score}", result.score >= 85)
        assertEquals(0, result.words.minOf { it.score })
        assertFalse("a destroyed word must block mastery", result.isMastery)
    }

    @Test
    fun `low completeness blocks mastery`() {
        val result = PhoneticComparator.calculateScoringResult("x", "kæt dɒɡ mæt", "kæt")
        assertFalse(result.isMastery)
    }

    // endregion

    // region generateLetterFeedback

    @Test
    fun `punctuation is always perfect`() {
        val alignment = listOf(
            PhonemeMatch("h", "h", MatchStatus.PERFECT),
            PhonemeMatch("ɪ", "ɪ", MatchStatus.PERFECT)
        )
        val feedback = PhoneticComparator.generateLetterFeedback("hi.", "hɪ", alignment)
        assertEquals(3, feedback.size)
        assertEquals(MatchStatus.PERFECT, feedback[2].status)
        assertEquals(".", feedback[2].char)
    }

    @Test
    fun `empty expected ipa marks everything perfect`() {
        val feedback = PhoneticComparator.generateLetterFeedback("abc", "", emptyList())
        assertEquals(3, feedback.size)
        assertTrue(feedback.all { it.status == MatchStatus.PERFECT })
    }

    @Test
    fun `missed phoneme propagates to its letters`() {
        val alignment = listOf(PhonemeMatch("æ", "x", MatchStatus.MISSED))
        val feedback = PhoneticComparator.generateLetterFeedback("a", "æ", alignment)
        assertEquals(MatchStatus.MISSED, feedback[0].status)
    }

    @Test
    fun `perfect alignment marks letters perfect`() {
        val alignment = listOf(
            PhonemeMatch("h", "h", MatchStatus.PERFECT),
            PhonemeMatch("ɪ", "ɪ", MatchStatus.PERFECT)
        )
        val feedback = PhoneticComparator.generateLetterFeedback("hi", "hɪ", alignment)
        assertTrue(feedback.all { it.status == MatchStatus.PERFECT })
    }

    // endregion

    // region getMinimalPairDescription

    @Test
    fun `known pair returns localized description`() {
        val en = PhoneticComparator.getMinimalPairDescription("p", "b", "en")
        val de = PhoneticComparator.getMinimalPairDescription("p", "b", "de")
        assertTrue(!en.isNullOrBlank())
        assertTrue(!de.isNullOrBlank())
        assertTrue(en != de)
    }

    @Test
    fun `unknown pair returns null`() {
        assertNull(PhoneticComparator.getMinimalPairDescription("q", "z", "en"))
    }

    @Test
    fun `unknown language falls back to english description`() {
        val fallback = PhoneticComparator.getMinimalPairDescription("p", "b", "xx")
        val en = PhoneticComparator.getMinimalPairDescription("p", "b", "en")
        assertEquals(en, fallback)
    }

    // endregion
}
