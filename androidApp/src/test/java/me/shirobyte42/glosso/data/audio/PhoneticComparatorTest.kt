package me.shirobyte42.glosso.data.audio

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PhoneticComparatorTest {

    // region normalize

    @Test
    fun `normalize lowercases and keeps simple phones`() {
        assertEquals("kæt", PhoneticComparator.normalize("KÆT"))
    }

    @Test
    fun `normalize strips stress and length marks`() {
        assertEquals("kæti", PhoneticComparator.normalize("ˈkætˌiː"))
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
        // l ↔ r has similarity 0.8 in the EN matrix -> CLOSE -> 0.6 weight; a, ɪ, t match -> (0.6 + 3) / 4 = 90
        val result = PhoneticComparator.calculateScoringResult("light", "laɪt", "raɪt")
        assertEquals(MatchStatus.CLOSE, result.alignment[0].status)
        assertEquals(90, result.score)
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
        // l↔r CLOSE (0.6), ɪ↔ɪ and t↔t PERFECT (1.0 each) -> (0.6 + 1.0 + 1.0) / 3 = 86
        val result = PhoneticComparator.calculateScoringResult("lit", "lɪt", "rɪt")
        assertEquals(86, result.score)
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
    fun `normalized output strips stress and length marks`() {
        val result = PhoneticComparator.calculateScoringResult("sheep", "ˈʃiːp", "ʃiːp")
        assertEquals("ʃip", result.normalizedExpected)
        assertEquals("ʃip", result.normalizedActual)
    }

    @Test
    fun `alignment length matches expected phone count`() {
        val result = PhoneticComparator.calculateScoringResult("cat", "kæt", "ʃuːz")
        assertEquals(3, result.alignment.size)
        assertEquals(listOf("k", "æ", "t"), result.alignment.map { it.expected })
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
