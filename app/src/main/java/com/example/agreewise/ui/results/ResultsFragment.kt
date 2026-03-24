package com.example.agreewise.ui.results

import android.content.res.ColorStateList
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.bumptech.glide.Glide
import com.example.agreewise.R
import com.example.agreewise.databinding.FragmentResultsBinding
import com.example.agreewise.ml.RiskScanner
import com.example.agreewise.model.HistoryItem
import com.example.agreewise.network.Content
import com.example.agreewise.network.GeminiRequest
import com.example.agreewise.network.Part
import com.example.agreewise.network.RetrofitInstance
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.ktx.Firebase
import com.google.firebase.remoteconfig.ktx.remoteConfig
import com.google.firebase.remoteconfig.ktx.remoteConfigSettings
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ResultsFragment : Fragment() {

    private var _binding: FragmentResultsBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentResultsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        loadUserProfile()

        binding.btnBack.setOnClickListener {
            findNavController().navigateUp()
        }

        binding.userProfile.setOnClickListener {
            findNavController().navigate(R.id.navigation_settings)
        }

        val textToAnalyze = arguments?.getString("contract_text")
        val title = arguments?.getString("title") ?: "New Analysis"
        val explanation = arguments?.getString("explanation")
        val score = arguments?.getInt("score", 0) ?: 0
        val riskLevel = arguments?.getString("risk_level")

        Log.d("ResultsFragment", "Text to analyze: '$textToAnalyze'")
        Log.d("ResultsFragment", "Title: '$title'")
        Log.d("ResultsFragment", "Explanation: '$explanation'")
        Log.d("ResultsFragment", "Score: $score")
        Log.d("ResultsFragment", "Risk Level: '$riskLevel'")

        if (textToAnalyze != null && textToAnalyze.isNotBlank()) {
            if (explanation != null && score > 0) {
                // This is from history, display the saved results
                displaySavedResults(title, textToAnalyze, score, riskLevel ?: "Low Risk Detected", explanation)
            } else {
                // This is a new analysis, analyze the text
                analyzeContract(textToAnalyze, title)
            }
        } else {
            // Show a more user-friendly message and navigate back
            binding.textExplanation.text = "No text to analyze. Please go back and try again."
            binding.textRiskScore.text = "0"
            binding.riskProgress.progress = 0
            binding.textRiskStatus.text = "No Analysis"
            binding.textFlaggedList.text = ""

            // Auto-navigate back after a delay
            view?.postDelayed({
                if (isAdded) {
                    findNavController().navigateUp()
                }
            }, 2000)
        }

    }

    private fun analyzeContract(text: String, title: String) {
        val analysisResult = RiskScanner.scanText(text)

        binding.textRiskScore.text = analysisResult.score.toString()
        binding.riskProgress.progress = analysisResult.score

        val color = when {
            analysisResult.score > 60 -> requireContext().getColor(R.color.risk_high)
            analysisResult.score > 30 -> requireContext().getColor(R.color.risk_medium)
            else -> requireContext().getColor(R.color.risk_low)
        }

        binding.textRiskStatus.text = if (analysisResult.score > 60) "High Risk Detected!" 
                                    else if (analysisResult.score > 30) "Medium Risk Detected" 
                                    else "Low Risk Detected"
        
        binding.textRiskStatus.setTextColor(color)
        binding.textRiskScore.setTextColor(color)
        binding.riskProgress.progressTintList = ColorStateList.valueOf(color)

        binding.textFlaggedList.text = if (analysisResult.flaggedKeywords.isEmpty()) {
            "No specific risky keywords found."
        } else {
            analysisResult.flaggedKeywords.joinToString("\n") { "• ${it.replaceFirstChar { char -> char.uppercase() }}" }
        }

        if (analysisResult.score > 20) {
            callGeminiApi(text, title, analysisResult.score, binding.textRiskStatus.text.toString(), analysisResult.flaggedKeywords)
        } else {
            binding.textExplanation.text = getString(R.string.no_high_risk_clauses)
            saveToFirebase(title, text, binding.textRiskStatus.text.toString(), analysisResult.score, getString(R.string.no_high_risk_clauses))
        }
    }

    private fun callGeminiApi(clause: String, title: String, score: Int, level: String, keywords: List<String>) {
        val remoteConfig = Firebase.remoteConfig
        
        // Use developer mode settings to fetch frequently during testing
        val configSettings = remoteConfigSettings {
            minimumFetchIntervalInSeconds = 0
        }
        remoteConfig.setConfigSettingsAsync(configSettings)
        
        binding.textExplanation.text = "Fetching configuration..."

        remoteConfig.fetchAndActivate().addOnCompleteListener { task ->
            if (task.isSuccessful) {
                val geminiApiKey = remoteConfig.getString("gemini_api_key")
                Log.d("RemoteConfig", "Key fetched: ${if (geminiApiKey.isNotBlank()) "YES" else "NO"}")

                if (geminiApiKey.isBlank()) {
                    binding.textExplanation.text = "API Key is empty in Remote Config. Check parameter name 'gemini_api_key'."
                    return@addOnCompleteListener
                }

                binding.textExplanation.text = getString(R.string.getting_ai_explanation)

                viewLifecycleOwner.lifecycleScope.launch {
                    val prompt = getString(R.string.gemini_prompt, keywords.joinToString(), clause)
                    val request = GeminiRequest(contents = listOf(Content(parts = listOf(Part(prompt)))))

                    try {
                        val response = RetrofitInstance.api.generateContent(geminiApiKey, request)
                        if (response.isSuccessful) {
                            val explanation = response.body()?.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
                            val finalExp = explanation ?: getString(R.string.no_explanation_received)
                            binding.textExplanation.text = finalExp
                            saveToFirebase(title, clause, level, score, finalExp)
                        } else {
                            val errorMsg = "AI API Error: ${response.code()} ${response.message()}"
                            binding.textExplanation.text = errorMsg
                            Log.e("GeminiAPI", "Error: ${response.errorBody()?.string()}")
                            saveToFirebase(title, clause, level, score, errorMsg)
                        }
                    } catch (e: Exception) {
                        val errorMsg = getString(R.string.network_error_message, e.message)
                        binding.textExplanation.text = errorMsg
                        Log.e("GeminiAPI", "Network Exception", e)
                        saveToFirebase(title, clause, level, score, errorMsg)
                    }
                }
            } else {
                val errorMsg = "Failed to fetch Remote Config: ${task.exception?.message}"
                binding.textExplanation.text = errorMsg
                Log.e("RemoteConfig", "Fetch failed", task.exception)
                saveToFirebase(title, clause, level, score, errorMsg)
            }
        }
    }

    private fun saveToFirebase(title: String, content: String, level: String, score: Int, explanation: String) {
        val userId = FirebaseAuth.getInstance().currentUser?.uid
        if (userId == null) {
            Log.e("ResultsFragment", "Cannot save history: User not authenticated")
            return
        }
        
        Log.d("ResultsFragment", "Saving history for user: $userId")
        val database = FirebaseDatabase.getInstance().getReference("history").child(userId)
        
        val date = SimpleDateFormat("MMMM d, yyyy", Locale.getDefault()).format(Date())
        val itemId = database.push().key
        if (itemId == null) {
            Log.e("ResultsFragment", "Failed to generate item ID")
            return
        }
        
        val item = HistoryItem(itemId, title, date, score, level, content, explanation)
        Log.d("ResultsFragment", "Saving item: $title, Score: $score, Level: $level")
        
        database.child(itemId).setValue(item)
            .addOnSuccessListener {
                Log.d("ResultsFragment", "History saved successfully: $itemId")
            }
            .addOnFailureListener { e ->
                Log.e("ResultsFragment", "Failed to save history: ${e.message}", e)
            }
    }

    private fun loadUserProfile() {
        val user = FirebaseAuth.getInstance().currentUser ?: return
        
        // Try Firebase Auth photo URL first
        user.photoUrl?.let { photoUri ->
            if (isAdded) {
                Glide.with(requireContext())
                    .load(photoUri)
                    .circleCrop()
                    .placeholder(R.drawable.agreewise_transparentlogo)
                    .error(R.drawable.agreewise_transparentlogo)
                    .into(binding.userProfile)
            }
        } ?: run {
            // Fallback to database
            val userId = user.uid
            val database = FirebaseDatabase.getInstance().getReference("users").child(userId)
            
            database.addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val photoUrl = snapshot.child("photoUrl").getValue(String::class.java)
                    if (!photoUrl.isNullOrEmpty() && isAdded) {
                        Glide.with(requireContext())
                            .load(photoUrl)
                            .circleCrop()
                            .placeholder(R.drawable.agreewise_transparentlogo)
                            .error(R.drawable.agreewise_transparentlogo)
                            .into(binding.userProfile)
                    }
                }

                override fun onCancelled(error: DatabaseError) {}
            })
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        stopAnalysis()
        arguments?.clear()
        _binding = null
    }
    
    private fun stopAnalysis() {
        // Cancel any ongoing analysis or API calls
        // Clear the analysis state
        try {
            // Reset UI to default state
            binding.textRiskScore.text = "0"
            binding.riskProgress.progress = 0
            binding.textRiskStatus.text = "No Analysis"
            binding.textFlaggedList.text = ""
            binding.textExplanation.text = ""
        } catch (e: Exception) {
            // Ignore cleanup errors
        }
    }

    private fun displaySavedResults(title: String, content: String, score: Int, riskLevel: String, explanation: String) {
        binding.textRiskScore.text = score.toString()
        binding.riskProgress.progress = score
        binding.textRiskStatus.text = riskLevel
        binding.textExplanation.text = explanation

        val color = when {
            score > 60 -> requireContext().getColor(R.color.risk_high)
            score > 30 -> requireContext().getColor(R.color.risk_medium)
            else -> requireContext().getColor(R.color.risk_low)
        }

        binding.textRiskStatus.setTextColor(color)
        binding.textRiskScore.setTextColor(color)
        binding.riskProgress.progressTintList = ColorStateList.valueOf(color)

        // Get flagged keywords from the content
        val analysisResult = RiskScanner.scanText(content)
        binding.textFlaggedList.text = if (analysisResult.flaggedKeywords.isEmpty()) {
            "No specific risky keywords found."
        } else {
            analysisResult.flaggedKeywords.joinToString("\n") { "• ${it.replaceFirstChar { char -> char.uppercase() }}" }
        }
    }
}