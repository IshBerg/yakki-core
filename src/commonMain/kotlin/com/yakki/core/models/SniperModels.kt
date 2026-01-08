/**
 * File Path: yakki-core/src/commonMain/kotlin/com/yakki/core/models/SniperModels.kt
 * File Name: SniperModels.kt
 * Created: 2026-01-08
 * Version: 1.0.0
 *
 * Description: Domain models for Sniper Mode (L1 Interference game).
 * Platform-independent - works on Android, iOS (Flutter), Web.
 *
 * Sniper Mode targets L1 interference patterns - common errors
 * that speakers of specific native languages make in English.
 */

package com.yakki.core.models

import kotlinx.serialization.Serializable

/**
 * Source language enum representing the learner's native language.
 * Different L1 backgrounds produce different interference patterns.
 */
@Serializable
enum class L1Source {
    HEBREW,
    RUSSIAN,
    ARABIC
}

/**
 * Category of the error pattern.
 * Used for interleaving algorithm to ensure variety in practice sessions.
 */
@Serializable
enum class ErrorCategory {
    SYNTAX,
    PREPOSITION,
    LEXICAL,
    TENSE,
    ARTICLE,
    WORD_ORDER
}

/**
 * L1 Trap Model - Static trap data.
 *
 * Represents a single interference pattern that native speakers
 * of a specific language commonly make when learning English.
 *
 * @property id Unique identifier
 * @property l1Source Native language causing the interference
 * @property category Error type for interleaving
 * @property context Semantic context (transport, time, etc.)
 * @property errorPattern Common incorrect usage
 * @property correctPattern Correct English usage
 * @property explanation Brief pedagogical explanation (English)
 * @property explanations Localized explanations map (key: locale code)
 */
@Serializable
data class L1Trap(
    val id: String,
    val l1Source: L1Source,
    val category: ErrorCategory,
    val context: String,
    val errorPattern: String,
    val correctPattern: String,
    val explanation: String,
    val explanations: Map<String, String> = emptyMap()
) {
    /**
     * Get explanation in the specified locale.
     * Falls back to English, then to legacy explanation field.
     */
    fun getExplanation(localeCode: String): String {
        return explanations[localeCode]
            ?: explanations["en"]
            ?: explanation
    }
}

/**
 * Domain model for user pattern statistics.
 * Used in Threat Engine calculations.
 */
@Serializable
data class PatternStats(
    val patternId: String,
    val l1Source: String,
    val category: String,
    val totalAttempts: Int,
    val mistakeCount: Int,
    val consecutiveCorrectStreak: Int,
    val lastMistakeTimestamp: Long?,
    val lastSeenTimestamp: Long
) {
    val accuracy: Float
        get() = if (totalAttempts > 0) {
            (totalAttempts - mistakeCount).toFloat() / totalAttempts
        } else 0f

    val isMastered: Boolean
        get() = consecutiveCorrectStreak >= 3 && accuracy >= 0.8f
}

/**
 * Domain model for combined trap + user stats.
 * Used in Threat Engine calculations.
 */
@Serializable
data class ScoredTrap(
    val trap: L1Trap,
    val stats: PatternStats?,
    val threatScore: Double
) {
    val isNovelty: Boolean
        get() = stats == null || stats.totalAttempts == 0
}

/**
 * Output model for a single Sniper round.
 * Ready for UI consumption.
 */
@Serializable
data class SniperRound(
    val trap: L1Trap,
    val threatScore: Double,
    val isNovelty: Boolean
)

/**
 * Aggregated statistics for Sniper Mode dashboard.
 */
@Serializable
data class SniperStats(
    val totalPatterns: Int,
    val practicedPatterns: Int,
    val masteredPatterns: Int,
    val totalAttempts: Int,
    val totalMistakes: Int
) {
    val overallAccuracy: Double
        get() = if (totalAttempts > 0) {
            (totalAttempts - totalMistakes).toDouble() / totalAttempts
        } else 0.0

    val masteryProgress: Float
        get() = if (totalPatterns > 0) {
            masteredPatterns.toFloat() / totalPatterns
        } else 0f
}

/**
 * Sniper game state (platform-independent).
 */
@Serializable
data class SniperGameState(
    val currentBatch: List<SniperRound> = emptyList(),
    val currentIndex: Int = 0,
    val timeLeftPercent: Float = 1.0f,
    val isOvertime: Boolean = false,
    val coverIntegrity: Int = 100,
    val isGameFinished: Boolean = false,
    val totalCorrect: Int = 0,
    val totalWrong: Int = 0,
    val xpAwarded: Long = 0L
) {
    val currentRound: SniperRound?
        get() = currentBatch.getOrNull(currentIndex)

    val batchProgress: Float
        get() = if (currentBatch.isEmpty()) 0f
        else currentIndex.toFloat() / currentBatch.size

    val hasNextQuestion: Boolean
        get() = currentIndex < currentBatch.size - 1

    val questionsRemaining: Int
        get() = (currentBatch.size - currentIndex).coerceAtLeast(0)

    val isCoverCritical: Boolean
        get() = coverIntegrity <= 25

    val isCoverBlown: Boolean
        get() = coverIntegrity <= 0

    val accuracy: Float
        get() = if (totalCorrect + totalWrong > 0) {
            totalCorrect.toFloat() / (totalCorrect + totalWrong)
        } else 0f
}

/**
 * Rank achieved at the end of Sniper session.
 */
@Serializable
enum class SniperRank(val displayName: String, val emoji: String, val xpMultiplier: Float) {
    GOLD("Ghost Protocol", "\uD83E\uDD47", 2.0f),
    SILVER("Clean Extraction", "\uD83E\uDD48", 1.5f),
    BRONZE("Scraped Through", "\uD83E\uDD49", 1.0f),
    COMPROMISED("Cover Blown", "\uD83D\uDCA5", 0.5f);

    companion object {
        fun fromGameState(state: SniperGameState): SniperRank {
            return when {
                state.coverIntegrity <= 0 -> COMPROMISED
                state.coverIntegrity >= 100 && !state.isOvertime -> GOLD
                state.coverIntegrity >= 50 && !state.isOvertime -> SILVER
                else -> BRONZE
            }
        }
    }
}

/**
 * Sniper game configuration constants.
 */
object SniperConfig {
    const val MISSION_TIME_SECONDS = 45
    const val TIMER_TICK_MS = 100L
    const val COVER_PENALTY_PER_ERROR = 15
    const val BATCH_SIZE = 5
    const val SILVER_THRESHOLD = 50
    const val BRONZE_THRESHOLD = 1
}

/**
 * Sniper game events (for state machine).
 */
sealed class SniperEvent {
    data class StartBatch(val rounds: List<SniperRound>) : SniperEvent()
    data class SubmitAnswer(val isCorrect: Boolean) : SniperEvent()
    data object NextQuestion : SniperEvent()
    data object TimerTick : SniperEvent()
    data object TimeExpired : SniperEvent()
    data object FinishGame : SniperEvent()
    data object Reset : SniperEvent()
}
