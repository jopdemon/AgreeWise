package com.example.agreewise.ml

object KeywordDatabase {

    val keywordWeights = mapOf(

        // === HIGH RISK (20 - 30 Points) ===
        "acceleration of debt" to 30,
        "collateral forfeiture" to 30,
        "deficiency judgment" to 30,
        "guaranty" to 30,
        "power of attorney" to 30,
        "balloon payment" to 25,
        "compounding interest" to 25,
        "right of offset" to 25,
        "cross-default" to 25,
        "garnishment" to 25,
        "variable rate hike" to 20,

        // Real Estate
        "specific performance" to 30,
        "contingency waiver" to 30,
        "lien on property" to 30,
        "right of first refusal" to 30,
        "earnest money forfeiture" to 25,
        "as-is condition" to 25,
        "encumbrance" to 25,
        "easement" to 20,
        "quitclaim" to 20,

        // Employment / Business
        "work for hire" to 30,
        "non-compete" to 25,
        "non-solicitation" to 25,
        "liquidated damages" to 25,
        "intellectual property assignment" to 25,

        // IT / Legal
        "transfer of ownership" to 30,
        "irrevocable license" to 30,
        "third-party data sharing" to 20,
        "security breach waiver" to 25,

        "irrevocable" to 30,
        "waives the right" to 30,
        "binding arbitration" to 25,
        "class action waiver" to 25,
        "exclusive jurisdiction" to 20,
        "without notice" to 20,
        "at its sole discretion" to 20,
        "limitation of liability" to 20,

        // === MEDIUM RISK ===
        "excludes wear and tear" to 15,
        "void if tampered" to 15,
        "refurbished parts" to 15,
        "authorized service center only" to 15,
        "consequential damages" to 15,
        "deductible" to 15,
        "shipping and handling fees" to 10,
        "limited warranty" to 10,

        "automatic renewal" to 15,
        "recurring charge" to 15,
        "non-refundable" to 15,
        "early termination fee" to 15,
        "prepayment penalty" to 15,
        "origination fee" to 15,
        "escrow shortage" to 15,
        "late payment penalty" to 15,
        "price adjustment" to 10,

        "sub-processor" to 15,
        "data residency" to 15,
        "indemnify" to 15,
        "hold harmless" to 15,
        "vesting schedule" to 10,
        "severance pay" to 10,

        "fob origin" to 15,
        "customs duties" to 15,
        "restocking fee" to 10,

        // === LOW RISK ===
        "subject to change" to 5,
        "cookies" to 5,
        "data collection" to 5,
        "service level agreement" to 5,
        "acceptable use" to 5,
        "warranty disclaimer" to 5,
        "force majeure" to 5,
        "amortization schedule" to 5,
        "apr" to 5,
        "zoning requirements" to 5,
        "manufacturer's warranty" to 5,
        "severability" to 5,
        "homeowners association" to 5,
        "hoa" to 5,
        "title insurance" to 5,
        "closing disclosure" to 5,
        "eula" to 5,
        "privacy policy" to 5
    )

    fun getCategory(keyword: String): String {
        return when (keyword.lowercase()) {

            // Banking
            "acceleration of debt", "collateral forfeiture", "deficiency judgment", "guaranty",
            "power of attorney", "balloon payment", "compounding interest", "right of offset",
            "cross-default", "garnishment", "variable rate hike",
            "prepayment penalty", "origination fee", "escrow shortage",
            "late payment penalty", "amortization schedule", "apr" -> "Banking & Loans"

            // Real Estate
            "specific performance", "contingency waiver", "lien on property", "right of first refusal",
            "earnest money forfeiture", "as-is condition", "encumbrance", "easement", "quitclaim",
            "zoning requirements", "homeowners association", "hoa",
            "title insurance", "closing disclosure" -> "Real Estate & Property"

            // Employment
            "work for hire", "non-compete", "non-solicitation",
            "liquidated damages", "intellectual property assignment",
            "indemnify", "hold harmless", "vesting schedule", "severance pay" -> "Professional & Employment"

            // IT
            "transfer of ownership", "irrevocable license", "third-party data sharing",
            "security breach waiver", "sub-processor", "data residency",
            "service level agreement", "acceptable use",
            "data collection", "cookies", "eula", "privacy policy" -> "IT & Software"

            // Consumer
            "excludes wear and tear", "void if tampered", "refurbished parts",
            "authorized service center only", "consequential damages",
            "deductible", "limited warranty", "manufacturer's warranty",
            "warranty disclaimer", "shipping and handling fees" -> "Consumer Goods & Warranty"

            // E-commerce
            "non-refundable", "restocking fee", "fob origin", "customs duties" -> "E-commerce & Retail"

            // Subscription
            "automatic renewal", "recurring charge", "price adjustment",
            "early termination fee" -> "Subscription Services"

            else -> "General Legal"
        }
    }
}