/**
 * File Path: yakki-core/src/commonMain/kotlin/com/yakki/core/utils/SentenceUtils.kt
 * File Name: SentenceUtils.kt
 * Created: 2026-01-08
 * Version: 1.0.0
 *
 * Description: Smart sentence tokenizer for Scrambler mode.
 * Platform-independent - works on Android, iOS (Flutter), Web.
 *
 * Handles:
 * - Words with apostrophes: "don't" -> "don't" (single token)
 * - Punctuation marks: "Hello, world!" -> ["Hello", ",", "world", "!"]
 */

package com.yakki.core.utils

/**
 * SentenceUtils - Smart tokenizer for sentence scrambler.
 */
object SentenceUtils {

    private val PUNCTUATION_REGEX = Regex("[.,!?;:]")

    /**
     * Tokenize text into words and punctuation marks.
     *
     * Examples:
     * - "Hello, world!" -> ["Hello", ",", "world", "!"]
     * - "If I go, I will call." -> ["If", "I", "go", ",", "I", "will", "call", "."]
     * - "I don't know" -> ["I", "don't", "know"]
     */
    fun tokenize(text: String): List<String> {
        val regex = Regex("[\\w']+|[.,!?;:]")
        return regex.findAll(text).map { it.value }.toList()
    }

    /**
     * Reconstruct sentence from tokens with proper spacing.
     *
     * Rules:
     * - No space before punctuation
     * - Space between words
     *
     * Examples:
     * - ["Hello", ",", "world", "!"] -> "Hello, world!"
     */
    fun detokenize(tokens: List<String>): String {
        if (tokens.isEmpty()) return ""

        val sb = StringBuilder()
        tokens.forEachIndexed { index, token ->
            val isPunctuation = token.matches(PUNCTUATION_REGEX)

            if (index > 0 && !isPunctuation) {
                sb.append(" ")
            }
            sb.append(token)
        }
        return sb.toString()
    }

    /**
     * Check if a token is a punctuation mark.
     */
    fun isPunctuation(token: String): Boolean {
        return token.matches(PUNCTUATION_REGEX)
    }

    /**
     * Compare two tokens for equality.
     * - Punctuation: exact match required
     * - Words: case-insensitive
     */
    fun tokensMatch(token1: String, token2: String): Boolean {
        return if (isPunctuation(token1) || isPunctuation(token2)) {
            token1 == token2
        } else {
            token1.equals(token2, ignoreCase = true)
        }
    }

    /**
     * Mask a word in sentence by index.
     *
     * @param words List of words
     * @param maskIndex Index of word to mask
     * @param maskChar Character to use for mask (default "___")
     * @return Sentence with masked word
     */
    fun maskWordAtIndex(words: List<String>, maskIndex: Int, maskChar: String = "___"): String {
        return words.mapIndexed { index, word ->
            if (index == maskIndex) maskChar else word
        }.let { detokenize(it) }
    }

    /**
     * Split sentence into words (simple space split).
     */
    fun splitIntoWords(sentence: String): List<String> {
        return sentence.split(Regex("\\s+")).filter { it.isNotEmpty() }
    }

    /**
     * Count words in sentence (excluding punctuation).
     */
    fun countWords(sentence: String): Int {
        return tokenize(sentence).count { !isPunctuation(it) }
    }
}
