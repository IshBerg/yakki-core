/**
 * File Path: yakki-core/src/commonMain/kotlin/com/yakki/core/logic/ClozeReducer.kt
 * File Name: ClozeReducer.kt
 * Created: 2026-01-08
 * Version: 1.0.0
 *
 * Description: Pure functional reducer for Cloze game state.
 * MVI pattern: (State, Event) -> State
 *
 * Platform-independent business logic for Cloze Drills.
 */

package com.yakki.core.logic

import com.yakki.core.models.ClozeEvent
import com.yakki.core.models.ClozeGameState
import com.yakki.core.models.ClozeRank

/**
 * ClozeReducer - Pure function that transforms state based on events.
 *
 * Usage:
 * ```kotlin
 * val newState = ClozeReducer.reduce(currentState, ClozeEvent.SubmitAnswer("went"))
 * ```
 */
object ClozeReducer {

    /**
     * Process event and return new state.
     * Pure function - no side effects.
     *
     * @param state Current game state
     * @param event Event to process
     * @return New game state
     */
    fun reduce(state: ClozeGameState, event: ClozeEvent): ClozeGameState {
        return when (event) {
            is ClozeEvent.StartDrill -> handleStartDrill(state, event)
            is ClozeEvent.SubmitAnswer -> handleSubmitAnswer(state, event)
            is ClozeEvent.Skip -> handleSkip(state)
            is ClozeEvent.NextQuestion -> handleNextQuestion(state)
            is ClozeEvent.TimerTick -> handleTimerTick(state)
            is ClozeEvent.TimeExpired -> handleTimeExpired(state)
            is ClozeEvent.FinishGame -> handleFinishGame(state)
            is ClozeEvent.Reset -> ClozeGameState()
        }
    }

    private fun handleStartDrill(state: ClozeGameState, event: ClozeEvent.StartDrill): ClozeGameState {
        return ClozeGameState(
            currentDrill = event.drill,
            currentIndex = 0,
            totalCorrect = 0,
            totalWrong = 0,
            streak = 0,
            bestStreak = 0,
            timeLeftSeconds = 30,
            isOvertime = false,
            isGameFinished = false,
            xpAwarded = 0
        )
    }

    private fun handleSubmitAnswer(state: ClozeGameState, event: ClozeEvent.SubmitAnswer): ClozeGameState {
        val currentQuestion = state.currentQuestion ?: return state

        val isCorrect = event.answer.equals(currentQuestion.correctAnswer, ignoreCase = true)

        return if (isCorrect) {
            val newStreak = state.streak + 1
            state.copy(
                totalCorrect = state.totalCorrect + 1,
                streak = newStreak,
                bestStreak = maxOf(state.bestStreak, newStreak)
            )
        } else {
            state.copy(
                totalWrong = state.totalWrong + 1,
                streak = 0
            )
        }
    }

    private fun handleSkip(state: ClozeGameState): ClozeGameState {
        return state.copy(
            totalWrong = state.totalWrong + 1,
            streak = 0
        )
    }

    private fun handleNextQuestion(state: ClozeGameState): ClozeGameState {
        val drill = state.currentDrill ?: return state
        val nextIndex = state.currentIndex + 1

        return if (nextIndex < drill.questions.size) {
            state.copy(currentIndex = nextIndex)
        } else {
            handleFinishGame(state)
        }
    }

    private fun handleTimerTick(state: ClozeGameState): ClozeGameState {
        val newTime = (state.timeLeftSeconds - 1).coerceAtLeast(0)
        val nowOvertime = newTime <= 0

        return state.copy(
            timeLeftSeconds = newTime,
            isOvertime = nowOvertime || state.isOvertime
        )
    }

    private fun handleTimeExpired(state: ClozeGameState): ClozeGameState {
        return state.copy(
            isOvertime = true,
            timeLeftSeconds = 0
        )
    }

    private fun handleFinishGame(state: ClozeGameState): ClozeGameState {
        val rank = ClozeRank.fromAccuracy(state.accuracy, state.isOvertime)
        val baseXp = state.totalCorrect * 10
        val xpReward = (baseXp * rank.xpMultiplier).toInt()

        return state.copy(
            isGameFinished = true,
            xpAwarded = xpReward
        )
    }
}

/**
 * ClozeValidator - Validates answers for Cloze questions.
 */
object ClozeValidator {

    /**
     * Check if answer is correct.
     *
     * @param userAnswer User's submitted answer
     * @param correctAnswer Expected correct answer
     * @param strictMode If true, case-sensitive comparison
     * @return True if answer is correct
     */
    fun isCorrect(
        userAnswer: String,
        correctAnswer: String,
        strictMode: Boolean = false
    ): Boolean {
        val normalizedUser = userAnswer.trim()
        val normalizedCorrect = correctAnswer.trim()

        return if (strictMode) {
            normalizedUser == normalizedCorrect
        } else {
            normalizedUser.equals(normalizedCorrect, ignoreCase = true)
        }
    }

    /**
     * Check if answer is partially correct (for feedback).
     * Useful for giving hints like "almost correct".
     *
     * @param userAnswer User's submitted answer
     * @param correctAnswer Expected correct answer
     * @return Similarity score (0.0 - 1.0)
     */
    fun getSimilarity(userAnswer: String, correctAnswer: String): Float {
        val user = userAnswer.lowercase().trim()
        val correct = correctAnswer.lowercase().trim()

        if (user == correct) return 1.0f
        if (user.isEmpty() || correct.isEmpty()) return 0.0f

        // Simple Levenshtein-based similarity
        val maxLen = maxOf(user.length, correct.length)
        val distance = levenshteinDistance(user, correct)

        return (1.0f - distance.toFloat() / maxLen).coerceIn(0f, 1f)
    }

    private fun levenshteinDistance(s1: String, s2: String): Int {
        val dp = Array(s1.length + 1) { IntArray(s2.length + 1) }

        for (i in 0..s1.length) dp[i][0] = i
        for (j in 0..s2.length) dp[0][j] = j

        for (i in 1..s1.length) {
            for (j in 1..s2.length) {
                val cost = if (s1[i - 1] == s2[j - 1]) 0 else 1
                dp[i][j] = minOf(
                    dp[i - 1][j] + 1,      // deletion
                    dp[i][j - 1] + 1,      // insertion
                    dp[i - 1][j - 1] + cost // substitution
                )
            }
        }

        return dp[s1.length][s2.length]
    }
}
