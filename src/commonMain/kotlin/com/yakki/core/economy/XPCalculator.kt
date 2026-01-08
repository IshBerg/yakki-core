/**
 * File Path: yakki-core/src/commonMain/kotlin/com/yakki/core/economy/XPCalculator.kt
 * File Name: XPCalculator.kt
 * Created: 2026-01-08
 * Version: 1.0.0
 *
 * Description: Unified XP Calculator for all game modes.
 * Platform-independent - works on Android, iOS (Flutter), Web.
 *
 * Central Bank of the Yakki Economy:
 * - Higher CEFR levels pay more (A1 x1.0 -> C2 x5.0)
 * - Perfect completion = 100% reward
 * - Self-corrected = 70% reward
 * - Gave up = 0% reward
 */

package com.yakki.core.economy

import kotlinx.serialization.Serializable

/**
 * XPCalculator - Unified currency system for all game modes.
 */
object XPCalculator {

    private const val BASE_XP = 10

    // ============================================================
    // CEFR LEVEL MULTIPLIERS
    // ============================================================

    private fun getLevelMultiplier(level: CEFRLevel): Double {
        return when (level) {
            CEFRLevel.A1 -> 1.0   // Beginner
            CEFRLevel.A2 -> 1.2   // Elementary
            CEFRLevel.B1 -> 1.5   // Intermediate
            CEFRLevel.B2 -> 2.0   // Upper-Intermediate
            CEFRLevel.C1 -> 3.0   // Advanced
            CEFRLevel.C2 -> 5.0   // Mastery
        }
    }

    // ============================================================
    // ACCURACY MULTIPLIERS
    // ============================================================

    private fun getAccuracyMultiplier(isPerfect: Boolean, hintUsed: Boolean): Double {
        return when {
            hintUsed -> 0.0      // Gave up = no reward
            isPerfect -> 1.0     // Clean win = full reward
            else -> 0.7          // Self-corrected = 70%
        }
    }

    // ============================================================
    // SCRAMBLER XP
    // ============================================================

    /**
     * Calculate XP for Scrambler (sentence building) mode.
     */
    fun calculateScramblerXP(
        level: CEFRLevel,
        sentenceLength: Int,
        isPerfect: Boolean,
        hintUsed: Boolean
    ): Int {
        val complexityMultiplier = when {
            sentenceLength <= 6 -> 1.0
            sentenceLength <= 10 -> 1.2
            else -> 1.5
        }

        val total = BASE_XP *
                getLevelMultiplier(level) *
                complexityMultiplier *
                getAccuracyMultiplier(isPerfect, hintUsed)

        return total.toInt()
    }

    fun calculateScramblerXP(
        levelCode: String,
        sentenceLength: Int,
        isPerfect: Boolean,
        hintUsed: Boolean
    ): Int {
        val level = CEFRLevel.fromCode(levelCode)
        return calculateScramblerXP(level, sentenceLength, isPerfect, hintUsed)
    }

    // ============================================================
    // CLOZE XP
    // ============================================================

    /**
     * Calculate XP for Cloze Drills mode.
     */
    fun calculateClozeXP(
        level: CEFRLevel,
        correctCount: Int,
        accuracy: Float,
        wasOvertime: Boolean
    ): Int {
        val baseReward = correctCount * BASE_XP
        val levelMultiplier = getLevelMultiplier(level)

        // Accuracy bonus
        val accuracyBonus = when {
            accuracy >= 1.0f -> 2.0    // Perfect
            accuracy >= 0.9f -> 1.5    // Excellent
            accuracy >= 0.7f -> 1.2    // Good
            else -> 1.0                 // Fair
        }

        // Overtime penalty
        val timePenalty = if (wasOvertime) 0.9 else 1.0

        return (baseReward * levelMultiplier * accuracyBonus * timePenalty).toInt()
    }

    fun calculateClozeXP(
        levelCode: String,
        correctCount: Int,
        accuracy: Float,
        wasOvertime: Boolean
    ): Int {
        val level = CEFRLevel.fromCode(levelCode)
        return calculateClozeXP(level, correctCount, accuracy, wasOvertime)
    }

    // ============================================================
    // SNIPER XP
    // ============================================================

    /**
     * Calculate XP for Sniper (grammar combat) mode.
     */
    fun calculateSniperXP(
        level: CEFRLevel,
        correctCount: Int,
        accuracy: Float,
        coverIntegrity: Float
    ): Int {
        val baseReward = correctCount * BASE_XP
        val levelMultiplier = getLevelMultiplier(level)

        // Cover integrity bonus (survival bonus)
        val survivalBonus = 1.0 + (coverIntegrity * 0.5) // 0-50% bonus

        // Accuracy bonus
        val accuracyBonus = when {
            accuracy >= 0.95f -> 1.5
            accuracy >= 0.8f -> 1.2
            else -> 1.0
        }

        return (baseReward * levelMultiplier * survivalBonus * accuracyBonus).toInt()
    }

    // ============================================================
    // XP PREVIEW
    // ============================================================

    /**
     * Calculate maximum possible XP for display.
     */
    fun getMaxPossibleXP(level: CEFRLevel, sentenceLength: Int): Int {
        return calculateScramblerXP(level, sentenceLength, isPerfect = true, hintUsed = false)
    }
}

// ============================================================
// CEFR LEVEL ENUM
// ============================================================

/**
 * Common European Framework of Reference (CEFR) levels.
 */
@Serializable
enum class CEFRLevel(val code: String, val displayName: String) {
    A1("A1", "Beginner"),
    A2("A2", "Elementary"),
    B1("B1", "Intermediate"),
    B2("B2", "Upper-Intermediate"),
    C1("C1", "Advanced"),
    C2("C2", "Mastery");

    companion object {
        fun fromCode(code: String): CEFRLevel {
            return entries.find { it.code.equals(code, ignoreCase = true) } ?: A1
        }
    }
}
