/**
 * File Path: yakki-core/src/commonMain/kotlin/com/yakki/core/economy/LevelCalculator.kt
 * File Name: LevelCalculator.kt
 * Created: 2026-01-08
 * Version: 1.0.0
 *
 * Description: Calculates user level based on XP using CEFR-inspired progression.
 * Platform-independent - works on Android, iOS (Flutter), Web.
 *
 * Leveling System:
 * - A1 Novice: 0-499 XP
 * - A2 Apprentice: 500-1,499 XP
 * - B1 Intermediate: 1,500-2,999 XP
 * - B2 Upper-Intermediate: 3,000-4,999 XP
 * - C1 Advanced: 5,000-7,999 XP
 * - C2 Master: 8,000+ XP
 */

package com.yakki.core.economy

import kotlinx.serialization.Serializable

/**
 * Level data containing all level information.
 */
@Serializable
data class LevelInfo(
    val code: String,
    val name: String,
    val minXp: Long,
    val maxXp: Long?
) {
    val displayName: String
        get() = "$code $name"
}

/**
 * Progress data for XP towards next level.
 */
@Serializable
data class LevelProgress(
    val current: Long,
    val required: Long,
    val percentage: Float,
    val isMaxLevel: Boolean
)

/**
 * LevelCalculator - Determines user level based on accumulated XP.
 */
object LevelCalculator {

    val levels = listOf(
        LevelInfo("A1", "Novice", 0, 500),
        LevelInfo("A2", "Apprentice", 500, 1500),
        LevelInfo("B1", "Intermediate", 1500, 3000),
        LevelInfo("B2", "Upper-Intermediate", 3000, 5000),
        LevelInfo("C1", "Advanced", 5000, 8000),
        LevelInfo("C2", "Master", 8000, null)
    )

    /**
     * Get level info for given XP amount.
     */
    fun getLevelForXp(xp: Long): LevelInfo {
        return levels.lastOrNull { xp >= it.minXp } ?: levels.first()
    }

    /**
     * Get level code (A1, A2, etc.) for given XP.
     */
    fun getLevelCode(xp: Long): String {
        return getLevelForXp(xp).code
    }

    /**
     * Calculate progress towards next level.
     */
    fun getProgressToNextLevel(xp: Long): LevelProgress {
        val level = getLevelForXp(xp)

        if (level.maxXp == null) {
            return LevelProgress(
                current = xp - level.minXp,
                required = xp - level.minXp,
                percentage = 1.0f,
                isMaxLevel = true
            )
        }

        val xpInCurrentLevel = xp - level.minXp
        val xpRequiredForLevel = level.maxXp - level.minXp

        return LevelProgress(
            current = xpInCurrentLevel,
            required = xpRequiredForLevel,
            percentage = (xpInCurrentLevel.toFloat() / xpRequiredForLevel).coerceIn(0f, 1f),
            isMaxLevel = false
        )
    }

    /**
     * Get next level info (or null if at max).
     */
    fun getNextLevel(xp: Long): LevelInfo? {
        val currentLevel = getLevelForXp(xp)
        val currentIndex = levels.indexOf(currentLevel)
        return if (currentIndex < levels.size - 1) {
            levels[currentIndex + 1]
        } else {
            null
        }
    }

    /**
     * Check if user leveled up from old XP to new XP.
     */
    fun checkLevelUp(oldXp: Long, newXp: Long): LevelInfo? {
        val oldLevel = getLevelForXp(oldXp)
        val newLevel = getLevelForXp(newXp)

        return if (newLevel.code != oldLevel.code) {
            newLevel
        } else {
            null
        }
    }

    /**
     * Calculate XP needed to reach next level.
     */
    fun xpToNextLevel(xp: Long): Long {
        val level = getLevelForXp(xp)
        return if (level.maxXp != null) {
            level.maxXp - xp
        } else {
            0
        }
    }
}
