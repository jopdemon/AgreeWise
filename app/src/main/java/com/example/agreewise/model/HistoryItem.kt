package com.example.agreewise.model

data class HistoryItem(
    val id: String = "",
    val title: String = "",
    val date: String = "",
    val riskScore: Int = 0,
    val riskLevel: String = "",
    val content: String = "",
    val explanation: String = ""
)