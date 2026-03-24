# AgreeWise for Xcode (iOS)

Open this project in **Xcode** to build and run the iOS version of AgreeWise.

## Open in Xcode

1. On a **Mac**, open **Xcode**.
2. **File → Open** and select:  
   **`AgreeWise.xcodeproj`**  
   (inside the `ios/AgreeWise` folder).
3. Choose the **AgreeWise** scheme and an iPhone simulator or device.
4. Press **Run** (⌘R).

## Gemini API key (for AI explanations)

To enable AI explanations, add your Gemini API key:

- In Xcode: select the **AgreeWise** target → **Info** tab → add key **`GEMINI_API_KEY`** (String) with your key.
- Or edit **AgreeWise/Info.plist** and add the same key under the root dictionary.

Without the key, local risk scoring and flagged keywords still work; only the Gemini call is skipped.

## Requirements

- Xcode 15+
- iOS 17.0+
- Swift 5
