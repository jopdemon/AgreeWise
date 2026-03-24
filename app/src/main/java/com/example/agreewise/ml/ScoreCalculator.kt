package com.example.agreewise.ml

object ScoreCalculator {

    fun calculateScore(text: String): Int {
        var score = 0
        val textToScan = text.lowercase()

        for ((keyword, weight) in KeywordDatabase.keywordWeights) {
            if (textToScan.contains(keyword)) {
                score += weight
            }
        }

        return score
    }
}
