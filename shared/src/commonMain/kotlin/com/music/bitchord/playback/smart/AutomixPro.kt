package com.music.bitchord.playback.smart

/**
 * Automix Pro: the user-facing transition mode.
 *
 * This is the single selector the settings UI shows (Off / Crossfade /
 * Gapless / Automix Pro). It replaces the old pair of controls
 * (`crossfade_seconds` 0–12 s + the `smart_fade_enabled` toggle) whose
 * combination was ambiguous — e.g. "crossfade 0 s" silently meant gapless.
 *
 * Mapping to the planner's [CrossfadeMode]:
 * - OFF, CROSSFADE, GAPLESS → [CrossfadeMode.STANDARD]
 *   (OFF = no overlap at all; GAPLESS = near-instant handoff)
 * - AUTOMIX_PRO → [CrossfadeMode.SMART]
 */
enum class TransitionMode {
    OFF,
    CROSSFADE,
    GAPLESS,
    AUTOMIX_PRO,
}

/**
 * Migrates the legacy settings pair to a [TransitionMode].
 *
 * Preserves behavior exactly:
 * - Automix on (old `smart_fade_enabled`) → AUTOMIX_PRO, whatever the
 *   crossfade seconds were (the old code ignored them when smart fade ran,
 *   using them only as an analysis fallback window).
 * - Otherwise a positive crossfade duration → CROSSFADE.
 * - Otherwise (0 s) → GAPLESS, which is what 0 s effectively did.
 */
fun migrateLegacyTransitionMode(smartFadeEnabled: Boolean, crossfadeSeconds: Int): TransitionMode =
    when {
        smartFadeEnabled -> TransitionMode.AUTOMIX_PRO
        crossfadeSeconds > 0 -> TransitionMode.CROSSFADE
        else -> TransitionMode.GAPLESS
    }

/**
 * How ambitious Automix Pro may be with the overlap length.
 *
 * Applied as a cap on the planner's chosen overlap (`maxFadeSeconds`):
 * the analysis still decides the technique and the tier, the style only
 * bounds how long a blend may run.
 */
enum class AutomixStyle(
    /** Shortest overlap this style allows, seconds. */
    val minOverlapSeconds: Double,
    /** Longest overlap this style allows, seconds. */
    val maxOverlapSeconds: Double,
) {
    /** Subtle: short overlaps, ≈ 4–6 s. */
    SUBTLE(4.0, 6.0),

    /** Balanced: the default, ≈ 8–12 s. */
    BALANCED(8.0, 12.0),

    /**
     * DJ: long blends up to ≈ 30 s. Only reaches the top end when the pair
     * earns it (steady pulse, high beat confidence); the planner still
     * degrades to shorter overlaps on uncertain material.
     */
    DJ(16.0, 30.0),
}

/**
 * How many of the most recent transitions the anti-repetition penalty looks
 * back at before it stops caring that a technique repeats.
 */
const val ANTI_REPEAT_WINDOW = 4

/** Pitch class of each root name the analyzer emits (sharps and flats as written). */
private val ROOT_PITCH_CLASS = mapOf(
    "C" to 0, "C♯" to 1, "D♭" to 1, "D" to 2, "E♭" to 3, "D♯" to 3,
    "E" to 4, "F" to 5, "F♯" to 6, "G♭" to 6, "G" to 7, "A♭" to 8,
    "G♯" to 8, "A" to 9, "B♭" to 10, "A♯" to 10, "B" to 11, "C♭" to 11,
)

/** Camelot number (1–12) for a pitch class, major side (B). C major = 8B. */
private val MAJOR_CAMELOT = intArrayOf(8, 3, 10, 5, 12, 7, 2, 9, 4, 11, 6, 1)

/** Camelot number (1–12) for a pitch class, minor side (A). A minor = 8A. */
private val MINOR_CAMELOT = intArrayOf(5, 12, 7, 2, 9, 4, 11, 6, 1, 8, 3, 10)

/**
 * Describes the harmonic relationship between two analyzed keys
 * ("C major", "F♯ minor" — the analyzer's format) on the Camelot wheel.
 *
 * Returns "" when either key is unknown or unparseable: uncertainty stays
 * neutral and never blocks or authorizes anything.
 */
fun camelotRelation(keyA: String, keyB: String): String {
    val a = parseCamelot(keyA) ?: return ""
    val b = parseCamelot(keyB) ?: return ""
    val (numA, minorA) = a
    val (numB, minorB) = b
    val labelA = "$numA${if (minorA) "A" else "B"}"
    val labelB = "$numB${if (minorB) "A" else "B"}"
    val relation = when {
        a == b -> "same key"
        numA == numB -> "relative major/minor"
        minorA == minorB && (numB - numA + 12) % 12 == 1 -> "+1, energy boost"
        minorA == minorB && (numA - numB + 12) % 12 == 1 -> "−1, energy drop"
        minorA == minorB && (numB - numA + 12) % 12 == 2 -> "+2, lift"
        minorA == minorB && (numA - numB + 12) % 12 == 2 -> "−2, settle"
        else -> "clash risk"
    }
    return "$labelA→$labelB ($relation)"
}

private fun parseCamelot(key: String): Pair<Int, Boolean>? {
    val parts = key.trim().split(" ")
    if (parts.size != 2) return null
    val pitch = ROOT_PITCH_CLASS[parts[0]] ?: return null
    val minor = when (parts[1].lowercase()) {
        "minor" -> true
        "major" -> false
        else -> return null
    }
    val number = if (minor) MINOR_CAMELOT[pitch] else MAJOR_CAMELOT[pitch]
    return number to minor
}

/**
 * One-line human summary of a [TransitionVerdict] for stats for nerds, e.g.
 * "Nivel 1 · DJ_BLEND · 8.2 s (16 beats) · +2.1 % · 8A→9A (+1, energy boost)".
 */
fun TransitionVerdict.describe(): String {
    val tierLabel = when (tier) {
        TransitionTier.BEATMATCHED -> "Nivel 1"
        TransitionTier.DJ_ASSISTED -> "Nivel 2"
        TransitionTier.PLAIN_CROSSFADE -> "Nivel 3"
    }
    val overlap = buildString {
        append("%.1f s".format(overlapSeconds))
        if (overlapBeats > 0) append(" ($overlapBeats beats)")
    }
    val stretch = if (stretchFraction > 0.0005) {
        " · +%.1f %%".format(stretchFraction * 100)
    } else {
        ""
    }
    val key = if (keyRelation.isNotBlank()) " · $keyRelation" else ""
    val why = if (reason.isNotBlank()) " · $reason" else ""
    return "$tierLabel · $technique · $overlap$stretch$key$why"
}

/**
 * The planner's verdict on one transition, published for "stats for nerds"
 * and the transition log. Every field is decided from stored analysis —
 * nothing here is measured off live PCM.
 */
data class TransitionVerdict(
    /** Which confidence rung the pair landed on (Nivel 1/2/3). */
    val tier: TransitionTier = TransitionTier.PLAIN_CROSSFADE,
    /** The rendering technique chosen, e.g. DJ_BLEND, DJ_FILTER/SWEEP, EQUAL_POWER, GAPLESS. */
    val technique: String = "",
    /** Overlap length actually planned, in seconds. */
    val overlapSeconds: Double = 0.0,
    /** Overlap length in beats of the outgoing grid, 0 when not beat-matched. */
    val overlapBeats: Int = 0,
    /** Tempo stretch applied to the incoming track, as a fraction (0.04 = 4 %). */
    val stretchFraction: Double = 0.0,
    /** Camelot/key relationship, e.g. "8A→9A (+1, compatible)" or "" when unknown. */
    val keyRelation: String = "",
    /** First entry is the most disqualifying reason when the tier degraded. */
    val reason: String = "",
)
