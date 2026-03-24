import Foundation

enum GeminiApiError: Error {
    case invalidURL
    case noData
    case decodingError
    case apiError(String)
    case networkError(Error)
}

final class GeminiApiService {
    private let baseURL = "https://generativelanguage.googleapis.com"
    private let session: URLSession = {
        let config = URLSessionConfiguration.default
        config.timeoutIntervalForRequest = 60
        config.timeoutIntervalForResource = 60
        return URLSession(configuration: config)
    }()

    func generateContent(apiKey: String, request: GeminiRequest) async throws -> String {
        guard let url = URL(string: "\(baseURL)/v1/models/gemini-2.5-flash:generateContent?key=\(apiKey)") else {
            throw GeminiApiError.invalidURL
        }
        var urlRequest = URLRequest(url: url)
        urlRequest.httpMethod = "POST"
        urlRequest.setValue("application/json", forHTTPHeaderField: "Content-Type")
        urlRequest.httpBody = try JSONEncoder().encode(request)

        let (data, response) = try await session.data(for: urlRequest)
        guard let httpResponse = response as? HTTPURLResponse else { throw GeminiApiError.noData }

        if httpResponse.statusCode != 200 {
            if let json = try? JSONSerialization.jsonObject(with: data) as? [String: Any],
               let error = json["error"] as? [String: Any],
               let message = error["message"] as? String {
                throw GeminiApiError.apiError(message)
            }
            throw GeminiApiError.apiError("\(httpResponse.statusCode)")
        }

        let decoded = try JSONDecoder().decode(GeminiResponse.self, from: data)
        guard let text = decoded.candidates?.first?.content.parts?.first?.text else {
            throw GeminiApiError.decodingError
        }
        return text
    }
}
