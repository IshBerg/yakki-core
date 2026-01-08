/**
 * File Path: yakki-core/src/commonMain/kotlin/com/yakki/core/logic/SniperReducer.kt
 * File Name: SniperReducer.kt
 * Created: 2026-01-08
 * Version: 1.0.0
 *
 * Description: Pure functional reducer for Sniper game state.
 * MVI pattern: (State, Event) -> State
 *
 * Implements "Soft Train, Hard Rank" philosophy:
 * - Timer reaching 0 does NOT stop the game
 * - Player can always complete all questions
 * - Rank is calculated at the end based on time and accuracy
 */

package com.yakki.core.logic

import com.yakki.core.models.SniperConfig
import com.yakki.core.models.SniperEvent
import com.yakki.core.models.SniperGameState
import com.yakki.core.models.SniperRank

/**
 * SniperReducer - Pure function that transforms state based on events.
 */
object SniperReducer {

    /**
     * Process event and return new state.
     * Pure function - no side effects.
     */
    fun reduce(state: SniperGameState, event: SniperEvent): SniperGameState {
        return when (event) {
            is SniperEvent.StartBatch -> handleStartBatch(event)
            is SniperEvent.SubmitAnswer -> handleSubmitAnswer(state, event)
            is SniperEvent.NextQuestion -> handleNextQuestion(state)
            is SniperEvent.TimerTick -> handleTimerTick(state)
            is SniperEvent.TimeExpired -> handleTimeExpired(state)
            is SniperEvent.FinishGame -> handleFinishGame(state)
            is SniperEvent.Reset -> SniperGameState()
        }
    }

    private fun handleStartBatch(event: SniperEvent.StartBatch): SniperGameState {
        return SniperGameState(
            currentBatch = event.rounds,
            currentIndex = 0,
            timeLeftPercent = 1.0f,
            isOvertime = false,
            coverIntegrity = 100,
            isGameFinished = false,
            totalCorrect = 0,
            totalWrong = 0,
            xpAwarded = 0L
        )
    }

    private fun handleSubmitAnswer(
        state: SniperGameState,
        event: SniperEvent.SubmitAnswer
    ): SniperGameState {
        return if (event.isCorrect) {
            state.copy(
                totalCorrect = state.totalCorrect + 1
            )
        } else {
            val newCover = (state.coverIntegrity - SniperConfig.COVER_PENALTY_PER_ERROR)
                .coerceAtLeast(0)
            state.copy(
                totalWrong = state.totalWrong + 1,
                coverIntegrity = newCover
            )
        }
    }

    private fun handleNextQuestion(state: SniperGameState): SniperGameState {
        val nextIndex = state.currentIndex + 1

        return if (nextIndex < state.currentBatch.size) {
            state.copy(currentIndex = nextIndex)
        } else {
            handleFinishGame(state)
        }
    }

    private fun handleTimerTick(state: SniperGameState): SniperGameState {
        // Timer decreases by tick amount
        val tickDecrement = SniperConfig.TIMER_TICK_MS.toFloat() /
                (SniperConfig.MISSION_TIME_SECONDS * 1000f)

        val newTimeLeft = (state.timeLeftPercent - tickDecrement).coerceAtLeast(0f)
        val nowOvertime = newTimeLeft <= 0f

        return state.copy(
            timeLeftPercent = newTimeLeft,
            isOvertime = nowOvertime || state.isOvertime
        )
    }

    private fun handleTimeExpired(state: SniperGameState): SniperGameState {
        return state.copy(
            isOvertime = true,
            timeLeftPercent = 0f
        )
    }

    private fun handleFinishGame(state: SniperGameState): SniperGameState {
        val rank = SniperRank.fromGameState(state)
        val xp = calculateXP(state, rank)

        return state.copy(
            isGameFinished = true,
            xpAwarded = xp
        )
    }

    private fun calculateXP(state: SniperGameState, rank: SniperRank): Long {
        // Base XP: 10 per correct answer
        val baseXp = state.totalCorrect * 10L

        // Cover integrity bonus (0-50%)
        val coverBonus = (state.coverIntegrity / 200.0) // 0.0 - 0.5

        // Apply rank multiplier
        val total = baseXp * (1.0 + coverBonus) * rank.xpMultiplier

        return total.toLong()
    }
}

/**
 * ThreatEngine - Calculates threat scores for pattern prioritization.
 *
 * Uses a weighted formula to determine which patterns need practice:
 * - Recent mistakes are weighted higher
 * - Patterns with low accuracy are prioritized
 * - Novel patterns get a base score for exposure
 */
object ThreatEngine {

    private const val NOVELTY_BASE_SCORE = 50.0
    private const val RECENCY_WEIGHT = 0.3
    private const val FREQUENCY_WEIGHT = 0.3
    private const val ACCURACY_WEIGHT = 0.4

    /**
     * Calculate threat score for a pattern.
     *
     * @param totalAttempts Total times this pattern was attempted
     * @param mistakeCount Number of mistakes made
     * @param lastMistakeMs Timestamp of last mistake (null if never)
     * @param nowMs Current timestamp
     * @return Threat score (0-100)
     */
    fun calculateThreatScore(
        totalAttempts: Int,
        mistakeCount: Int,
        lastMistakeMs: Long?,
        nowMs: Long
    ): Double {
        // Novelty: never seen before
        if (totalAttempts == 0) {
            return NOVELTY_BASE_SCORE
        }

        // Accuracy factor (0-100, inverted: low accuracy = high threat)
        val accuracy = if (totalAttempts > 0) {
            (totalAttempts - mistakeCount).toDouble() / totalAttempts
        } else 0.0
        val accuracyScore = (1.0 - accuracy) * 100.0

        // Recency factor (recent mistakes = higher threat)
        val recencyScore = if (lastMistakeMs != null) {
            val daysSinceMistake = (nowMs - lastMistakeMs) / (24 * 60 * 60 * 1000.0)
            // Decay: score decreases by half every 7 days
            100.0 * Math.pow(0.5, daysSinceMistake / 7.0)
        } else {
            0.0
        }

        // Frequency factor (more attempts = more data = more reliable score)
        val frequencyScore = minOf(totalAttempts * 5.0, 100.0)

        // Weighted combination
        return (accuracyScore * ACCURACY_WEIGHT +
                recencyScore * RECENCY_WEIGHT +
                frequencyScore * FREQUENCY_WEIGHT).coerceIn(0.0, 100.0)
    }

    /**
     * Sort patterns by threat score (highest first).
     */
    fun prioritizePatterns(
        patterns: List<Pair<String, Double>>
    ): List<String> {
        return patterns
            .sortedByDescending { it.second }
            .map { it.first }
    }
}
