package com.example.agreewise.ml

object KeywordDatabase {

    val keywordWeights = mapOf(
        // === HIGH RISK (CRITICAL: 40 - 60 Points) ===
        "high risk" to 60,
        "high-risk" to 60,
        "financial loss" to 50,
        "legal disputes" to 50,
        "reputational impact" to 40,
        "adverse outcomes" to 40,
        "liability" to 40,
        "indemnity" to 40,
        "termination" to 40,
        "breach" to 40,
        "damages" to 40,
        "limitation of liability" to 50,
        "irrevocable" to 50,
        "waives the right" to 50,
        "acceleration of debt" to 60,
        "collateral forfeiture" to 60,
        "power of attorney" to 60,
        "binding arbitration" to 40,
        "class action waiver" to 45,
        "non-compete" to 45,
        "transfer of ownership" to 50,

        // === MEDIUM RISK (SIGNIFICANT: 20 - 35 Points) ===
        "ambiguous" to 30,
        "disproportionate" to 30,
        "insufficient verification" to 30,
        "unclear provisions" to 30,
        "subscription" to 25,
        "automatic renewal" to 30,
        "recurring charge" to 25,
        "non-refundable" to 30,
        "early termination fee" to 35,
        "payment" to 20,
        "automatic" to 20,
        "fee" to 20,
        "penalty" to 25,
        "indemnify" to 30,
        "hold harmless" to 30,

        // === LOW RISK (INFORMATIONAL: 5 - 15 Points) ===
        "subject to change" to 10,
        "cookies" to 5,
        "data collection" to 10,
        "privacy policy" to 5,
        "warranty disclaimer" to 15,
        "force majeure" to 10,
        "eula" to 5
    )

    fun getCategory(keyword: String): String {
        val k = keyword.lowercase()
        return when {
            k in listOf("acceleration of debt", "collateral forfeiture", "power of attorney") -> "Banking & Loans"
            k in listOf("non-compete", "indemnify", "hold harmless") -> "Professional & Employment"
            k in listOf("transfer of ownership", "data collection", "cookies", "eula", "privacy policy") -> "IT & Software"
            k in listOf("automatic renewal", "recurring charge", "subscription", "early termination fee") -> "Subscription Services"
            else -> "General Legal"
        }
    }
}