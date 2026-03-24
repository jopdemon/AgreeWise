package com.example.agreewise.ml

// Data class to hold the results of the local scan
data class RiskAnalysisResult(val score: Int, val flaggedKeywords: List<String>)

object RiskScanner {

    fun scanText(text: String): RiskAnalysisResult {
        val score = ScoreCalculator.calculateScore(text)
        val flaggedKeywords = mutableListOf<String>()
        val textToScan = text.lowercase()

        for (keyword in KeywordDatabase.keywordWeights.keys) {
            if (textToScan.contains(keyword)) {
                flaggedKeywords.add(keyword)
            }
        }

        return RiskAnalysisResult(score, flaggedKeywords)
    }
}
