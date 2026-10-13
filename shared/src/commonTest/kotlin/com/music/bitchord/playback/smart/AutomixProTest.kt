package com.music.bitchord.playback.smart

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class AutomixProTest {

    /** A synthetic, fully analysed track on a steady grid. */
    private fun track(
        id: String,
        bpm: Double,
        key: String,
        duration: Double = 200.0,
    ): TrackAnalysis {
        val beat = 60.0 / bpm
        val downbeats = generateSequence(0.5) { it + 4 * beat }.takeWhile { it < duration - 1 }.toList()
        val energy = (0 until (duration * 2).toInt()).map { EnergySample(it / 2.0, 1.0) }
        return TrackAnalysis(
            status = TrackAnalysis.STATUS_READY,
            trackId = id,
            duration = duration,
            bpm = bpm,
            beatInterval = beat,
            beatConfidence = 0.9,
            downbeats = downbeats,
            phraseBoundaries = downbeats.filterIndexed { index, _ -> index % 8 == 0 },
            firstBeat = 0.5,
            key = key,
            keyConfidence = 0.9,
            audibleStartTime = 0.5,
            contentEndTime = duration - 2,
            mixInTime = downbeats[4],
            energyCurve = energy,
        )
    }

    private fun info(t: TrackAnalysis) = TransitionTrackInfo(t.trackId, (t.duration * 1000).toLong())

    // ---- Legacy migration --------------------------------------------------

    @Test
    fun legacyAutomixOnMigratesToAutomixPro() {
        assertEquals(
            TransitionMode.AUTOMIX_PRO,
            migrateLegacyTransitionMode(smartFadeEnabled = true, crossfadeSeconds = 0),
        )
        // The old Automix ignored the slider; the migration must too.
        assertEquals(
            TransitionMode.AUTOMIX_PRO,
            migrateLegacyTransitionMode(smartFadeEnabled = true, crossfadeSeconds = 8),
        )
    }

    @Test
    fun legacyCrossfadeSecondsMigratesToCrossfade() {
        assertEquals(
            TransitionMode.CROSSFADE,
            migrateLegacyTransitionMode(smartFadeEnabled = false, crossfadeSeconds = 6),
        )
    }

    @Test
    fun legacyZeroCrossfadeMigratesToGapless() {
        assertEquals(
            TransitionMode.GAPLESS,
            migrateLegacyTransitionMode(smartFadeEnabled = false, crossfadeSeconds = 0),
        )
    }

    // ---- Style caps ----------------------------------------------------------

    private fun planWithStyle(a: TrackAnalysis, b: TrackAnalysis, style: AutomixStyle): TransitionPlan =
        planTransition(
            analysis = a,
            nextAnalysis = b,
            currentTrack = info(a),
            nextTrack = info(b),
            currentTime = 10.0,
            duration = a.duration,
            mode = CrossfadeMode.SMART,
            advanced = true,
            style = style,
        )

    @Test
    fun subtleStyleCapsOverlapAtSixSeconds() {
        val a = track("a", 120.0, "C major")
        val b = track("b", 120.0, "G major")
        val plan = planWithStyle(a, b, AutomixStyle.SUBTLE)
        assertTrue(
            plan.fadeSeconds <= AutomixStyle.SUBTLE.maxOverlapSeconds + 0.001,
            "SUBTLE overlap ${plan.fadeSeconds}s exceeds 6s cap",
        )
    }

    @Test
    fun balancedStyleCapsOverlapAtTwelveSeconds() {
        val a = track("a", 120.0, "C major")
        val b = track("b", 120.0, "G major")
        val plan = planWithStyle(a, b, AutomixStyle.BALANCED)
        assertTrue(
            plan.fadeSeconds <= AutomixStyle.BALANCED.maxOverlapSeconds + 0.001,
            "BALANCED overlap ${plan.fadeSeconds}s exceeds 12s cap",
        )
    }

    @Test
    fun djStyleAllowsLongerOverlapThanBalanced() {
        val a = track("a", 128.0, "A minor", duration = 400.0)
        val b = track("b", 128.0, "A minor", duration = 400.0)
        val balanced = planWithStyle(a, b, AutomixStyle.BALANCED)
        val dj = planWithStyle(a, b, AutomixStyle.DJ)
        assertTrue(
            dj.fadeSeconds >= balanced.fadeSeconds,
            "DJ (${dj.fadeSeconds}s) should allow at least BALANCED's overlap (${balanced.fadeSeconds}s)",
        )
        assertTrue(
            dj.fadeSeconds <= AutomixStyle.DJ.maxOverlapSeconds + 0.001,
            "DJ overlap ${dj.fadeSeconds}s exceeds 30s cap",
        )
    }

    // ---- Anti-repetition -------------------------------------------------------

    @Test
    fun filterVariantRotatesWhenRecentRepeats() {
        val a = track("a", 100.0, "C major")
        val b = track("b", 140.0, "F major")
        // Tempi far apart → DJ_FILTER path, where the variant matters.
        val first = planTransition(
            analysis = a,
            nextAnalysis = b,
            currentTrack = info(a),
            nextTrack = info(b),
            currentTime = 10.0,
            duration = a.duration,
            mode = CrossfadeMode.SMART,
            advanced = true,
        )
        assertEquals(TransitionStyle.DJ_FILTER, first.transitionStyle)
        val repeated = first.filterVariant
        val second = planTransition(
            analysis = a,
            nextAnalysis = b,
            currentTrack = info(a),
            nextTrack = info(b),
            currentTime = 10.0,
            duration = a.duration,
            mode = CrossfadeMode.SMART,
            advanced = true,
            recentFilterVariants = listOf(repeated),
        )
        assertTrue(
            second.filterVariant != repeated,
            "variant $repeated repeated despite anti-repetition history",
        )
    }

    @Test
    fun filterVariantStableWithoutHistory() {
        val a = track("a", 100.0, "C major")
        val b = track("b", 140.0, "F major")
        fun variant() = planTransition(
            analysis = a,
            nextAnalysis = b,
            currentTrack = info(a),
            nextTrack = info(b),
            currentTime = 10.0,
            duration = a.duration,
            mode = CrossfadeMode.SMART,
            advanced = true,
        ).filterVariant
        assertEquals(variant(), variant(), "deterministic pick must not drift without history")
    }

    // ---- Camelot ---------------------------------------------------------------

    @Test
    fun camelotSameKey() {
        assertEquals("8A→8A (same key)", camelotRelation("A minor", "A minor"))
    }

    @Test
    fun camelotRelativeMajorMinor() {
        assertEquals("8A→8B (relative major/minor)", camelotRelation("A minor", "C major"))
    }

    @Test
    fun camelotEnergyBoost() {
        // A minor (8A) → E minor (9A): +1
        assertEquals("8A→9A (+1, energy boost)", camelotRelation("A minor", "E minor"))
    }

    @Test
    fun camelotClash() {
        // C major (8B) → F♯ major (2B): clash
        assertEquals("8B→2B (clash risk)", camelotRelation("C major", "F♯ major"))
    }

    @Test
    fun camelotUnknownStaysNeutral() {
        assertEquals("", camelotRelation("", "C major"))
        assertEquals("", camelotRelation("C major", "nonsense"))
        assertEquals("", camelotRelation("", ""))
    }

    // ---- Harmonic selection ------------------------------------------------------

    private fun planWithKeys(keyA: String, keyB: String): TransitionPlan {
        val a = track("a", 120.0, keyA)
        val b = track("b", 120.0, keyB)
        return planTransition(
            analysis = a,
            nextAnalysis = b,
            currentTrack = info(a),
            nextTrack = info(b),
            currentTime = 10.0,
            duration = a.duration,
            mode = CrossfadeMode.SMART,
            advanced = true,
        )
    }

    @Test
    fun keyClashNeverOpenBlends() {
        // Same tempo, high confidence: without the clash this is DJ_BLEND.
        // C major (8B) vs F# major (2B) is a clash: must be masked, not blended.
        val plan = planWithKeys("C major", "F♯ major")
        assertEquals(
            TransitionStyle.DJ_FILTER, plan.transitionStyle,
            "key clash must not open-blend",
        )
        assertTrue(
            plan.filterSweep > 0.0,
            "key clash must ride the closing low-pass",
        )
    }

    @Test
    fun compatibleKeysStillBlend() {
        // C major (8B) -> G major (9B): energy boost, compatible -> DJ_BLEND.
        val plan = planWithKeys("C major", "G major")
        assertEquals(TransitionStyle.DJ_BLEND, plan.transitionStyle)
    }

    @Test
    fun unknownKeyStaysNeutral() {
        val a = track("a", 120.0, "")
        val b = track("b", 120.0, "")
        val plan = planTransition(
            analysis = a,
            nextAnalysis = b,
            currentTrack = info(a),
            nextTrack = info(b),
            currentTime = 10.0,
            duration = a.duration,
            mode = CrossfadeMode.SMART,
            advanced = true,
        )
        // No key information: the old behavior stands, nothing forced.
        assertEquals(TransitionStyle.DJ_BLEND, plan.transitionStyle)
    }

    @Test
    fun harmonicRelationStructured() {
        assertEquals(HarmonicRelation.SAME, harmonicRelationOf("A minor", "A minor"))
        assertEquals(HarmonicRelation.RELATIVE, harmonicRelationOf("A minor", "C major"))
        assertEquals(HarmonicRelation.ENERGY_UP, harmonicRelationOf("A minor", "E minor"))
        assertEquals(HarmonicRelation.ENERGY_DOWN, harmonicRelationOf("E minor", "A minor"))
        assertEquals(HarmonicRelation.CLASH, harmonicRelationOf("C major", "F♯ major"))
        assertEquals(HarmonicRelation.UNKNOWN, harmonicRelationOf("", "C major"))
        assertEquals(HarmonicRelation.UNKNOWN, harmonicRelationOf("C major", "nonsense"))
    }

    // ---- Energy matching ---------------------------------------------------------

    private fun trackWithEnergy(
        id: String,
        bpm: Double,
        key: String,
        energy: (Double) -> Double,
        duration: Double = 200.0,
    ): TrackAnalysis {
        val beat = 60.0 / bpm
        val downbeats = generateSequence(0.5) { it + 4 * beat }.takeWhile { it < duration - 1 }.toList()
        val samples = (0 until (duration * 2).toInt()).map { i ->
            val t = i / 2.0
            EnergySample(t, energy(t))
        }
        return TrackAnalysis(
            status = TrackAnalysis.STATUS_READY,
            trackId = id,
            duration = duration,
            bpm = bpm,
            beatInterval = beat,
            beatConfidence = 0.9,
            downbeats = downbeats,
            phraseBoundaries = downbeats.filterIndexed { index, _ -> index % 8 == 0 },
            firstBeat = 0.5,
            key = key,
            keyConfidence = 0.9,
            audibleStartTime = 0.5,
            contentEndTime = duration - 2,
            mixInTime = downbeats[4],
            energyCurve = samples,
        )
    }

    @Test
    fun energyDropShortensOverlap() {
        // Clashing keys (C major -> F# major) so the phrase-switch path is
        // skipped and both plans run through the same mixPlan branch; the
        // only difference is the incoming track's intro energy.
        fun planFor(introEnergy: Double): TransitionPlan {
            val a = trackWithEnergy("a", 120.0, "C major", energy = { 1.0 }, duration = 400.0)
            val b = trackWithEnergy("b", 120.0, "F♯ major", energy = { introEnergy }, duration = 400.0)
            return planTransition(
                analysis = a,
                nextAnalysis = b,
                currentTrack = info(a),
                nextTrack = info(b),
                currentTime = 10.0,
                duration = a.duration,
                mode = CrossfadeMode.SMART,
                advanced = true,
                style = AutomixStyle.DJ,
            )
        }
        val matched = planFor(1.0)
        val dropped = planFor(0.1)
        assertTrue(
            dropped.fadeSeconds <= 6.0 + 0.001,
            "energy drop should cap overlap at 6s, was ${dropped.fadeSeconds}s",
        )
        assertTrue(
            dropped.fadeSeconds < matched.fadeSeconds,
            "energy drop (${dropped.fadeSeconds}s) should shorten the blend " +
                "vs matched energy (${matched.fadeSeconds}s)",
        )
    }

    // ---- Zehren cue rules ------------------------------------------------------------

    private fun trackWithCues(
        energy: (Double) -> Double,
        vararg cueTimes: Double,
        duration: Double = 120.0,
    ): TrackAnalysis {
        val bpm = 120.0
        val beat = 0.5
        val downbeats = generateSequence(0.5) { it + 4 * beat }.takeWhile { it < duration - 1 }.toList()
        val samples = (0 until (duration * 2).toInt()).map { i ->
            val t = i / 2.0
            EnergySample(t, energy(t))
        }
        return TrackAnalysis(
            status = TrackAnalysis.STATUS_READY,
            trackId = "cue",
            duration = duration,
            bpm = bpm,
            beatInterval = beat,
            beatConfidence = 0.9,
            downbeats = downbeats,
            phraseBoundaries = downbeats.filterIndexed { index, _ -> index % 8 == 0 },
            firstBeat = 0.5,
            key = "C major",
            keyConfidence = 0.9,
            audibleStartTime = 0.5,
            contentEndTime = duration - 2,
            mixInTime = downbeats[4],
            mixInCandidates = cueTimes.map { MixCandidate(it, 0.5, "intro_drop") },
            energyCurve = samples,
        )
    }

    @Test
    fun phraseEntryWithNoveltyWins() {
        // Candidate A: phrase boundary + downbeat, track arrives (0.2 -> 1.0),
        // strong section follows. Candidate B: plain downbeat, flat energy.
        // Same base score and type: the musical rules must decide.
        val analysis = trackWithCues(
            energy = { t -> if (t < 16.5) 0.2 else 1.0 },
            16.5, 10.5,
        )
        val ranked = rankMixInCandidates(analysis)
        assertEquals(16.5, ranked.first().time, "phrase entry with novelty should win")
    }

    @Test
    fun weakSectionAfterCueLoses() {
        // Candidate C: phrase boundary, but the section after it collapses
        // (1.0 -> 0.1). Candidate E: plain downbeat with a sustaining section.
        // Salience must outweigh the phrase-boundary bonus.
        val analysis = trackWithCues(
            energy = { t -> if (t in 32.5..48.0) 0.1 else 1.0 },
            32.5, 20.5,
        )
        val ranked = rankMixInCandidates(analysis)
        assertEquals(20.5, ranked.first().time, "sustaining section should beat a weak phrase entry")
    }

    // ---- Verdict -----------------------------------------------------------------

    @Test
    fun verdictDescribesTierTechniqueOverlapStretchAndKey() {
        val v = TransitionVerdict(
            tier = TransitionTier.BEATMATCHED,
            technique = "DJ_BLEND",
            overlapSeconds = 8.2,
            overlapBeats = 16,
            stretchFraction = 0.021,
            keyRelation = "8A→9A (+1, energy boost)",
            reason = "",
        )
        val text = v.describe()
        assertTrue(text.contains("Nivel 1"), text)
        assertTrue(text.contains("DJ_BLEND"), text)
        assertTrue(text.contains("8.2 s (16 beats)"), text)
        assertTrue(text.contains("+2.1 %"), text)
        assertTrue(text.contains("8A→9A"), text)
    }

    @Test
    fun verdictOmitsStretchAndKeyWhenUnknown() {
        val v = TransitionVerdict(
            tier = TransitionTier.PLAIN_CROSSFADE,
            technique = "EQUAL_POWER",
            overlapSeconds = 4.0,
        )
        val text = v.describe()
        assertTrue(text.contains("Nivel 3"), text)
        assertTrue(!text.contains("%"), text)
    }
}
