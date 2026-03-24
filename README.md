# AgreeWise - Smart Contract Analyzer

AgreeWise is an Android application that serves as a smart contract-reading assistant. It helps everyday people understand complex legal terms before they agree to a contract, terms of service, or privacy policy.

## Core Problem Solved

Most people do not read or understand the terms and conditions they agree to online, which can lead to:

- Hidden subscription traps
- Data privacy abuse
- Unfair liability clauses

AgreeWise mitigates these risks by translating complex legal language into simple explanations, risk warnings, and credibility indicators.

## Core Features (MVP)

1.  **Local ML Risk Scanner:** An offline-capable engine that instantly scans contract text for risky keywords and calculates a risk score.
2.  **AI-Powered Clause Explanation:** For contracts deemed high-risk, the app utilizes the Gemini API to provide a detailed, human-friendly explanation of the flagged clauses and their potential implications.

## Architecture

AgreeWise is built on a **hybrid AI + ML architecture** to ensure reliability and efficiency.

1.  **Layer 1 - Local Risk Scanner (Offline):** A rule-based machine learning model (`/app/src/main/java/com/example/agreewise/ml`) that performs initial keyword detection and risk scoring. This ensures the app is always functional, even without an internet connection.
2.  **Layer 2 - AI Explanation (Online):** If the local scan determines a high risk score, the app makes a network call to the Gemini API for a more nuanced and detailed analysis. This makes the system intelligent and provides deeper insights without being fully dependent on an API for its core functionality.

### System Flow

1.  User pastes contract text into the app.
2.  The app's local `RiskScanner` calculates a risk score and identifies flagged keywords.
3.  The risk score and flagged keywords are immediately displayed.
4.  **If** the risk score is above a predefined threshold (currently 20), the app calls the Gemini API.
5.  The AI-generated explanation is then displayed to the user.

## Setup

To build and run this project, you will need to configure your Firebase and Gemini API keys.

### 1. Firebase Setup

1.  Create a new project in the [Firebase Console](https://console.firebase.google.com/).
2.  Add an Android app to your Firebase project with the package name `com.example.agreewise`.
3.  Download the `google-services.json` file provided by Firebase.
4.  Place the downloaded `google-services.json` file into the **`app/`** directory of this project.

### 2. Gemini API Key Setup

The Gemini API key is managed securely through Firebase Remote Config.

1.  In the Firebase Console, navigate to the **Remote Config** section (under the "Release & Monitor" group).
2.  Click **"Create parameter"**.
3.  Set the **Parameter name** to exactly `gemini_api_key`.
4.  In the **Default value** field, paste your actual Gemini API key.
5.  Click **Save** and then **Publish changes**.

## Key Project Files

-   **`MainActivity.kt`**: The main screen of the application, which contains the core UI logic and orchestrates the calls to the ML and AI components.
-   **`/app/src/main/java/com/example/agreewise/ml`**: This package contains the local, rule-based ML model.
    -   `KeywordDatabase.kt`: Stores the risky keywords and their associated weights.
    -   `ScoreCalculator.kt`: Calculates a risk score from the input text.
    -   `RiskScanner.kt`: The main entry point for the local analysis.
-   **`/app/src/main/java/com/example/agreewise/network`**: This package handles all network communication with the Gemini API.
    -   `GeminiApiService.kt`: The Retrofit interface defining the API endpoint.
    -   `RetrofitInstance.kt`: Creates and configures the Retrofit client, including timeouts.
    -   Data classes (`GeminiRequest.kt`, `GeminiResponse.kt`): Defines the structure of the data sent to and received from the API.
