import SwiftUI

struct ContentView: View {
    @State private var contractText = ""
    @State private var resultText = "Ready to analyze."
    @State private var riskScoreText = ""
    @State private var flaggedKeywordsText = ""
    @State private var showRiskSection = false
    @State private var showAIExplanation = false
    @State private var isLoading = false
    @State private var geminiApiKey: String = ""

    private let apiService = GeminiApiService()

    var body: some View {
        NavigationView {
            ScrollView {
                VStack(alignment: .leading, spacing: 16) {
                    Text("Paste contract text here")
                        .font(.subheadline)
                        .foregroundStyle(.secondary)
                    TextEditor(text: $contractText)
                        .frame(minHeight: 200)
                        .padding(8)
                        .overlay(RoundedRectangle(cornerRadius: 8).stroke(Color.secondary.opacity(0.3), lineWidth: 1))
                        .scrollContentBackground(.hidden)

                    Button("Analyze") { analyzeContract() }
                        .buttonStyle(.borderedProminent)
                        .disabled(contractText.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty)
                        .frame(maxWidth: .infinity)

                    if isLoading { ProgressView().frame(maxWidth: .infinity) }

                    VStack(alignment: .leading, spacing: 12) {
                        Text("Analysis Result").font(.title2).fontWeight(.semibold)
                        if showRiskSection {
                            Text(riskScoreText).font(.headline)
                            Text(flaggedKeywordsText).font(.body).foregroundStyle(.secondary)
                        }
                        if showAIExplanation { Text("AI Explanation").font(.headline).padding(.top, 8) }
                        Text(resultText).font(.body).fixedSize(horizontal: false, vertical: true)
                    }
                    .padding()
                    .frame(maxWidth: .infinity, alignment: .leading)
                    .background(Color(.secondarySystemBackground))
                    .cornerRadius(12)
                    .padding(.top, 8)
                }
                .padding()
            }
            .navigationTitle("AgreeWise")
            .onAppear { loadApiKey() }
        }
    }

    private func loadApiKey() {
        geminiApiKey = (Bundle.main.object(forInfoDictionaryKey: "GEMINI_API_KEY") as? String) ?? ""
    }

    private func analyzeContract() {
        let text = contractText.trimmingCharacters(in: .whitespacesAndNewlines)
        guard !text.isEmpty else { return }

        let result = RiskScanner.scanText(text)
        let riskLevel = result.score > 60 ? "High" : result.score > 30 ? "Medium" : "Low"

        showRiskSection = true
        riskScoreText = "Risk Score: \(result.score) (\(riskLevel))"
        flaggedKeywordsText = "Flagged: \(result.flaggedKeywords.joined(separator: ", "))"

        if result.score > 20 {
            showAIExplanation = true
            callGemini(clause: text, keywords: result.flaggedKeywords)
        } else {
            showAIExplanation = false
            resultText = "No high-risk clauses were detected that require a detailed AI explanation."
        }
    }

    private func callGemini(clause: String, keywords: [String]) {
        if geminiApiKey.isEmpty {
            resultText = "Add GEMINI_API_KEY in Info.plist to get AI explanations."
            return
        }
        isLoading = true
        resultText = "Getting AI explanation..."

        let prompt = """
        You are a legal assistant analyzing a contract clause.
        The local scanner has already flagged these keywords: \(keywords.joined(separator: ", ")).

        1. Explain the full clause in simple English.
        2. Focusing on the flagged keywords, explain why this clause is risky.
        3. Give short, actionable advice for the user.

        Clause:
        "\(clause)"
        """
        let request = GeminiRequest(contents: [GeminiContent(parts: [GeminiPart(text: prompt)])])

        Task {
            do {
                let explanation = try await apiService.generateContent(apiKey: geminiApiKey, request: request)
                await MainActor.run { isLoading = false; resultText = explanation }
            } catch {
                await MainActor.run {
                    isLoading = false
                    if let err = error as? GeminiApiError {
                        switch err {
                        case .apiError(let msg): resultText = "Error: \(msg)"
                        case .networkError(let e): resultText = "Network: \(e.localizedDescription)"
                        default: resultText = "Error: \(error.localizedDescription)"
                        }
                    } else {
                        resultText = "Error: \(error.localizedDescription)"
                    }
                }
            }
        }
    }
}

#Preview { ContentView() }
