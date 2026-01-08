/**
 * File Path: yakki-core/src/commonMain/kotlin/com/yakki/core/models/ScramblerModels.kt
 * File Name: ScramblerModels.kt
 * Created: 2026-01-08
 * Version: 1.0.0
 *
 * Description: Domain models for Scrambler Mode (Sentence Builder game).
 * Platform-independent - works on Android, iOS (Flutter), Web.
 *
 * Scrambler Mode: User arranges scrambled words to form correct sentences.
 * Supports AI Referee for non-standard but grammatically valid answers.
 */

package com.yakki.core.models

import kotlinx.serialization.Serializable

/**
 * Game screen state machine for Scrambler sessions.
 */
@Serializable
enum class GameScreenState {
    SETUP,      // Settings screen (level, session length)
    PLAYING,    // Active gameplay (arrange tokens)
    VALIDATING, // AI Referee is checking the answer
    FEEDBACK,   // Result screen (correct/incorrect)
    FINISHED    // Session complete (final score)
}

/**
 * Game mode selection.
 */
@Serializable
enum class GameMode {
    NORMAL,     // Standard AI-generated exercises
    MISTAKES    // Review mode using saved mistakes
}

/**
 * Scrambler exercise - a single sentence to unscramble.
 */
@Serializable
data class ScramblerExercise(
    val id: String,
    val originalSentence: String,
    val scrambledWords: List<String>,
    val alternatives: List<String> = emptyList(),
    val translation: String? = null,
    val level: String = "A1",
    val tense: String = "Present Simple"
)

/**
 * Scrambler result - validation outcome.
 */
@Serializable
data class ScramblerResult(
    val isCorrect: Boolean,
    val scorePercent: Int,           // 0-100
    val correctCount: Int,
    val totalCount: Int,
    val message: String,
    val tokenResults: Map<Int, Boolean> = emptyMap(), // index -> isCorrectPosition
    val refereeResult: AIRefereeResult? = null
)

/**
 * AI Referee result for non-standard answers.
 *
 * Example:
 * - Original: "I often go to the park."
 * - Student: "To the park I often go." (Yoda style)
 * - AI Referee: isValid=true, qualityScore=70, feedback="Grammatically correct but unnatural"
 */
@Serializable
data class AIRefereeResult(
    val isValid: Boolean,            // Grammatically acceptable?
    val qualityScore: Int,           // 0-100
    val feedback: String,            // Explanation
    val betterVersion: String? = null
) {
    val xpMultiplier: Float
        get() = when {
            qualityScore >= 90 -> 1.0f
            qualityScore >= 70 -> 0.8f
            qualityScore >= 50 -> 0.5f
            else -> 0.2f
        }

    val colorTier: ColorTier
        get() = when {
            qualityScore >= 80 -> ColorTier.GREEN
            qualityScore >= 50 -> ColorTier.YELLOW
            else -> ColorTier.RED
        }

    @Serializable
    enum class ColorTier { GREEN, YELLOW, RED }
}

/**
 * Scrambler game state (platform-independent).
 */
@Serializable
data class ScramblerGameState(
    val screenState: GameScreenState = GameScreenState.SETUP,
    val gameMode: GameMode = GameMode.NORMAL,

    // Session settings
    val selectedLevel: String = "A1",
    val selectedTense: String = "Any",
    val selectedType: String = "Statement",
    val sessionLength: Int = 5,

    // Session progress
    val currentQuestionIndex: Int = 0,
    val sessionScore: Int = 0,
    val currentExercise: ScramblerExercise? = null,

    // Current question state
    val userPlacement: List<Int> = emptyList(),
    val lockedIndices: Set<Int> = emptySet(),
    val insertionIndex: Int? = null,
    val validationResult: ScramblerResult? = null,

    // Scoring flags
    val hasError: Boolean = false,
    val isHintUsed: Boolean = false,
    val isGenerating: Boolean = false,
    val isValidating: Boolean = false
) {
    val isLastQuestion: Boolean
        get() = currentQuestionIndex >= sessionLength - 1

    val progressPercent: Float
        get() = if (sessionLength > 0) {
            (currentQuestionIndex + 1).toFloat() / sessionLength
        } else 0f

    val accuracy: Float
        get() = if (currentQuestionIndex > 0) {
            sessionScore.toFloat() / currentQuestionIndex
        } else 0f
}

/**
 * Scrambler rank based on session performance.
 */
@Serializable
enum class ScramblerRank(val displayName: String, val emoji: String, val xpMultiplier: Float) {
    PERFECT("Perfect", "\uD83C\uDFC6", 2.0f),
    EXCELLENT("Excellent", "\uD83E\uDD47", 1.5f),
    GOOD("Good", "\uD83E\uDD48", 1.2f),
    FAIR("Fair", "\uD83E\uDD49", 1.0f),
    NEEDS_PRACTICE("Needs Practice", "\uD83D\uDCDA", 0.5f);

    companion object {
        fun fromAccuracy(accuracy: Float): ScramblerRank {
            return when {
                accuracy >= 1.0f -> PERFECT
                accuracy >= 0.9f -> EXCELLENT
                accuracy >= 0.7f -> GOOD
                accuracy >= 0.5f -> FAIR
                else -> NEEDS_PRACTICE
            }
        }
    }
}

/**
 * Scrambler game events (for state machine).
 */
sealed class ScramblerEvent {
    data class StartSession(
        val level: String,
        val tense: String,
        val sessionLength: Int
    ) : ScramblerEvent()

    data class LoadExercise(val exercise: ScramblerExercise) : ScramblerEvent()
    data class PlaceWord(val wordIndex: Int) : ScramblerEvent()
    data class RemoveWord(val placementIndex: Int) : ScramblerEvent()
    data class SetInsertionPoint(val index: Int?) : ScramblerEvent()
    data class LockWord(val index: Int) : ScramblerEvent()
    data object Submit : ScramblerEvent()
    data class Validated(val result: ScramblerResult) : ScramblerEvent()
    data object UseHint : ScramblerEvent()
    data object NextQuestion : ScramblerEvent()
    data object FinishSession : ScramblerEvent()
    data object Reset : ScramblerEvent()
}

/**
 * Grammar options constants.
 */
object GrammarOptions {
    val TENSES = listOf(
        "Any",
        "Present Simple",
        "Present Continuous",
        "Present Perfect",
        "Present Perfect Continuous",
        "Past Simple",
        "Past Continuous",
        "Past Perfect",
        "Past Perfect Continuous",
        "Future Simple",
        "Future Continuous",
        "Future Perfect",
        "Future Perfect Continuous"
    )

    val LEVELS = listOf("A1", "A2", "B1", "B2", "C1", "C2")

    val SENTENCE_TYPES = listOf("Statement", "Question", "Negative")

    val SESSION_LENGTHS = listOf(5, 10, 25, 50)
}
