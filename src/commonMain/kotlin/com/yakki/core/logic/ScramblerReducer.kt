/**
 * File Path: yakki-core/src/commonMain/kotlin/com/yakki/core/logic/ScramblerReducer.kt
 * File Name: ScramblerReducer.kt
 * Created: 2026-01-08
 * Version: 1.0.0
 *
 * Description: Pure functional reducer for Scrambler game state.
 * MVI pattern: (State, Event) -> State
 *
 * Fair Scoring System:
 * - Clean win (1st attempt): 100% XP
 * - Self-corrected (2nd+ attempt): 70% XP
 * - Gave up (Help button): 0% XP
 */

package com.yakki.core.logic

import com.yakki.core.models.GameScreenState
import com.yakki.core.models.ScramblerEvent
import com.yakki.core.models.ScramblerGameState
import com.yakki.core.models.ScramblerRank
import com.yakki.core.utils.SentenceUtils

/**
 * ScramblerReducer - Pure function that transforms state based on events.
 */
object ScramblerReducer {

    /**
     * Process event and return new state.
     * Pure function - no side effects.
     */
    fun reduce(state: ScramblerGameState, event: ScramblerEvent): ScramblerGameState {
        return when (event) {
            is ScramblerEvent.StartSession -> handleStartSession(state, event)
            is ScramblerEvent.LoadExercise -> handleLoadExercise(state, event)
            is ScramblerEvent.PlaceWord -> handlePlaceWord(state, event)
            is ScramblerEvent.RemoveWord -> handleRemoveWord(state, event)
            is ScramblerEvent.SetInsertionPoint -> handleSetInsertionPoint(state, event)
            is ScramblerEvent.LockWord -> handleLockWord(state, event)
            is ScramblerEvent.Submit -> handleSubmit(state)
            is ScramblerEvent.Validated -> handleValidated(state, event)
            is ScramblerEvent.UseHint -> handleUseHint(state)
            is ScramblerEvent.NextQuestion -> handleNextQuestion(state)
            is ScramblerEvent.FinishSession -> handleFinishSession(state)
            is ScramblerEvent.Reset -> ScramblerGameState()
        }
    }

    private fun handleStartSession(
        state: ScramblerGameState,
        event: ScramblerEvent.StartSession
    ): ScramblerGameState {
        return state.copy(
            screenState = GameScreenState.PLAYING,
            selectedLevel = event.level,
            selectedTense = event.tense,
            sessionLength = event.sessionLength,
            currentQuestionIndex = 0,
            sessionScore = 0,
            isGenerating = true
        )
    }

    private fun handleLoadExercise(
        state: ScramblerGameState,
        event: ScramblerEvent.LoadExercise
    ): ScramblerGameState {
        return state.copy(
            currentExercise = event.exercise,
            userPlacement = emptyList(),
            lockedIndices = emptySet(),
            insertionIndex = null,
            validationResult = null,
            hasError = false,
            isHintUsed = false,
            isGenerating = false,
            screenState = GameScreenState.PLAYING
        )
    }

    private fun handlePlaceWord(
        state: ScramblerGameState,
        event: ScramblerEvent.PlaceWord
    ): ScramblerGameState {
        // Word already placed?
        if (event.wordIndex in state.userPlacement) {
            return state
        }

        val newPlacement = if (state.insertionIndex != null) {
            // Insert at specific position
            state.userPlacement.toMutableList().apply {
                add(state.insertionIndex.coerceAtMost(size), event.wordIndex)
            }
        } else {
            // Append to end
            state.userPlacement + event.wordIndex
        }

        return state.copy(
            userPlacement = newPlacement,
            insertionIndex = null // Reset cursor after placement
        )
    }

    private fun handleRemoveWord(
        state: ScramblerGameState,
        event: ScramblerEvent.RemoveWord
    ): ScramblerGameState {
        // Can't remove locked words
        val wordIndex = state.userPlacement.getOrNull(event.placementIndex) ?: return state
        if (wordIndex in state.lockedIndices) {
            return state
        }

        return state.copy(
            userPlacement = state.userPlacement.filterIndexed { index, _ ->
                index != event.placementIndex
            }
        )
    }

    private fun handleSetInsertionPoint(
        state: ScramblerGameState,
        event: ScramblerEvent.SetInsertionPoint
    ): ScramblerGameState {
        return state.copy(insertionIndex = event.index)
    }

    private fun handleLockWord(
        state: ScramblerGameState,
        event: ScramblerEvent.LockWord
    ): ScramblerGameState {
        val newLocked = if (event.index in state.lockedIndices) {
            state.lockedIndices - event.index
        } else {
            state.lockedIndices + event.index
        }
        return state.copy(lockedIndices = newLocked)
    }

    private fun handleSubmit(state: ScramblerGameState): ScramblerGameState {
        return state.copy(
            screenState = GameScreenState.VALIDATING,
            isValidating = true
        )
    }

    private fun handleValidated(
        state: ScramblerGameState,
        event: ScramblerEvent.Validated
    ): ScramblerGameState {
        val newScore = if (event.result.isCorrect && !state.hasError && !state.isHintUsed) {
            state.sessionScore + 1
        } else {
            state.sessionScore
        }

        return state.copy(
            screenState = GameScreenState.FEEDBACK,
            validationResult = event.result,
            sessionScore = newScore,
            hasError = !event.result.isCorrect || state.hasError,
            isValidating = false
        )
    }

    private fun handleUseHint(state: ScramblerGameState): ScramblerGameState {
        val exercise = state.currentExercise ?: return state

        // Show correct sentence - place all words in correct order
        val correctOrder = (0 until exercise.scrambledWords.size).toList()

        return state.copy(
            userPlacement = correctOrder,
            isHintUsed = true
        )
    }

    private fun handleNextQuestion(state: ScramblerGameState): ScramblerGameState {
        return if (state.isLastQuestion) {
            handleFinishSession(state)
        } else {
            state.copy(
                screenState = GameScreenState.PLAYING,
                currentQuestionIndex = state.currentQuestionIndex + 1,
                currentExercise = null,
                userPlacement = emptyList(),
                lockedIndices = emptySet(),
                insertionIndex = null,
                validationResult = null,
                hasError = false,
                isHintUsed = false,
                isGenerating = true
            )
        }
    }

    private fun handleFinishSession(state: ScramblerGameState): ScramblerGameState {
        return state.copy(
            screenState = GameScreenState.FINISHED
        )
    }
}

/**
 * ScramblerValidator - Validates user sentence against correct answer.
 */
object ScramblerValidator {

    /**
     * Validate user's word arrangement.
     *
     * @param exercise The exercise being solved
     * @param userPlacement Indices of words in user's order
     * @return Validation result
     */
    fun validate(
        exercise: com.yakki.core.models.ScramblerExercise,
        userPlacement: List<Int>
    ): com.yakki.core.models.ScramblerResult {
        // Build user's sentence
        val userSentence = userPlacement
            .mapNotNull { exercise.scrambledWords.getOrNull(it) }
            .let { SentenceUtils.detokenize(it) }

        // Check against original
        if (userSentence.equals(exercise.originalSentence, ignoreCase = true)) {
            return com.yakki.core.models.ScramblerResult(
                isCorrect = true,
                scorePercent = 100,
                correctCount = exercise.scrambledWords.size,
                totalCount = exercise.scrambledWords.size,
                message = "Perfect!"
            )
        }

        // Check against alternatives
        for (alt in exercise.alternatives) {
            if (userSentence.equals(alt, ignoreCase = true)) {
                return com.yakki.core.models.ScramblerResult(
                    isCorrect = true,
                    scorePercent = 100,
                    correctCount = exercise.scrambledWords.size,
                    totalCount = exercise.scrambledWords.size,
                    message = "Correct! (Alternative form)"
                )
            }
        }

        // Calculate partial score
        val originalWords = SentenceUtils.tokenize(exercise.originalSentence)
        val userWords = userPlacement.mapNotNull { exercise.scrambledWords.getOrNull(it) }

        var correctCount = 0
        val tokenResults = mutableMapOf<Int, Boolean>()

        userWords.forEachIndexed { index, word ->
            val isCorrect = originalWords.getOrNull(index)?.let {
                SentenceUtils.tokensMatch(word, it)
            } ?: false
            tokenResults[index] = isCorrect
            if (isCorrect) correctCount++
        }

        val scorePercent = if (originalWords.isNotEmpty()) {
            (correctCount * 100) / originalWords.size
        } else 0

        return com.yakki.core.models.ScramblerResult(
            isCorrect = false,
            scorePercent = scorePercent,
            correctCount = correctCount,
            totalCount = originalWords.size,
            message = "Try again! $correctCount/${originalWords.size} words in correct position.",
            tokenResults = tokenResults
        )
    }
}
