package me.shirobyte42.glosso.data.audio

import android.util.Log
import kotlin.math.max

data class ScoringResult(
    val score: Int,
    val normalizedExpected: String,
    val normalizedActual: String,
    val alignment: List<PhonemeMatch> = emptyList()
)

data class PhonemeMatch(
    val expected: String,
    val actual: String,
    val status: MatchStatus // PERFECT, CLOSE, MISSED
)

data class LetterFeedbackInfo(
    val char: String,
    val status: MatchStatus
)

enum class MatchStatus { PERFECT, CLOSE, MISSED }

object PhoneticComparator {
    
    private const val TAG = "PhoneticComparator"

    // EN similarity matrix
    // Maps eng_to_ipa format ↔ Allosaurus format
    private val enSimilarityMatrix = mapOf(
        // Plosives
        setOf("k", "ɡ") to 0.98,
        // Fricatives - exact
        // Nasals - exact
        // Approximants
        setOf("l", "r") to 0.8,
        // Rhotic: eng_to_ipa uses r, Allosaurus uses ɹ or syllabic ɹ̩
        setOf("r", "ɹ") to 0.98,
        setOf("r", "ɹ̩") to 0.98,
        // Affricates: eng_to_ipa ʧ/ʤ vs Allosaurus t͡ʃ/d͡ʒ or tʃ/dʒ
        setOf("ʧ", "t͡ʃ") to 0.98,
        setOf("ʧ", "tʃ") to 0.98,
        setOf("ʤ", "d͡ʒ") to 0.98,
        setOf("ʤ", "dʒ") to 0.98,
        // Diphthong onsets
        setOf("ɪ", "j") to 0.85,
        setOf("ʊ", "w") to 0.85,
        // Vowels that differ between systems
        setOf("i", "ɪ") to 0.9,
        setOf("e", "ɛ") to 0.9,
        setOf("æ", "a") to 0.9,
        setOf("ɑ", "a") to 0.9,
        setOf("ɔ", "o") to 0.9,
        setOf("u", "ʊ") to 0.9,
        setOf("ə", "ʌ") to 0.95,
        setOf("ə", "ɤ") to 0.95,
        setOf("ʌ", "ə") to 0.95,
        setOf("ɑ", "ɔ") to 0.85
    )


    // FR similarity matrix (from glosso-studio-fr)
    private val frSimilarityMatrix = mapOf(
        setOf("p", "b") to 0.8,
        setOf("t", "d") to 0.8,
        setOf("k", "ɡ") to 0.8,
        setOf("t", "t̪") to 0.9,
        setOf("d", "d̪") to 0.9,
        setOf("f", "v") to 0.8,
        setOf("s", "z") to 0.8,
        setOf("ʃ", "ʒ") to 0.8,
        setOf("ʁ", "ʀ") to 0.9,
        setOf("ʁ", "r") to 0.7,
        setOf("m", "n") to 0.6,
        setOf("n", "ɲ") to 0.6,
        setOf("n", "ŋ") to 0.6,
        setOf("y", "u") to 0.7,
        setOf("ø", "e") to 0.7,
        setOf("œ", "ɛ") to 0.7,
        setOf("ø", "œ") to 0.8,
        setOf("ɛ̃", "ɑ̃") to 0.6,
        setOf("œ̃", "ɛ̃") to 0.7,
        setOf("ɔ̃", "ɑ̃") to 0.6,
        setOf("e", "ɛ") to 0.7,
        setOf("o", "ɔ") to 0.7,
        setOf("a", "ɑ") to 0.8,
        setOf("ə", "e") to 0.7,
        setOf("ə", "œ") to 0.7,
        setOf("j", "i") to 0.6,
        setOf("w", "u") to 0.6,
        setOf("ɥ", "y") to 0.7,
        setOf("ɥ", "w") to 0.6
    )

    private fun getMatrix(language: String) =
        if (language == "fr") frSimilarityMatrix else enSimilarityMatrix

    /** Human-readable descriptions for minimal phoneme pairs. */
    private val pairDescriptions: Map<Set<String>, Map<String, String>> = mapOf(
        setOf("p", "b") to mapOf("en" to "Put your fingers on your throat — B vibrates, P is silent. Same lips, different voice. Try: PAT vs. BAT.", "es" to "Pon los dedos en la garganta — la /b/ vibra, la /p/ no. Misma posición de los labios, distinto uso de la voz. Prueba: PAT vs. BAT.", "fr" to "Posez les doigts sur la gorge — le /b/ vibre, pas le /p/. Mêmes lèvres, voix différente. Essayez : PAT vs. BAT.", "de" to "Leg die Finger an den Hals — /b/ vibriert, /p/ nicht. Gleiche Lippen, andere Stimme. Probiere: PAT vs. BAT."),
        setOf("t", "d") to mapOf("en" to "D hums in your throat, T doesn't. Same tongue tap, different voice. Try: TEN vs. DEN.", "es" to "La /d/ zumba en la garganta, la /t/ no. Mismo toque de la lengua, distinto uso de la voz. Prueba: TEN vs. DEN.", "fr" to "Le /d/ résonne dans la gorge, pas le /t/. Même contact de langue, voix différente. Essayez : TEN vs. DEN.", "de" to "/d/ brummt im Hals, /t/ nicht. Gleicher Zungenschlag, andere Stimme. Probiere: TEN vs. DEN."),
        setOf("k", "ɡ") to mapOf("en" to "G vibrates in your throat, K doesn't. Try: COAT vs. GOAT.", "es" to "La /ɡ/ vibra en la garganta, la /k/ no. Prueba: COAT vs. GOAT.", "fr" to "Le /ɡ/ vibre dans la gorge, pas le /k/. Essayez : COAT vs. GOAT.", "de" to "/ɡ/ vibriert im Hals, /k/ nicht. Probiere: COAT vs. GOAT."),
        setOf("k", "g") to mapOf("en" to "G vibrates in your throat, K doesn't. Try: COAT vs. GOAT.", "es" to "La /g/ vibra en la garganta, la /k/ no. Prueba: COAT vs. GOAT.", "fr" to "Le /g/ vibre dans la gorge, pas le /k/. Essayez : COAT vs. GOAT.", "de" to "/g/ vibriert im Hals, /k/ nicht. Probiere: COAT vs. GOAT."),
        setOf("f", "v") to mapOf("en" to "V is a buzzing F — same lip-teeth contact, add voice. Try: FAN vs. VAN.", "es" to "La /v/ es una /f/ con zumbido — mismo contacto de labio y dientes, añade voz. Prueba: FAN vs. VAN.", "fr" to "Le /v/ est un /f/ bourdonnant — même contact lèvre-dents, ajoutez la voix. Essayez : FAN vs. VAN.", "de" to "/v/ ist ein summendes /f/ — gleicher Lippen-Zahn-Kontakt, nur mit Stimme. Probiere: FAN vs. VAN."),
        setOf("s", "z") to mapOf("en" to "Z is a buzzing S — same hiss, add voice from your throat. Try: SUE vs. ZOO.", "es" to "La /z/ es una /s/ con zumbido — mismo siseo, añade voz desde la garganta. Prueba: SUE vs. ZOO.", "fr" to "Le /z/ est un /s/ bourdonnant — même sifflement, ajoutez la voix. Essayez : SUE vs. ZOO.", "de" to "/z/ ist ein summendes /s/ — gleiches Zischen, Stimme aus dem Hals dazu. Probiere: SUE vs. ZOO."),
        setOf("ʃ", "ʒ") to mapOf("en" to "ZH is a buzzing SH — like SH but voiced. SH is in 'shoe'; ZH is in 'measure'.", "es" to "La /ʒ/ es una /ʃ/ con zumbido — como SH pero con voz. SH está en 'shoe'; ZH está en 'measure'.", "fr" to "Le /ʒ/ est un /ʃ/ bourdonnant — comme /ʃ/ mais voisé. /ʃ/ est dans 'shoe' ; /ʒ/ dans 'measure'.", "de" to "/ʒ/ ist ein summendes /ʃ/ — wie SCH, aber stimmhaft. SCH in 'shoe'; /ʒ/ in 'measure'."),
        setOf("θ", "ð") to mapOf("en" to "Put your tongue between your teeth. TH in 'think' (unvoiced) vs. TH in 'this' (voiced).", "es" to "Pon la lengua entre los dientes. TH en 'think' (sin voz) vs. TH en 'this' (con voz).", "fr" to "Placez la langue entre les dents. TH de 'think' (sourd) vs. TH de 'this' (voisé).", "de" to "Leg die Zunge zwischen die Zähne. TH in 'think' (stimmlos) vs. TH in 'this' (stimmhaft)."),
        setOf("m", "n") to mapOf("en" to "M closes your lips; N rests the tongue behind your teeth. Try humming 'M' then 'N' — you'll feel the difference.", "es" to "La /m/ cierra los labios; la /n/ apoya la lengua detrás de los dientes. Tararea 'M' y luego 'N' — notarás la diferencia.", "fr" to "Le /m/ ferme les lèvres ; le /n/ pose la langue derrière les dents. Fredonnez /m/ puis /n/ — vous sentirez la différence.", "de" to "/m/ schließt die Lippen; /n/ legt die Zunge hinter die Zähne. Summe 'M' und dann 'N' — du spürst den Unterschied."),
        setOf("n", "ŋ") to mapOf("en" to "N is at the front (teeth); NG is at the back (soft palate, as in 'sing'). Don't add a G at the end.", "es" to "La /n/ va adelante (dientes); la /ŋ/ va atrás (paladar blando, como en 'sing'). No añadas una G al final.", "fr" to "Le /n/ est à l'avant (dents) ; /ŋ/ est à l'arrière (palais mou, comme dans 'sing'). N'ajoutez pas de /g/ à la fin.", "de" to "/n/ ist vorn (an den Zähnen); /ŋ/ ist hinten (am Gaumensegel, wie in 'sing'). Kein /g/ am Ende anhängen."),
        setOf("l", "r") to mapOf("en" to "L touches the roof of your mouth with the tongue tip. R never touches — it curls or bunches. Try: LIGHT vs. RIGHT.", "es" to "La /l/ toca el paladar con la punta de la lengua. La /r/ inglesa nunca toca — se curva o se agrupa. Prueba: LIGHT vs. RIGHT.", "fr" to "Le /l/ touche le palais avec la pointe de la langue. Le /r/ ne touche jamais — il se recourbe. Essayez : LIGHT vs. RIGHT.", "de" to "/l/ berührt mit der Zungenspitze den Gaumen. /r/ berührt nichts — es krümmt oder bündelt sich. Probiere: LIGHT vs. RIGHT."),
        setOf("l", "ɾ") to mapOf("en" to "L holds your tongue against the roof. The tap (as in Spanish 'pero') is a single quick flick, not a hold.", "es" to "La /l/ mantiene la lengua contra el paladar. El toque (como en 'pero') es un golpecito rápido, no sostenido.", "fr" to "Le /l/ maintient la langue contre le palais. Le battement (comme dans 'pero' en espagnol) est un seul coup rapide, sans maintien.", "de" to "/l/ hält die Zunge am Gaumen. Der Schlag (wie im spanischen 'pero') ist ein einzelner schneller Tipper, kein Halten."),
        setOf("l", "ɹ") to mapOf("en" to "L touches the roof of your mouth; English R bunches the tongue back without touching. Try: LOCK vs. ROCK.", "es" to "La /l/ toca el paladar; la /ɹ/ inglesa agrupa la lengua atrás sin tocar. Prueba: LOCK vs. ROCK.", "fr" to "Le /l/ touche le palais ; le /ɹ/ anglais ramasse la langue en arrière sans toucher. Essayez : LOCK vs. ROCK.", "de" to "/l/ berührt den Gaumen; das englische /ɹ/ bündelt die Zunge zurück, ohne zu berühren. Probiere: LOCK vs. ROCK."),
        setOf("w", "v") to mapOf("en" to "Classical Latin V is pronounced like English W (veni = 'weh-nee', not 'veh-nee').", "es" to "La V del latín clásico se pronuncia como la W inglesa (veni = 'weh-nee', no 'veh-nee').", "fr" to "Le V du latin classique se prononce comme le W anglais (veni = 'weh-nee', pas 'veh-nee').", "de" to "Klassisch-lateinisches V wird wie englisches W gesprochen (veni = 'weh-nee', nicht 'veh-nee')."),
        setOf("j", "ʝ") to mapOf("en" to "Y in English 'yes' is a glide; the Spanish Y/LL ('yo', 'llave') is tighter, almost a J sound.", "es" to "La Y del inglés en 'yes' es una semivocal; la Y/LL del español ('yo', 'llave') es más cerrada, casi una J.", "fr" to "Le /j/ anglais de 'yes' est une semi-voyelle ; le Y/LL espagnol ('yo', 'llave') est plus tendu, presque un /ʝ/.", "de" to "/j/ im englischen 'yes' ist ein Gleitlaut; das spanische Y/LL ('yo', 'llave') ist enger, fast ein J-Laut."),
        setOf("tʃ", "ʃ") to mapOf("en" to "CH (chair) starts with a T before sliding into SH. SH (share) is pure fricative with no stop.", "es" to "La CH (chair) empieza con una T antes de deslizarse a SH. La SH (share) es pura fricativa, sin oclusión.", "fr" to "CH (chair) commence par un /t/ avant de glisser vers /ʃ/. SH (share) est une fricative pure sans occlusion.", "de" to "TSCH (chair) beginnt mit einem /t/, bevor es ins SCH gleitet. SCH (share) ist ein reiner Reibelaut ohne Verschluss."),
        setOf("dʒ", "ʒ") to mapOf("en" to "J (juice) starts with a D before the buzz. ZH (measure) is pure fricative with no stop.", "es" to "La J (juice) empieza con una D antes del zumbido. La ZH (measure) es pura fricativa, sin oclusión.", "fr" to "J (juice) commence par un /d/ avant le bourdonnement. ZH (measure) est une fricative pure sans occlusion.", "de" to "DSCH (juice) beginnt mit einem /d/ vor dem Summen. /ʒ/ (measure) ist ein reiner Reibelaut ohne Verschluss."),
        setOf("ts", "s") to mapOf("en" to "TS (German 'Zeit') starts with a T before the hiss. S is pure hiss.", "es" to "La TS (alemán 'Zeit') empieza con una T antes del siseo. La S es puro siseo.", "fr" to "TS (allemand 'Zeit') commence par un /t/ avant le sifflement. /s/ est un sifflement pur.", "de" to "TS (deutsches 'Zeit') beginnt mit einem /t/ vor dem Zischen. /s/ ist reines Zischen."),
        setOf("i", "ɪ") to mapOf("en" to "Tense EE (sheep) is long and smiley. Lax IH (ship) is short and relaxed. Don't stretch both the same.", "es" to "La EE tensa (sheep) es larga y con los labios estirados. La IH relajada (ship) es corta y suelta. No las alargues igual.", "fr" to "Le /i/ tendu (sheep) est long et souriant. Le /ɪ/ relâché (ship) est court et détendu. Ne les allongez pas pareil.", "de" to "Gespanntes EE (sheep) ist lang und lächelnd. Laxes IH (ship) ist kurz und entspannt. Nicht beide gleich dehnen."),
        setOf("iː", "ɪ") to mapOf("en" to "Long EE in 'sheep' vs. short IH in 'ship'. Length matters — don't collapse them.", "es" to "EE larga en 'sheep' vs. IH corta en 'ship'. La duración importa — no las mezcles.", "fr" to "Long /iː/ dans 'sheep' vs. court /ɪ/ dans 'ship'. La durée compte — ne les confondez pas.", "de" to "Langes /iː/ in 'sheep' vs. kurzes /ɪ/ in 'ship'. Die Länge zählt — wirf sie nicht zusammen."),
        setOf("e", "ɛ") to mapOf("en" to "E (like Spanish 'e') is tighter and higher than EH (English 'bed'). Drop your jaw slightly for EH.", "es" to "La E (como la 'e' española) es más cerrada y alta que la EH (inglés 'bed'). Baja un poco la mandíbula para EH.", "fr" to "Le /e/ (comme le 'e' espagnol) est plus tendu et plus fermé que /ɛ/ (anglais 'bed'). Ouvrez un peu la mâchoire pour /ɛ/.", "de" to "/e/ (wie das spanische 'e') ist enger und höher als /ɛ/ (englisches 'bed'). Kiefer für /ɛ/ leicht senken."),
        setOf("eɪ", "ɛ") to mapOf("en" to "AY (day) glides from E to EE. EH (dead) is a pure short vowel — no gliding.", "es" to "La AY (day) se desliza de E a EE. La EH (dead) es una vocal pura corta — sin deslizamiento.", "fr" to "AY (day) glisse de /e/ vers /i/. /ɛ/ (dead) est une voyelle courte pure — sans glissement.", "de" to "AY (day) gleitet von /e/ nach /i/. /ɛ/ (dead) ist ein reiner kurzer Vokal — kein Gleiten."),
        setOf("æ", "a") to mapOf("en" to "AA in 'cat' is a flatter, more front A. AH in 'palm' is deeper and more back. Open your mouth wider for AH.", "es" to "La AA en 'cat' es una A más plana y anterior. La AH en 'palm' es más grave y posterior. Abre más la boca para AH.", "fr" to "Le /æ/ de 'cat' est un /a/ plus plat et plus antérieur. Le /a/ de 'palm' est plus profond et postérieur. Ouvrez plus pour /a/.", "de" to "/æ/ in 'cat' ist ein flacheres, weiter vorne gebildetes A. /a/ in 'palm' ist tiefer und weiter hinten. Mund für /a/ weiter öffnen."),
        setOf("æ", "ɛ") to mapOf("en" to "AA in 'cat' is wider open than EH in 'bed'. Drop your jaw more for AA.", "es" to "La AA en 'cat' es más abierta que la EH en 'bed'. Baja más la mandíbula para AA.", "fr" to "Le /æ/ de 'cat' est plus ouvert que le /ɛ/ de 'bed'. Baissez davantage la mâchoire pour /æ/.", "de" to "/æ/ in 'cat' ist weiter geöffnet als /ɛ/ in 'bed'. Kiefer für /æ/ stärker senken."),
        setOf("ɑ", "a") to mapOf("en" to "AH in 'father' sits at the back. A in Spanish 'casa' sits more front. Move your tongue forward for the Spanish A.", "es" to "La AH en 'father' se produce atrás. La A del español 'casa' va más adelante. Mueve la lengua hacia adelante para la A española.", "fr" to "Le /ɑ/ de 'father' est postérieur. Le /a/ espagnol de 'casa' est plus antérieur. Avancez la langue pour le /a/ espagnol.", "de" to "/ɑ/ in 'father' sitzt hinten. /a/ im spanischen 'casa' sitzt weiter vorn. Zunge für das spanische A nach vorn."),
        setOf("ɑː", "ʌ") to mapOf("en" to "Long AH (father) is deep and open. UH (but) is shorter and higher in the mouth.", "es" to "La AH larga (father) es grave y abierta. La UH (but) es más corta y más alta en la boca.", "fr" to "Le /ɑː/ long (father) est profond et ouvert. /ʌ/ (but) est plus court et plus haut dans la bouche.", "de" to "Langes /ɑː/ (father) ist tief und offen. /ʌ/ (but) ist kürzer und höher im Mund."),
        setOf("u", "ʊ") to mapOf("en" to "OO in 'pool' is long with firmly rounded lips. OO in 'book' is shorter and less rounded.", "es" to "La OO en 'pool' es larga con labios bien redondeados. La OO en 'book' es más corta y menos redondeada.", "fr" to "Le /u/ de 'pool' est long avec les lèvres bien arrondies. Le /ʊ/ de 'book' est plus court et moins arrondi.", "de" to "/uː/ in 'pool' ist lang mit fest gerundeten Lippen. /ʊ/ in 'book' ist kürzer und weniger gerundet."),
        setOf("uː", "ʊ") to mapOf("en" to "Long OO (pool) vs. short OO (book) — tongue position is similar, but length and lip-rounding differ.", "es" to "OO larga (pool) vs. OO corta (book) — la lengua está parecida, pero cambian la duración y el redondeo.", "fr" to "Long /uː/ (pool) vs. court /ʊ/ (book) — position de langue proche, mais durée et arrondi diffèrent.", "de" to "Langes /uː/ (pool) vs. kurzes /ʊ/ (book) — Zungenstellung ähnlich, aber Länge und Lippenrundung unterscheiden sich."),
        setOf("ə", "ʌ") to mapOf("en" to "Schwa (sofa) is a completely relaxed, neutral sound. UH (cup) is slightly stronger and more open.", "es" to "La schwa (sofa) es un sonido totalmente relajado y neutro. La UH (cup) es algo más fuerte y abierta.", "fr" to "Le schwa /ə/ (sofa) est un son neutre complètement détendu. /ʌ/ (cup) est un peu plus fort et plus ouvert.", "de" to "Schwa (sofa) ist ein völlig entspannter, neutraler Laut. /ʌ/ (cup) ist etwas stärker und offener."),
        setOf("ə", "ɤ") to mapOf("en" to "Both are relaxed neutral vowels — slight tongue position difference, often interchangeable in rapid speech.", "es" to "Ambas son vocales neutras relajadas — leve diferencia de posición de la lengua, a menudo intercambiables en el habla rápida.", "fr" to "Les deux sont des voyelles neutres relâchées — légère différence de position de langue, souvent interchangeables en parole rapide.", "de" to "Beide sind entspannte neutrale Vokale — kleiner Unterschied in der Zungenstellung, oft austauschbar im schnellen Sprechen."),
        setOf("ɔ", "o") to mapOf("en" to "AW (thought) is open and relaxed. OH (note) is more rounded and higher in the mouth.", "es" to "La AW (thought) es abierta y relajada. La OH (note) es más redondeada y más alta en la boca.", "fr" to "Le /ɔ/ (thought) est ouvert et relâché. Le /o/ (note) est plus arrondi et plus haut dans la bouche.", "de" to "/ɔ/ (thought) ist offen und entspannt. /o/ (note) ist stärker gerundet und höher im Mund."),
        setOf("ɔː", "oʊ") to mapOf("en" to "AW (bought) is a pure open vowel. OH (boat) glides from O to UU — it's a diphthong.", "es" to "La AW (bought) es una vocal abierta pura. La OH (boat) se desliza de O a UU — es un diptongo.", "fr" to "/ɔː/ (bought) est une voyelle ouverte pure. /oʊ/ (boat) glisse de O vers U — c'est une diphtongue.", "de" to "/ɔː/ (bought) ist ein reiner offener Vokal. /oʊ/ (boat) gleitet von O nach U — es ist ein Diphthong."),
        setOf("aɪ", "eɪ") to mapOf("en" to "EYE (time) glides from AH to EE. AY (tame) glides from E to EE. Different starting point.", "es" to "La EYE (time) se desliza de AH a EE. La AY (tame) se desliza de E a EE. Distinto punto de partida.", "fr" to "/aɪ/ (time) glisse de /a/ vers /i/. /eɪ/ (tame) glisse de /e/ vers /i/. Point de départ différent.", "de" to "/aɪ/ (time) gleitet von /a/ nach /i/. /eɪ/ (tame) gleitet von /e/ nach /i/. Anderer Ausgangspunkt."),
        setOf("aʊ", "oʊ") to mapOf("en" to "OW (now) glides from AH to OO. OH (no) glides from O to OO. Different starting vowel.", "es" to "La OW (now) se desliza de AH a OO. La OH (no) se desliza de O a OO. Vocal inicial distinta.", "fr" to "/aʊ/ (now) glisse de /a/ vers /u/. /oʊ/ (no) glisse de /o/ vers /u/. Voyelle de départ différente.", "de" to "/aʊ/ (now) gleitet von /a/ nach /u/. /oʊ/ (no) gleitet von /o/ nach /u/. Anderer Ausgangsvokal."),
        setOf("ʁ", "r") to mapOf("en" to "French R is made at the back of the throat (like a soft gargle). It's NOT the rolled tongue R of Spanish or Italian.", "es" to "La R francesa se produce en el fondo de la garganta (como un gargareo suave). NO es la R vibrante de la lengua del español o el italiano.", "fr" to "Le /ʁ/ français se fait au fond de la gorge (comme un léger raclement). Ce n'est PAS le /r/ roulé espagnol ou italien.", "de" to "Das französische /ʁ/ entsteht hinten im Rachen (wie ein sanftes Gurgeln). Es ist NICHT das gerollte Zungen-R wie im Spanischen oder Italienischen."),
        setOf("ʁ", "ʀ") to mapOf("en" to "Both are French R variants from the back of the throat — one is a soft friction, the other a trill. Use the softer one.", "es" to "Ambas son variantes de la R francesa desde el fondo de la garganta — una es una fricción suave, la otra una vibrante. Usa la más suave.", "fr" to "Les deux sont des variantes françaises venant du fond de la gorge — l'une est une friction douce, l'autre un roulement. Utilisez la plus douce.", "de" to "Beide sind französische R-Varianten aus dem hinteren Rachen — eine ist sanfte Reibung, die andere ein Triller. Nimm die sanftere."),
        setOf("y", "u") to mapOf("en" to "French U (tu) — shape your lips as if whistling OO, but say EE with your tongue. OU (tout) is a normal OO.", "es" to "La U francesa (tu) — pon los labios como para silbar OO, pero di EE con la lengua. La OU (tout) es una OO normal.", "fr" to "Le U français (tu) — arrondissez les lèvres comme pour siffler OU, mais dites /i/ avec la langue. OU (tout) est un /u/ normal.", "de" to "Französisches /y/ (tu) — Lippen wie zum Pfeifen runden, aber mit der Zunge /i/ sagen. OU (tout) ist ein normales /u/."),
        setOf("ø", "e") to mapOf("en" to "EU (feu) — round your lips while saying E. É (fée) is the same E without the lip rounding.", "es" to "EU (feu) — redondea los labios mientras dices E. La É (fée) es la misma E sin redondeo de labios.", "fr" to "EU (feu) — arrondissez les lèvres en disant /e/. É (fée) est le même /e/ sans arrondi des lèvres.", "de" to "EU (feu) — runde die Lippen und sag gleichzeitig /e/. É (fée) ist dasselbe /e/ ohne Lippenrundung."),
        setOf("œ", "ɛ") to mapOf("en" to "EU open (peur) — round your lips while saying the open EH (père). Jaw slightly lower than for closed EU.", "es" to "EU abierta (peur) — redondea los labios mientras dices la EH abierta (père). Mandíbula un poco más baja que para la EU cerrada.", "fr" to "EU ouvert (peur) — arrondissez les lèvres en disant le /ɛ/ ouvert (père). Mâchoire un peu plus basse que pour EU fermé.", "de" to "Offenes EU (peur) — runde die Lippen und sag das offene /ɛ/ (père). Kiefer etwas tiefer als beim geschlossenen EU."),
        setOf("ø", "œ") to mapOf("en" to "Both are rounded front vowels. Closed EU (peu) is tighter, open EU (peur) has a more open mouth.", "es" to "Ambas son vocales anteriores redondeadas. La EU cerrada (peu) es más cerrada, la EU abierta (peur) tiene la boca más abierta.", "fr" to "Les deux sont des voyelles antérieures arrondies. EU fermé (peu) est plus tendu, EU ouvert (peur) a la bouche plus ouverte.", "de" to "Beide sind gerundete Vordervokale. Geschlossenes EU (peu) ist enger, offenes EU (peur) hat den Mund weiter offen."),
        setOf("ɥ", "w") to mapOf("en" to "UI (nuit) is a front-rounded glide (tongue forward, lips rounded). OUI uses a back glide (tongue back, lips rounded).", "es" to "La UI (nuit) es una semivocal anterior redondeada (lengua adelante, labios redondos). OUI usa una semivocal posterior (lengua atrás, labios redondos).", "fr" to "UI (nuit) est une semi-voyelle antérieure arrondie (langue avant, lèvres arrondies). OUI utilise une semi-voyelle postérieure (langue arrière, lèvres arrondies).", "de" to "UI (nuit) ist ein vorne-gerundeter Gleitlaut (Zunge vorn, Lippen gerundet). OUI nutzt einen hinteren Gleitlaut (Zunge hinten, Lippen gerundet)."),
        setOf("ɥ", "y") to mapOf("en" to "The semi-vowel in 'nuit' is a quick glide before the next vowel. The full vowel 'tu' holds longer.", "es" to "La semivocal de 'nuit' es un deslizamiento rápido antes de la siguiente vocal. La vocal plena 'tu' se mantiene más tiempo.", "fr" to "La semi-voyelle de 'nuit' est un glissement rapide avant la voyelle suivante. La voyelle pleine 'tu' se tient plus longtemps.", "de" to "Der Halbvokal in 'nuit' ist ein kurzer Gleitlaut vor dem nächsten Vokal. Der volle Vokal 'tu' wird länger gehalten."),
        setOf("ɔ̃", "ɑ̃") to mapOf("en" to "ON (bon) rounds the lips and pushes the sound through the nose. AN (banc) keeps lips unrounded, same nasal airflow.", "es" to "ON (bon) redondea los labios y empuja el sonido por la nariz. AN (banc) deja los labios sin redondear, con el mismo flujo nasal.", "fr" to "ON (bon) arrondit les lèvres et fait passer le son par le nez. AN (banc) garde les lèvres non arrondies, même passage nasal.", "de" to "ON (bon) rundet die Lippen und schickt den Laut durch die Nase. AN (banc) hält die Lippen ungerundet, gleicher Nasenstrom."),
        setOf("ɛ̃", "ɑ̃") to mapOf("en" to "IN (vin) is a nasal front vowel (like a nasal E). AN (banc) is a nasal back vowel (deeper and more open).", "es" to "IN (vin) es una vocal nasal anterior (como una E nasal). AN (banc) es una vocal nasal posterior (más grave y abierta).", "fr" to "IN (vin) est une voyelle nasale antérieure (comme un /ɛ/ nasalisé). AN (banc) est une voyelle nasale postérieure (plus profonde et plus ouverte).", "de" to "IN (vin) ist ein nasaler Vordervokal (wie ein nasales E). AN (banc) ist ein nasaler Hintervokal (tiefer und offener)."),
        setOf("ɛ̃", "œ̃") to mapOf("en" to "IN (vin) is unrounded; UN (brun) is rounded. In modern Parisian French these are often merged to IN.", "es" to "IN (vin) no lleva los labios redondeados; UN (brun) sí. En el francés parisino moderno suelen fusionarse en IN.", "fr" to "IN (vin) est non arrondi ; UN (brun) est arrondi. En français parisien moderne, ils se confondent souvent avec IN.", "de" to "IN (vin) ist ungerundet; UN (brun) ist gerundet. Im modernen Pariser Französisch werden sie oft zu IN verschmolzen."),
        setOf("ɲ", "n") to mapOf("en" to "GN (agneau) — press the middle of your tongue to the roof of your mouth. N is made with just the tongue tip.", "es" to "GN (agneau) — aprieta el medio de la lengua contra el paladar. La N se hace solo con la punta de la lengua.", "fr" to "GN (agneau) — pressez le milieu de la langue contre le palais. /n/ se fait juste avec la pointe de la langue.", "de" to "GN (agneau) — drücke den mittleren Teil der Zunge an den Gaumen. /n/ wird nur mit der Zungenspitze gebildet."),
        setOf("ʒ", "ʃ") to mapOf("en" to "J (jour) buzzes in your throat. CH (chou) is silent — same mouth position, different voicing.", "es" to "La J (jour) zumba en la garganta. La CH (chou) no tiene voz — misma posición de la boca, distinto uso de la voz.", "fr" to "J (jour) bourdonne dans la gorge. CH (chou) est sourd — même position de bouche, voisement différent.", "de" to "J (jour) summt im Hals. CH (chou) ist stimmlos — gleiche Mundstellung, anderes Stimmverhalten."),
        setOf("β", "b") to mapOf("en" to "Spanish B/V between vowels (lobo) — lips don't fully close, air keeps flowing. At start of word or after M/N it's a full stop.", "es" to "La B/V española entre vocales (lobo) — los labios no se cierran del todo, el aire sigue fluyendo. Al inicio de palabra o tras M/N es una oclusiva plena.", "fr" to "Le B/V espagnol entre voyelles (lobo) — les lèvres ne se ferment pas complètement, l'air continue de passer. En début de mot ou après M/N, c'est une occlusion pleine.", "de" to "Spanisches B/V zwischen Vokalen (lobo) — Lippen schließen sich nicht ganz, Luft strömt weiter. Am Wortanfang oder nach M/N ist es ein voller Verschluss."),
        setOf("ð", "d") to mapOf("en" to "Spanish D between vowels (cada) is soft, like English TH in 'this'. At start of word it's a crisp full D.", "es" to "La D española entre vocales (cada) es suave, como la TH inglesa en 'this'. Al inicio de palabra es una D plena y firme.", "fr" to "Le D espagnol entre voyelles (cada) est doux, comme le TH anglais de 'this'. En début de mot, c'est un D franc et plein.", "de" to "Spanisches D zwischen Vokalen (cada) ist weich, wie das englische TH in 'this'. Am Wortanfang ein klares volles D."),
        setOf("ɣ", "ɡ") to mapOf("en" to "Spanish G between vowels (lago) is soft — the tongue doesn't fully touch. At start it's a full hard G (gato).", "es" to "La G española entre vocales (lago) es suave — la lengua no toca del todo. Al inicio es una G plena y dura (gato).", "fr" to "Le G espagnol entre voyelles (lago) est doux — la langue ne touche pas complètement. En début, c'est un G dur plein (gato).", "de" to "Spanisches G zwischen Vokalen (lago) ist weich — die Zunge berührt nicht ganz. Am Anfang ist es ein volles hartes G (gato)."),
        setOf("ɣ", "g") to mapOf("en" to "Spanish G between vowels (lago) is soft — the tongue doesn't fully touch. At start it's a full hard G (gato).", "es" to "La G española entre vocales (lago) es suave — la lengua no toca del todo. Al inicio es una G plena y dura (gato).", "fr" to "Le G espagnol entre voyelles (lago) est doux — la langue ne touche pas complètement. En début, c'est un G dur plein (gato).", "de" to "Spanisches G zwischen Vokalen (lago) ist weich — die Zunge berührt nicht ganz. Am Anfang ist es ein volles hartes G (gato)."),
        setOf("ɾ", "r") to mapOf("en" to "Single R (pero) is ONE quick tongue flick. Double RR (perro) is a rolled trill — the tongue bounces several times.", "es" to "La R simple (pero) es UN golpecito rápido de la lengua. La RR doble (perro) es vibrante — la lengua rebota varias veces.", "fr" to "Le R simple (pero) est UN seul coup de langue rapide. Le RR double (perro) est un roulement — la langue bat plusieurs fois.", "de" to "Einfaches R (pero) ist EIN schneller Zungenschlag. Doppeltes RR (perro) ist ein gerollter Triller — die Zunge hüpft mehrfach."),
        setOf("ɾ", "ɹ") to mapOf("en" to "Spanish R is a quick tongue-tip flick against the roof. English R curls back without any tap — very different action.", "es" to "La R española es un golpecito rápido de la punta de la lengua contra el paladar. La R inglesa se curva atrás sin tocar — acción muy distinta.", "fr" to "Le /ɾ/ espagnol est un coup rapide de la pointe de la langue contre le palais. Le /ɹ/ anglais se recourbe sans toucher — geste très différent.", "de" to "Spanisches R ist ein schneller Zungenspitzen-Schlag gegen den Gaumen. Englisches /ɹ/ krümmt sich zurück ohne Schlag — ganz andere Bewegung."),
        setOf("r", "ɹ") to mapOf("en" to "Spanish trilled R bounces the tongue tip repeatedly. English R is a single bunch — no bouncing.", "es" to "La R vibrante española hace rebotar la punta de la lengua varias veces. La R inglesa es un solo agrupamiento — sin rebotes.", "fr" to "Le R roulé espagnol bat la pointe de la langue plusieurs fois. Le /ɹ/ anglais est un seul ramassé — sans battement.", "de" to "Spanisches gerolltes R lässt die Zungenspitze mehrfach springen. Englisches /ɹ/ ist ein einzelnes Bündeln — kein Springen."),
        setOf("ʝ", "ʎ") to mapOf("en" to "In most Spanish dialects Y and LL sound the same. LL traditionally uses a palatal L (press middle of tongue to roof).", "es" to "En la mayoría de dialectos del español, Y y LL suenan igual. La LL tradicional usa una L palatal (medio de la lengua contra el paladar).", "fr" to "Dans la plupart des dialectes espagnols, Y et LL se prononcent pareil. LL utilise traditionnellement un L palatal (milieu de la langue contre le palais).", "de" to "In den meisten spanischen Dialekten klingen Y und LL gleich. LL nutzt traditionell ein palatales L (Zungenmitte an den Gaumen drücken)."),
        setOf("ʎ", "l") to mapOf("en" to "LL (lluvia) presses the middle of the tongue against the roof. L (luna) uses just the tongue tip.", "es" to "LL (lluvia) aprieta el medio de la lengua contra el paladar. L (luna) usa solo la punta de la lengua.", "fr" to "LL (lluvia) presse le milieu de la langue contre le palais. /l/ (luna) n'utilise que la pointe de la langue.", "de" to "LL (lluvia) drückt die Zungenmitte an den Gaumen. /l/ (luna) nutzt nur die Zungenspitze."),
        setOf("x", "h") to mapOf("en" to "Spanish J (jota) is raspier — made at the back of the mouth with friction. English H is just breath with no friction.", "es" to "La J española (jota) es más áspera — se hace atrás en la boca con fricción. La H inglesa es solo aire, sin fricción.", "fr" to "Le J espagnol (jota) est plus râpeux — fait au fond de la bouche avec friction. Le H anglais est juste un souffle sans friction.", "de" to "Spanisches J (jota) ist rauer — hinten im Mund mit Reibung gebildet. Englisches H ist nur Atem ohne Reibung."),
        setOf("x", "k") to mapOf("en" to "Spanish J hisses at the back of the mouth. K is a hard stop — same place, but stop vs. hiss.", "es" to "La J española sisea atrás en la boca. La K es una oclusiva dura — mismo lugar, pero oclusiva vs. siseo.", "fr" to "Le J espagnol siffle au fond de la bouche. Le /k/ est une occlusion dure — même lieu, mais occlusion vs. sifflement.", "de" to "Spanisches J zischt hinten im Mund. /k/ ist ein harter Verschluss — gleicher Ort, aber Verschluss vs. Zischen."),
        setOf("x", "ç") to mapOf("en" to "German CH after A/O/U (Bach) is a back raspy sound. After E/I (ich) it's a soft front hiss like a whispered Y.", "es" to "La CH alemana tras A/O/U (Bach) es un sonido áspero posterior. Tras E/I (ich) es un siseo anterior suave, como una Y susurrada.", "fr" to "Le CH allemand après A/O/U (Bach) est un son râpeux postérieur. Après E/I (ich), c'est un doux sifflement antérieur, comme un Y chuchoté.", "de" to "Deutsches CH nach A/O/U (Bach) ist ein hinterer rauer Laut. Nach E/I (ich) ist es ein weiches vorderes Zischen wie ein geflüstertes Y."),
        setOf("ʁ", "ɐ") to mapOf("en" to "At the start of a syllable, German R is a back-of-throat sound. At the end (or after a vowel), it becomes a relaxed UH-like vowel.", "es" to "Al inicio de sílaba, la R alemana es un sonido del fondo de la garganta. Al final (o tras una vocal), se convierte en una vocal relajada tipo UH.", "fr" to "En début de syllabe, le R allemand est un son de fond de gorge. À la fin (ou après voyelle), il devient une voyelle relâchée proche d'un /ɐ/.", "de" to "Am Silbenanfang ist das deutsche R ein hinterer Rachenlaut. Am Ende (oder nach einem Vokal) wird es zu einem entspannten /ɐ/-ähnlichen Vokal."),
        setOf("pf", "f") to mapOf("en" to "PF (Pferd) starts with a P before the F — both sounds in quick succession. F alone is just the fricative.", "es" to "PF (Pferd) empieza con una P antes de la F — ambos sonidos en rápida sucesión. La F sola es solo la fricativa.", "fr" to "PF (Pferd) commence par un /p/ avant le /f/ — les deux sons s'enchaînent rapidement. /f/ seul n'est que la fricative.", "de" to "PF (Pferd) beginnt mit einem /p/ vor dem /f/ — beide Laute in schneller Folge. /f/ allein ist nur der Reibelaut."),
        setOf("ts", "z") to mapOf("en" to "Z (Zeit) is pronounced TS — a T immediately followed by S. German S between vowels is voiced like English Z.", "es" to "La Z (Zeit) se pronuncia TS — una T seguida de inmediato por una S. La S alemana entre vocales tiene voz, como la Z inglesa.", "fr" to "Z (Zeit) se prononce TS — un /t/ suivi immédiatement d'un /s/. Le S allemand entre voyelles est voisé, comme le Z anglais.", "de" to "Z (Zeit) wird TS gesprochen — ein /t/ sofort gefolgt von /s/. Deutsches S zwischen Vokalen ist stimmhaft wie englisches Z."),
        setOf("yː", "uː") to mapOf("en" to "Ü (Tür) — round your lips like OO, but keep your tongue forward as for EE. U (Tuch) is a normal back OO.", "es" to "Ü (Tür) — redondea los labios como para OO, pero mantén la lengua adelante como para EE. La U (Tuch) es una OO posterior normal.", "fr" to "Ü (Tür) — arrondissez les lèvres comme pour /uː/, mais gardez la langue en avant comme pour /iː/. U (Tuch) est un /uː/ postérieur normal.", "de" to "Ü (Tür) — runde die Lippen wie für OO, aber halte die Zunge vorn wie für EE. U (Tuch) ist ein normales hinteres /uː/."),
        setOf("ʏ", "yː") to mapOf("en" to "Short Ü (dünn) is relaxed and quick. Long Ü (Tür) is held longer with tighter lip rounding.", "es" to "Ü corta (dünn) es relajada y rápida. Ü larga (Tür) se sostiene más, con redondeo de labios más cerrado.", "fr" to "Ü court (dünn) est relâché et rapide. Ü long (Tür) se tient plus longtemps avec un arrondi de lèvres plus serré.", "de" to "Kurzes Ü (dünn) ist entspannt und schnell. Langes Ü (Tür) wird länger gehalten mit fester Lippenrundung."),
        setOf("ø", "oː") to mapOf("en" to "Ö (schön) — round your lips like O, but keep your tongue forward as for E. O (Sohn) is a normal back O.", "es" to "Ö (schön) — redondea los labios como para O, pero mantén la lengua adelante como para E. La O (Sohn) es una O posterior normal.", "fr" to "Ö (schön) — arrondissez les lèvres comme pour /oː/, mais gardez la langue en avant comme pour /e/. O (Sohn) est un /oː/ postérieur normal.", "de" to "Ö (schön) — runde die Lippen wie für O, aber halte die Zunge vorn wie für E. O (Sohn) ist ein normales hinteres O."),
        setOf("œ", "ɔ") to mapOf("en" to "Short Ö (können) is front-rounded. Short O (Sonne) is back-rounded. Move your tongue forward for Ö.", "es" to "Ö corta (können) es anterior redondeada. O corta (Sonne) es posterior redondeada. Mueve la lengua adelante para Ö.", "fr" to "Ö court (können) est antérieur arrondi. O court (Sonne) est postérieur arrondi. Avancez la langue pour Ö.", "de" to "Kurzes Ö (können) ist vorne gerundet. Kurzes O (Sonne) ist hinten gerundet. Zunge für Ö nach vorn bewegen."),
        setOf("ɛː", "eː") to mapOf("en" to "Ä long (Bär) is slightly lower and more open than E long (Beet). Drop your jaw a bit for Ä.", "es" to "Ä larga (Bär) es un poco más baja y abierta que E larga (Beet). Baja un poco la mandíbula para Ä.", "fr" to "Ä long (Bär) est un peu plus bas et plus ouvert que E long (Beet). Baissez un peu la mâchoire pour Ä.", "de" to "Langes Ä (Bär) ist etwas tiefer und offener als langes E (Beet). Kiefer für Ä etwas senken."),
        setOf("ə", "ɐ") to mapOf("en" to "Schwa (bitte) is a silent-E sound. Vocalic R (bitter) is slightly more open, like a softened UH.", "es" to "La schwa (bitte) es un sonido de E muda. La R vocálica (bitter) es algo más abierta, como una UH suavizada.", "fr" to "Le schwa /ə/ (bitte) est un E muet. Le R vocalisé /ɐ/ (bitter) est un peu plus ouvert, comme un /ʌ/ adouci.", "de" to "Schwa (bitte) ist ein stummer E-Laut. Vokalisches R (bitter) ist etwas offener, wie ein weiches /ɐ/."),
        setOf("a", "aː") to mapOf("en" to "Length matters in German! Stadt (short A, quick) vs. Staat (long A, held). Double the duration.", "es" to "¡La duración importa en alemán! Stadt (A corta, rápida) vs. Staat (A larga, sostenida). Duplica la duración.", "fr" to "La durée compte en allemand ! Stadt (A court, rapide) vs. Staat (A long, tenu). Doublez la durée.", "de" to "Länge ist im Deutschen wichtig! Stadt (kurzes A, schnell) vs. Staat (langes A, gehalten). Doppelte Dauer."),
        setOf("ɪ", "iː") to mapOf("en" to "Short I (bitten) is relaxed, like English 'bit'. Long I (bieten) is tense and held, like English 'beat'.", "es" to "I corta (bitten) es relajada, como la inglesa 'bit'. I larga (bieten) es tensa y sostenida, como la inglesa 'beat'.", "fr" to "I court (bitten) est relâché, comme l'anglais 'bit'. I long (bieten) est tendu et tenu, comme l'anglais 'beat'.", "de" to "Kurzes I (bitten) ist entspannt, wie im englischen 'bit'. Langes I (bieten) ist gespannt und gehalten, wie 'beat'."),
        setOf("ʊ", "uː") to mapOf("en" to "Short U (muss) is relaxed like English 'book'. Long U (Mus) is tense and held like English 'moose'.", "es" to "U corta (muss) es relajada como la inglesa 'book'. U larga (Mus) es tensa y sostenida como la inglesa 'moose'.", "fr" to "U court (muss) est relâché, comme l'anglais 'book'. U long (Mus) est tendu et tenu, comme l'anglais 'moose'.", "de" to "Kurzes U (muss) ist entspannt wie im englischen 'book'. Langes U (Mus) ist gespannt und gehalten wie 'moose'."),
        setOf("aː", "a") to mapOf("en" to "Latin long A is held twice as long as short A. Length changes meaning: MĀLUM (apple) vs. MALUM (bad).", "es" to "La A larga del latín se sostiene el doble que la A corta. La duración cambia el significado: MĀLUM (manzana) vs. MALUM (malo).", "fr" to "Le A long latin se tient deux fois plus longtemps que le A court. La durée change le sens : MĀLUM (pomme) vs. MALUM (mauvais).", "de" to "Lateinisches langes A wird doppelt so lang gehalten wie kurzes A. Länge verändert die Bedeutung: MĀLUM (Apfel) vs. MALUM (schlecht)."),
        setOf("eː", "e") to mapOf("en" to "Latin long E is held twice as long as short E. Length contrasts real words — LĒGIT (reads) vs. LEGIT (gathers).", "es" to "La E larga del latín se sostiene el doble que la E corta. La duración distingue palabras reales — LĒGIT (lee) vs. LEGIT (recoge).", "fr" to "Le E long latin se tient deux fois plus longtemps que le E court. La durée distingue des mots réels — LĒGIT (lit) vs. LEGIT (rassemble).", "de" to "Lateinisches langes E wird doppelt so lang gehalten wie kurzes E. Länge unterscheidet echte Wörter — LĒGIT (liest) vs. LEGIT (sammelt)."),
        setOf("iː", "i") to mapOf("en" to "Latin long I is held twice as long as short I. Keep the tongue in the same position — just longer.", "es" to "La I larga del latín se sostiene el doble que la I corta. Mantén la lengua en la misma posición — solo más tiempo.", "fr" to "Le I long latin se tient deux fois plus longtemps que le I court. Gardez la langue au même endroit — juste plus longtemps.", "de" to "Lateinisches langes I wird doppelt so lang gehalten wie kurzes I. Zunge in gleicher Stellung — nur länger."),
        setOf("oː", "o") to mapOf("en" to "Latin long O is held twice as long as short O. Same mouth shape, double duration.", "es" to "La O larga del latín se sostiene el doble que la O corta. Misma forma de la boca, duración duplicada.", "fr" to "Le O long latin se tient deux fois plus longtemps que le O court. Même forme de bouche, durée doublée.", "de" to "Lateinisches langes O wird doppelt so lang gehalten wie kurzes O. Gleiche Mundform, doppelte Dauer."),
        setOf("uː", "u") to mapOf("en" to "Latin long U is held twice as long as short U. Same mouth shape, double duration.", "es" to "La U larga del latín se sostiene el doble que la U corta. Misma forma de la boca, duración duplicada.", "fr" to "Le U long latin se tient deux fois plus longtemps que le U court. Même forme de bouche, durée doublée.", "de" to "Lateinisches langes U wird doppelt so lang gehalten wie kurzes U. Gleiche Mundform, doppelte Dauer."),
        setOf("kʷ", "k") to mapOf("en" to "QU in 'quod' is K with rounded lips — almost 'kw'. K alone has unrounded lips.", "es" to "QU en 'quod' es K con labios redondeados — casi 'kw'. La K sola lleva los labios sin redondear.", "fr" to "QU dans 'quod' est un /k/ avec lèvres arrondies — presque 'kw'. /k/ seul a les lèvres non arrondies.", "de" to "QU in 'quod' ist /k/ mit gerundeten Lippen — fast 'kw'. /k/ allein hat ungerundete Lippen."),
        setOf("ɡʷ", "ɡ") to mapOf("en" to "GU before a vowel (lingua) is G with rounded lips — almost 'gw'. G alone has unrounded lips.", "es" to "GU antes de vocal (lingua) es G con labios redondeados — casi 'gw'. La G sola lleva los labios sin redondear.", "fr" to "GU devant une voyelle (lingua) est un /ɡ/ avec lèvres arrondies — presque 'gw'. /ɡ/ seul a les lèvres non arrondies.", "de" to "GU vor einem Vokal (lingua) ist /ɡ/ mit gerundeten Lippen — fast 'gw'. /ɡ/ allein hat ungerundete Lippen."),
    )

    /** Returns a human-readable description if two phonemes form a known minimal pair. */
    fun getMinimalPairDescription(expected: String, actual: String, uiLanguage: String = "en"): String? {
        val entry = pairDescriptions[setOf(expected, actual)] ?: return null
        return entry[uiLanguage] ?: entry["en"]
    }

    fun calculateScoringResult(text: String, expected: String, actual: String, language: String = "en"): ScoringResult {
        val matrix = getMatrix(language)
        val expectedList = getNormalizedPhoneList(expected)
        val actualList = getNormalizedPhoneList(actual)
        
        if (expectedList.isEmpty()) {
            return ScoringResult(if (actualList.isEmpty()) 100 else 0, "", "")
        }
        
        // Simple position-by-position comparison
        // For each expected phoneme, find the best match in actual
        var totalWeight = 0.0
        var matchWeight = 0.0
        val alignment = mutableListOf<PhonemeMatch>()
        
        for (exp in expectedList) {
            totalWeight += 1.0
            // Find best similarity with any position in actual
            var bestSim = 0.0
            var bestMatch = "-"
            for (act in actualList) {
                val sim = getSimilarity(exp, act, matrix)
                if (sim > bestSim) {
                    bestSim = sim
                    bestMatch = act
                }
            }
            // Lower threshold: 0.85 is close enough for PERFECT
            val status = when {
                bestSim >= 0.85 -> MatchStatus.PERFECT
                bestSim >= 0.3 -> MatchStatus.CLOSE
                else -> MatchStatus.MISSED
            }
            matchWeight += when (status) {
                MatchStatus.PERFECT -> 1.0
                MatchStatus.CLOSE -> 0.6
                MatchStatus.MISSED -> 0.0
            }
            alignment.add(PhonemeMatch(exp, bestMatch, status))
        }
        
        val score = if (totalWeight > 0) (matchWeight / totalWeight * 100).toInt() else 0
        
        val normExpected = expectedList.joinToString("")
            .replace(Regex("[ˈˌ]"), "")
        val normActual = actualList.joinToString("")
            .replace(Regex("[ˈˌ]"), "")
        
        return ScoringResult(
            score = score.coerceIn(0, 100),
            normalizedExpected = normExpected,
            normalizedActual = normActual,
            alignment = alignment
        )
    }

    fun generateLetterFeedback(text: String, expectedIpa: String, phonemeAlignment: List<PhonemeMatch>, language: String = "en"): List<LetterFeedbackInfo> {
        val normalizedText = text.lowercase()
        val expectedPhonemes = getNormalizedPhoneList(expectedIpa)

        val n = normalizedText.length
        val m = expectedPhonemes.size

        if (m == 0) {
            return text.map { LetterFeedbackInfo(it.toString(), MatchStatus.PERFECT) }
        }

        // DP for Text to Phoneme alignment
        val dp = Array(n + 1) { IntArray(m + 1) }
        for (i in 0..n) dp[i][0] = i * 10
        for (j in 0..m) dp[0][j] = j * 10

        for (i in 1..n) {
            for (j in 1..m) {
                val cost = getG2PCost(normalizedText[i-1], expectedPhonemes[j-1], language)
                dp[i][j] = minOf(
                    dp[i-1][j] + 10, // Skip char
                    dp[i][j-1] + 10, // Skip phoneme
                    dp[i-1][j-1] + cost // Match
                )
            }
        }

        // Backtrack
        val charToPhonemeIndex = mutableMapOf<Int, Int>()
        var i = n
        var j = m
        while (i > 0 && j > 0) {
            val cost = getG2PCost(normalizedText[i-1], expectedPhonemes[j-1], language)
            if (dp[i][j] == dp[i-1][j-1] + cost) {
                charToPhonemeIndex[i-1] = j-1
                i--; j--
            } else if (dp[i][j] == dp[i-1][j] + 10) {
                i--
            } else {
                j--
            }
        }

        // Map status from phonemeAlignment (which contains EXPECTED phonemes)
        val expectedStatus = phonemeAlignment.filter { it.expected != "-" }.map { it.status }
        
        val result = mutableListOf<LetterFeedbackInfo>()
        val nonPronounceable = setOf('.', ',', '!', '?', ';', ':', '(', ')', '-', ' ', '\'', '"')
        
        for (idx in 0 until text.length) {
            val char = text[idx]
            if (char in nonPronounceable) {
                result.add(LetterFeedbackInfo(char.toString(), MatchStatus.PERFECT))
                continue
            }
            
            val phonemeIdx = charToPhonemeIndex[idx]
            val status = if (phonemeIdx != null && phonemeIdx < expectedStatus.size) {
                expectedStatus[phonemeIdx]
            } else {
                // If a letter didn't map to a phoneme (backtrack skipped it), 
                // look for neighbors
                val prevStatus = charToPhonemeIndex[idx - 1]?.let { if (it < expectedStatus.size) expectedStatus[it] else null }
                val nextStatus = charToPhonemeIndex[idx + 1]?.let { if (it < expectedStatus.size) expectedStatus[it] else null }
                prevStatus ?: nextStatus ?: MatchStatus.PERFECT
            }
            result.add(LetterFeedbackInfo(char.toString(), status))
        }
        
        return result
    }

    private fun getG2PCost(char: Char, phoneme: String, language: String = "en"): Int {
        val p = phoneme.lowercase()
        val c = char.lowercaseChar()

        // Exact match
        if (p.startsWith(c)) return 0

        // Vowels
        val vowels = setOf('a', 'e', 'i', 'o', 'u')
        val ipaVowels = setOf('æ', 'ɑ', 'ɔ', 'ə', 'ɛ', 'ɪ', 'ʊ', 'ʌ', 'i', 'u', 'e', 'o', 'a',
            'y', 'ø', 'œ') // French rounded vowels
        if (c in vowels && p.any { it in ipaVowels }) return 2

        // Consonants — base English G2P
        val g2pSim = mutableMapOf(
            'c' to setOf("k", "s", "t͡ʃ"),
            'g' to setOf("ɡ", "g", "d͡ʒ", "j"),
            'j' to setOf("d͡ʒ", "j"),
            'y' to setOf("j", "i", "ɪ"),
            'x' to setOf("z", "k", "s"),
            'q' to setOf("k"),
            'w' to setOf("w", "u", "ʊ"),
            'h' to setOf("h", "ɦ"),
            'm' to setOf("m", "ɱ"),
            'n' to setOf("n", "ŋ", "ɲ"),
            'r' to setOf("r", "ɹ", "ɻ", "l"),
            's' to setOf("s", "z", "ʃ"),
            't' to setOf("t", "θ", "ð"),
            'd' to setOf("d", "ð"),
            'f' to setOf("f", "v"),
            'v' to setOf("v", "f")
        )

        if (language == "fr") {
            // French-specific overrides and additions
            // 'u' in French → /y/ (front rounded), not /u/
            g2pSim['u'] = setOf("y", "u", "ʊ", "ə")
            // 'r' in French → /ʁ/ (uvular)
            g2pSim['r'] = setOf("ʁ", "ʀ", "r", "ɹ")
            // 'e' → many allophones
            g2pSim['e'] = setOf("e", "ɛ", "ø", "œ", "ə")
            // 'o' → /o/ or /ɔ/
            g2pSim['o'] = setOf("o", "ɔ", "ø")
            // 'j' in French → /ʒ/ (je, jour)
            g2pSim['j'] = setOf("ʒ", "j")
            // 'g' → /ʒ/ before e/i, /ɡ/ elsewhere
            g2pSim['g'] = setOf("ɡ", "g", "ʒ")
            // 'n' often part of nasal vowel digraph
            g2pSim['n'] = setOf("n", "ŋ", "ɲ", "ɑ̃", "ɛ̃", "ɔ̃", "œ̃")
            // 'ç' → /s/
            g2pSim['ç'] = setOf("s")
            // 'é', 'è', 'ê', 'ë' → front vowel phonemes
            g2pSim['é'] = setOf("e", "ɛ")
            g2pSim['è'] = setOf("ɛ", "e")
            g2pSim['ê'] = setOf("ɛ", "e")
            g2pSim['à'] = setOf("a", "ɑ")
            g2pSim['â'] = setOf("ɑ", "a")
            g2pSim['î'] = setOf("i")
            g2pSim['ï'] = setOf("i")
            g2pSim['ô'] = setOf("o", "ɔ")
            g2pSim['û'] = setOf("y", "u")
            g2pSim['ù'] = setOf("y", "u")
        }

        if (g2pSim[c]?.contains(p) == true) return 2

        return 8 // High cost for non-matching
    }

    private fun getNormalizedPhoneList(ipa: String): List<String> {
        // Reference IPA comes from espeak-ng (has stress marks, allophones like
        // ɐ/ɚ/ᵻ, and ZWJs in diphthongs). Runtime IPA comes from wav2vec2 —
        // same phoneme inventory but usually no stress and less consistent
        // allophone choice. Fold near-identical allophones so a correct
        // speaker can actually hit a high score.
        val folded = ipa
            .replace('ɐ', 'ə')
            .replace('ᵻ', 'ɪ')
            .replace("ɚ", "ər")
            .replace("\u200D", "")
            .replace("\u00A0", " ")

        val wordGroups = folded.lowercase().split(" ").filter { it.isNotBlank() }
        val result = mutableListOf<String>()

        for (word in wordGroups) {
            val cleaned = word
                .replace(Regex("[ˈˌ.?!()\\-]"), "")
                .replace(Regex("[\u0300-\u0302\u0304-\u0360\u0362-\u036F]"), "")
                .replace("\u00A0", "")
            
            if (cleaned.isEmpty()) continue
            
            var i = 0
            while (i < cleaned.length) {
                val c = cleaned[i]
                if (i + 1 < cleaned.length && cleaned[i + 1] == '\u0303') {
                    result.add(cleaned.substring(i, i + 2))
                    i += 2
                } else if (i + 2 < cleaned.length && cleaned[i + 1] == '\u0361') {
                    result.add(cleaned.substring(i, i + 3))
                    i += 3
                } else if (i + 1 < cleaned.length && isTiedPair(cleaned[i], cleaned[i+1])) {
                    result.add(cleaned.substring(i, i + 2))
                    i += 2
                } else if (c == 'g') {
                    result.add("ɡ")
                    i++
                } else {
                    result.add(c.toString())
                    i++
                }
            }
        }
        
        return result
    }

    private fun isTiedPair(c1: Char, c2: Char): Boolean {
        val s = "$c1$c2"
        return s == "tʃ" || s == "dʒ" || s == "ts" || s == "dz"
    }

    private val knownPhones = setOf(
        "a", "b", "d", "e", "f", "h", "i", "j", "k", "l", "m", "n", "o", "p", 
        "s", "t", "u", "v", "w", "z", "æ", "ð", "ŋ", "ɑ", "ɔ", "ə", "ɛ", "ɡ", 
        "ɪ", "ɹ", "ɹ̩", "ʃ", "ʊ", "ʌ", "ʒ", "θ", "t͡ʃ", "d͡ʒ", "tʃ", "dʒ"
    )

    private fun getSimilarity(p1: String, p2: String, matrix: Map<Set<String>, Double>): Double {
        if (p1 == p2) return 1.0
        matrix[setOf(p1, p2)]?.let { return it }
        // No fallback - only explicit mappings count
        // This prevents random matches from scoring high
        return 0.0
    }

    private fun align(expected: List<String>, actual: List<String>, matrix: Map<Set<String>, Double>): List<PhonemeMatch> {
        val n = expected.size
        val m = actual.size
        val dp = Array(n + 1) { DoubleArray(m + 1) }
        
        for (i in 0..n) dp[i][0] = i.toDouble()
        for (j in 0..m) dp[0][j] = j.toDouble()
        
        for (i in 1..n) {
            for (j in 1..m) {
                val sim = getSimilarity(expected[i - 1], actual[j - 1], matrix)
                val cost = 1.0 - sim
                // Gap cost = 0: insertions/deletions don't penalize at all
                // This handles epenthetic schwas, extra sounds, etc without score loss
                dp[i][j] = minOf(
                    dp[i - 1][j] + 0.0,    // Deletion
                    dp[i][j - 1] + 0.0,    // Insertion
                    dp[i - 1][j - 1] + cost   // Substitution
                )
            }
        }
        
        val result = mutableListOf<PhonemeMatch>()
        var i = n
        var j = m
        while (i > 0 || j > 0) {
            if (i > 0 && j > 0) {
                val sim = getSimilarity(expected[i - 1], actual[j - 1], matrix)
                val cost = 1.0 - sim
                if (dp[i][j] == dp[i - 1][j - 1] + cost) {
                    val status = when {
                        sim >= 1.0 -> MatchStatus.PERFECT
                        sim >= 0.3 -> MatchStatus.CLOSE  // Lowered from 0.5 for more partial credit
                        else -> MatchStatus.MISSED
                    }
                    result.add(0, PhonemeMatch(expected[i - 1], actual[j - 1], status))
                    i--; j--
                    continue
                }
            }
            if (i > 0 && (j == 0 || dp[i][j] == dp[i - 1][j])) {
                result.add(0, PhonemeMatch(expected[i - 1], "-", MatchStatus.MISSED))
                i--
            } else {
                j--
            }
        }
        return result
    }

    fun normalize(ipa: String): String {
        return getNormalizedPhoneList(ipa).joinToString("")
    }
}
