package com.example.agreewise.ui.results

import android.content.res.ColorStateList
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
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
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ResultsFragment : Fragment() {

    private var _binding: FragmentResultsBinding? = null
    private val binding get() = _binding!!

    // API Key Rotation List
    private val fallbackGeminiKeys = listOf(
        "AIzaSyAhdQwTg7ol5b9VgEa-uvWZf0DkKVt4-90",
        "AIzaSyDnr8dODs1cClygT8pdVRzoOvGeCJ-iWyc",
        "AIzaSyC6ESyOeqvytKnwXF0zXtRZfxjbXA6sj3M",
        "AIzaSyAPIjsCGtOS-j_EJs3xoJ4GzSns0wR9oD0",
        "AIzaSyB9IdATTKO2j1eTgr4MD908ufk30uJgs94"
    )

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

        if (textToAnalyze != null && textToAnalyze.isNotBlank()) {
            if (explanation != null) {
                displaySavedResults(title, textToAnalyze, score, riskLevel ?: "Low Risk Detected", explanation)
            } else {
                analyzeContract(textToAnalyze, title)
            }
        } else {
            binding.textExplanation.text = "No text to analyze. Please go back and try again."
            view?.postDelayed({
                if (isAdded) findNavController().navigateUp()
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

        callGeminiApi(text, title, analysisResult.score, binding.textRiskStatus.text.toString(), analysisResult.flaggedKeywords)
    }

    private fun callGeminiApi(clause: String, title: String, score: Int, level: String, keywords: List<String>) {
        val remoteConfig = Firebase.remoteConfig
        val configSettings = remoteConfigSettings { minimumFetchIntervalInSeconds = 0 }
        remoteConfig.setConfigSettingsAsync(configSettings)
        
        binding.textExplanation.text = "Initializing API Rotation..."

        remoteConfig.fetchAndActivate().addOnCompleteListener { task ->
            if (_binding == null) return@addOnCompleteListener

            val firebaseKey = if (task.isSuccessful) remoteConfig.getString("gemini_api_key") else ""
            
            val keysToTry = mutableListOf<String>()
            if (firebaseKey.isNotBlank()) keysToTry.add(firebaseKey)
            fallbackGeminiKeys.forEach { if (it != firebaseKey) keysToTry.add(it) }

            viewLifecycleOwner.lifecycleScope.launch {
                if (_binding == null) return@launch

                binding.textExplanation.text = getString(R.string.getting_ai_explanation)
                val prompt = getString(R.string.gemini_prompt, keywords.joinToString(), clause)
                val request = GeminiRequest(contents = listOf(Content(parts = listOf(Part(prompt)))))

                var apiSuccess = false
                var lastErrorMessage = ""

                for ((index, key) in keysToTry.withIndex()) {
                    try {
                        Log.d("GeminiAPI", "Attempting request with key ${index + 1}/${keysToTry.size}")
                        val response = RetrofitInstance.api.generateContent(key, request)
                        
                        if (response.isSuccessful) {
                            val fullResponse = response.body()?.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text ?: ""
                            if (_binding != null) {
                                handleSuccessfulResponse(fullResponse, clause, title, score, level)
                                apiSuccess = true
                            }
                            break
                        } else {
                            val errorCode = response.code()
                            lastErrorMessage = "Error $errorCode: ${response.message()}"
                            Log.e("GeminiAPI", "Key ${index + 1} failed: $lastErrorMessage")
                            if (errorCode == 429 || errorCode == 401 || errorCode == 403) {
                                continue
                            } else {
                                continue
                            }
                        }
                    } catch (e: Exception) {
                        lastErrorMessage = e.message ?: "Unknown network error"
                        Log.e("GeminiAPI", "Exception with key ${index + 1}: $lastErrorMessage")
                        continue
                    }
                }

                if (!apiSuccess && _binding != null) {
                    val finalError = "All API keys failed. Last error: $lastErrorMessage"
                    binding.textExplanation.text = finalError
                    saveToFirebase(title, clause, level, score, finalError)
                }
            }
        }
    }

    private fun handleSuccessfulResponse(fullResponse: String, clause: String, title: String, score: Int, level: String) {
        val lines = fullResponse.lines()
        val generatedTitle = if (lines.isNotEmpty() && lines.first().isNotBlank()) {
            lines.first().replace("**", "").replace("#", "").trim()
        } else title
        
        val rawExplanation = if (lines.size > 1) {
            lines.drop(1).joinToString("\n").trim()
        } else {
            fullResponse
        }

        val finalExp = if (rawExplanation.isNotBlank()) rawExplanation else getString(R.string.no_explanation_received)
        
        binding.textExplanation.text = finalExp
        binding.textTopTitle.text = generatedTitle
        
        saveToFirebase(generatedTitle, clause, level, score, finalExp)
    }

    private fun saveToFirebase(title: String, content: String, level: String, score: Int, explanation: String) {
        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return
        val database = FirebaseDatabase.getInstance().getReference("history").child(userId)
        val date = SimpleDateFormat("MMMM d, yyyy", Locale.getDefault()).format(Date())
        val itemId = database.push().key ?: return
        
        val item = HistoryItem(itemId, title, date, score, level, content, explanation)
        database.child(itemId).setValue(item)
    }

    private fun loadUserProfile() {
        val user = FirebaseAuth.getInstance().currentUser ?: return
        user.photoUrl?.let { photoUri ->
            if (isAdded) Glide.with(requireContext()).load(photoUri).circleCrop().into(binding.userProfile)
        }
    }

    private fun displaySavedResults(title: String, content: String, score: Int, riskLevel: String, explanation: String) {
        binding.textTopTitle.text = title
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

        val analysisResult = RiskScanner.scanText(content)
        binding.textFlaggedList.text = analysisResult.flaggedKeywords.joinToString("\n") { "• ${it.replaceFirstChar { c -> c.uppercase() }}" }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}