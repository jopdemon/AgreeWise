import Foundation

enum RiskScanner {
    static func scanText(_ text: String) -> RiskAnalysisResult {
        let score = ScoreCalculator.calculateScore(text: text)
        let textToScan = text.lowercased()
        let flagged = KeywordDatabase.keywordWeights.keys.filter { textToScan.contains($0) }
        return RiskAnalysisResult(score: score, flaggedKeywords: Array(flagged))
    }
}
