import Foundation

enum ScoreCalculator {
    static func calculateScore(text: String) -> Int {
        var score = 0
        let textToScan = text.lowercased()
        for (keyword, weight) in KeywordDatabase.keywordWeights {
            if textToScan.contains(keyword) { score += weight }
        }
        return score
    }
}
