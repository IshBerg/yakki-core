/**
 * File Path: yakki-core/src/commonMain/kotlin/com/yakki/core/models/ClozeModels.kt
 * File Name: ClozeModels.kt
 * Created: 2026-01-08
 * Version: 1.0.0
 *
 * Description: Pure Kotlin data models for Cloze Drills game mode.
 * Platform-independent - can be used in Android, iOS (Flutter), Web.
 *
 * Cloze Test Types:
 * - Single word gap: "I ___ to the store" (went)
 * - Multiple choice: 4 options including correct answer
 * - Contextual hints: translation available
 */

package com.yakki.core.models

import kotlinx.serialization.Serializable

/**
 * ClozeQuestion - A single cloze test item.
 *
 * @param id Unique question ID
 * @param unitId Source GameUnit ID
 * @param originalSentence Full original sentence
 * @param maskedSentence Sentence with gap (___)
 * @param correctAnswer The word that fills the gap
 * @param options List of answer options (including correct)
 * @param gapIndex Index of the gap in word list
 * @param translation Translation hint (optional)
 * @param difficulty Question difficulty (1.0-5.0)
 */
@Serializable
data class ClozeQuestion(
    val id: String,
    val unitId: String,
    val originalSentence: String,
    val maskedSentence: String,
    val correctAnswer: String,
    val options: List<String>,
    val gapIndex: Int,
    val translation: String? = null,
    val difficulty: Float = 1.0f
)

/**
 * ClozeDrill - A collection of cloze questions from a library.
 *
 * @param libraryId Source library ID
 * @param libraryName Library display name
 * @param libraryEmoji Library icon emoji
 * @param cefrLevel CEFR level (A1-C2)
 * @param questions List of cloze questions
 * @param totalQuestions Total number of questions
 */
@Serializable
data class ClozeDrill(
    val libraryId: String,
    val libraryName: String,
    val libraryEmoji: String,
    val cefrLevel: String,
    val questions: List<ClozeQuestion>,
    val totalQuestions: Int = questions.size
)

/**
 * ClozeRound - Current game round state.
 *
 * @param question Current question
 * @param selectedAnswer User's selected answer (null if not answered)
 * @param isCorrect Whether the answer is correct (null if not submitted)
 * @param timeSpent Time spent on this question (ms)
 */
@Serializable
data class ClozeRound(
    val question: ClozeQuestion,
    val selectedAnswer: String? = null,
    val isCorrect: Boolean? = null,
    val timeSpent: Long = 0
)

/**
 * ClozeGameState - Complete game state (platform-independent).
 * UI state is platform-specific; this is the core game logic state.
 */
@Serializable
data class ClozeGameState(
    // Game state
    val currentDrill: ClozeDrill? = null,
    val currentIndex: Int = 0,

    // Progress
    val totalCorrect: Int = 0,
    val totalWrong: Int = 0,
    val streak: Int = 0,
    val bestStreak: Int = 0,

    // Timer
    val timeLeftSeconds: Int = 30,
    val isOvertime: Boolean = false,

    // Results
    val isGameFinished: Boolean = false,
    val xpAwarded: Int = 0
) {
    val totalAnswered: Int get() = totalCorrect + totalWrong

    val accuracy: Float get() = if (totalAnswered > 0) {
        totalCorrect.toFloat() / totalAnswered
    } else 0f

    val progress: Float get() = if (currentDrill != null && currentDrill.totalQuestions > 0) {
        currentIndex.toFloat() / currentDrill.totalQuestions
    } else 0f

    val currentQuestion: ClozeQuestion? get() = currentDrill?.questions?.getOrNull(currentIndex)
}

/**
 * ClozeRank - Performance rank based on accuracy.
 */
enum class ClozeRank(val displayName: String, val emoji: String, val xpMultiplier: Float) {
    PERFECT("Perfect", "\uD83C\uDFC6", 2.0f),      // Trophy
    EXCELLENT("Excellent", "\uD83E\uDD47", 1.5f),  // Gold medal
    GOOD("Good", "\uD83E\uDD48", 1.2f),            // Silver medal
    FAIR("Fair", "\uD83E\uDD49", 1.0f),            // Bronze medal
    NEEDS_PRACTICE("Needs Practice", "\uD83D\uDCDA", 0.5f); // Books

    companion object {
        fun fromAccuracy(accuracy: Float, wasOvertime: Boolean): ClozeRank {
            val adjustedAccuracy = if (wasOvertime) accuracy * 0.9f else accuracy
            return when {
                adjustedAccuracy >= 1.0f -> PERFECT
                adjustedAccuracy >= 0.9f -> EXCELLENT
                adjustedAccuracy >= 0.7f -> GOOD
                adjustedAccuracy >= 0.5f -> FAIR
                else -> NEEDS_PRACTICE
            }
        }
    }
}

/**
 * ClozeEvent - Game events (for state machine).
 */
sealed class ClozeEvent {
    data class StartDrill(val drill: ClozeDrill) : ClozeEvent()
    data class SubmitAnswer(val answer: String) : ClozeEvent()
    data object Skip : ClozeEvent()
    data object NextQuestion : ClozeEvent()
    data object TimerTick : ClozeEvent()
    data object TimeExpired : ClozeEvent()
    data object FinishGame : ClozeEvent()
    data object Reset : ClozeEvent()
}

/**
 * GrammarErrorType to Cloze Library mapping.
 * Used for Intel Reader → Cloze Drills integration.
 */
enum class ClozeLibraryMapping(
    val errorType: String,
    val libraryFile: String?,
    val deckFilter: String?,
    val isSupported: Boolean,
    val alternativeDrillType: String?
) {
    // === FILL_IN_BLANK types (supported by Cloze) ===
    VERB_TENSE(
        errorType = "VERB_TENSE",
        libraryFile = "cloze_verb_tenses.json",
        deckFilter = null,
        isSupported = true,
        alternativeDrillType = null
    ),
    ARTICLE_USAGE(
        errorType = "ARTICLE_USAGE",
        libraryFile = "cloze_articles.json",
        deckFilter = null,
        isSupported = true,
        alternativeDrillType = null
    ),
    PLURAL_SINGULAR(
        errorType = "PLURAL_SINGULAR",
        libraryFile = "cloze_plurals.json",
        deckFilter = null,
        isSupported = false,
        alternativeDrillType = "FILL_IN_BLANK"
    ),

    // === MULTIPLE_CHOICE types (can convert to Cloze) ===
    SUBJECT_VERB_AGREE(
        errorType = "SUBJECT_VERB_AGREE",
        libraryFile = "cloze_irregular_verbs_a2.json",
        deckFilter = "mixed_practice",
        isSupported = true,
        alternativeDrillType = null
    ),
    PRONOUN_REFERENCE(
        errorType = "PRONOUN_REFERENCE",
        libraryFile = "cloze_pronouns.json",
        deckFilter = null,
        isSupported = true,
        alternativeDrillType = null
    ),
    PREPOSITION(
        errorType = "PREPOSITION",
        libraryFile = "cloze_prepositions.json",
        deckFilter = null,
        isSupported = true,
        alternativeDrillType = null
    ),

    // === SENTENCE_BUILDING types ===
    WORD_ORDER(
        errorType = "WORD_ORDER",
        libraryFile = null,
        deckFilter = null,
        isSupported = false,
        alternativeDrillType = "SENTENCE_BUILDING"
    ),
    QUESTION_FORM(
        errorType = "QUESTION_FORM",
        libraryFile = "cloze_questions.json",
        deckFilter = null,
        isSupported = true,
        alternativeDrillType = null
    ),
    NEGATIVE_FORM(
        errorType = "NEGATIVE_FORM",
        libraryFile = "cloze_negatives.json",
        deckFilter = null,
        isSupported = true,
        alternativeDrillType = null
    ),

    // === ERROR_CORRECTION types ===
    COMPARATIVE(
        errorType = "COMPARATIVE",
        libraryFile = "cloze_comparatives.json",
        deckFilter = null,
        isSupported = false,
        alternativeDrillType = "FILL_IN_BLANK"
    ),
    SPELLING(
        errorType = "SPELLING",
        libraryFile = null,
        deckFilter = null,
        isSupported = false,
        alternativeDrillType = "ERROR_CORRECTION"
    ),
    CAPITALIZATION(
        errorType = "CAPITALIZATION",
        libraryFile = null,
        deckFilter = null,
        isSupported = false,
        alternativeDrillType = "ERROR_CORRECTION"
    ),
    PUNCTUATION(
        errorType = "PUNCTUATION",
        libraryFile = null,
        deckFilter = null,
        isSupported = false,
        alternativeDrillType = "ERROR_CORRECTION"
    );

    companion object {
        /**
         * Find library for given error type.
         */
        fun findLibraryForError(errorType: String): ClozeLibraryMapping? {
            return entries.find { it.errorType == errorType && it.isSupported }
        }

        /**
         * Get all error types that can be trained with Cloze.
         */
        fun getSupportedErrorTypes(): List<String> {
            return entries.filter { it.isSupported }.map { it.errorType }
        }

        /**
         * Get error types that need new libraries.
         */
        fun getPendingLibraries(): List<ClozeLibraryMapping> {
            return entries.filter { !it.isSupported && it.libraryFile != null }
        }
    }
}
