// MARK: - Request
struct GeminiRequest: Encodable {
    let contents: [GeminiContent]
}

struct GeminiContent: Encodable {
    let parts: [GeminiPart]
}

struct GeminiPart: Encodable {
    let text: String
}

// MARK: - Response
struct GeminiResponse: Decodable {
    let candidates: [GeminiCandidate]?
}

struct GeminiCandidate: Decodable {
    let content: GeminiResponseContent
}

struct GeminiResponseContent: Decodable {
    let parts: [GeminiResponsePart]?
}

struct GeminiResponsePart: Decodable {
    let text: String?
}
