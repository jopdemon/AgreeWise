package com.example.agreewise.network

data class GeminiResponse(
    val candidates: List<Candidate>
)

data class Candidate(
    val content: Content
)