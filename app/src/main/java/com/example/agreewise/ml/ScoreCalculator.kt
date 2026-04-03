package com.example.agreewise.ml

object ScoreCalculator {

    fun calculateScore(text: String): Int {
        var totalScore = 0
        val textToScan = text.lowercase()

        // Sort keywords by length descending to handle "limitation of liability" before "liability"
        val sortedKeywords = KeywordDatabase.keywordWeights.keys.sortedByDescending { it.length }
        val foundKeywords = mutableSetOf<String>()

        var remainingText = textToScan
        for (keyword in sortedKeywords) {
            if (remainingText.contains(keyword)) {
                totalScore += KeywordDatabase.keywordWeights[keyword] ?: 0
                foundKeywords.add(keyword)
                // Remove the keyword from text so it's not double-counted by shorter substrings
                remainingText = remainingText.replace(keyword, " ")
            }
        }

        return totalScore
    }
}