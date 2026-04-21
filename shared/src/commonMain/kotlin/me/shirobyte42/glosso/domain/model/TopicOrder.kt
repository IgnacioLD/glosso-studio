package me.shirobyte42.glosso.domain.model

/**
 * Canonical pedagogical order for topics, per language + CEFR level.
 *
 * EN/FR/ES/DE share the same modern progression (survival → abstract).
 * Latin uses its own classical/LLPSI-inspired progression.
 *
 * Keep in sync with `data/build/topics.py` — the data-generation pipeline
 * writes DBs using exactly these topic names.
 */
object TopicOrder {
    private val MODERN: Map<String, List<String>> = mapOf(
        "A1" to listOf(
            "Greetings", "Family", "Daily Life", "Food",
            "My House", "School", "Weather",
        ),
        "A2" to listOf(
            "Shopping", "Clothing", "Time & Dates", "Hobbies",
            "Health", "Jobs", "My Hometown", "Transport", "Travel",
        ),
        "B1" to listOf(
            "Education", "Work", "Lifestyle", "Entertainment",
            "Social Media", "Media", "Culture", "Society", "Environment",
        ),
        "B2" to listOf(
            "Relationships", "Identity", "Arts", "Technology",
            "Science", "News", "Digital Media", "Ethics",
        ),
        "C1" to listOf(
            "Psychology", "Economics", "Global Issues", "Politics",
            "Contemporary Society", "Law", "Philosophy",
        ),
        "C2" to listOf(
            "Idiomatic Expression", "Rhetoric", "Argumentation",
            "Literature", "Literary Criticism", "Linguistics",
            "Academic Discourse", "Diplomacy", "Advanced Sociology",
            "Metaphysics", "Quantum Physics",
        ),
    )

    private val LATIN: Map<String, List<String>> = mapOf(
        "A1" to listOf(
            "De Orbe Terrarum", "De Familia", "De Domo",
            "De Schola", "De Cibo", "Salutationes",
        ),
        "A2" to listOf(
            "In Taberna", "In Via", "De Tempore",
            "De Bestiis", "De Corpore", "De Vestibus",
        ),
        "B1" to listOf(
            "De Coniugio", "De Pueritia", "De Tempestate",
            "De Amicitia", "De Litteris", "De Viatoribus",
        ),
        "B2" to listOf(
            "De Re Publica", "De Philosophia", "De Historia",
            "De Poesi", "De Legibus", "De Religione",
        ),
        "C1" to listOf(
            "De Oratione", "De Bello", "De Moribus",
            "De Theatro", "De Disputatione", "De Imperatoribus",
        ),
        "C2" to listOf(
            "Cicero", "Vergilius", "Caesar",
            "Tacitus", "Seneca", "Horatius",
        ),
    )

    /** Return [actual] topics sorted pedagogically; unknown topics sink to the end alphabetically. */
    fun sortedTopics(language: String, level: String, actual: List<String>): List<String> {
        val canonical = if (language == "la") LATIN[level] else MODERN[level]
        if (canonical.isNullOrEmpty()) return actual.sorted()
        val order = canonical.withIndex().associate { (i, name) -> name to i }
        return actual.sortedWith(
            compareBy<String> { order[it] ?: Int.MAX_VALUE }.thenBy { it }
        )
    }
}
