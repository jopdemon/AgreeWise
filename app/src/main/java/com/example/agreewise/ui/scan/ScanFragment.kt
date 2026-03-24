package com.example.agreewise.ui.scan

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.bumptech.glide.Glide
import com.example.agreewise.R
import com.example.agreewise.databinding.FragmentScanBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import com.google.mlkit.vision.documentscanner.GmsDocumentScanning
import androidx.activity.result.IntentSenderRequest
import com.google.mlkit.vision.documentscanner.GmsDocumentScannerOptions
import com.google.mlkit.vision.documentscanner.GmsDocumentScanningResult
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ScanFragment : Fragment() {

    private var _binding: FragmentScanBinding? = null
    private val binding get() = _binding!!
    
    private var isScanningActive = false

    private val scannerLauncher = registerForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) { result ->
        if (result.resultCode == android.app.Activity.RESULT_OK) {
            val scanResult = GmsDocumentScanningResult.fromActivityResultIntent(result.data)
            scanResult?.pages?.get(0)?.imageUri?.let { uri ->
                processScannedImage(uri)
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentScanBinding.inflate(inflater, container, false)
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

        binding.btnShutter.setOnClickListener {
            startScanning()
        }
        
        // Auto-start scanner for better UX
        startScanning()
    }

    private fun startScanning() {
        isScanningActive = true
        val options = GmsDocumentScannerOptions.Builder()
            .setScannerMode(GmsDocumentScannerOptions.SCANNER_MODE_FULL)
            .setResultFormats(GmsDocumentScannerOptions.RESULT_FORMAT_JPEG)
            .setGalleryImportAllowed(true)
            .build()

        GmsDocumentScanning.getClient(options)
            .getStartScanIntent(requireActivity())
            .addOnSuccessListener { intentSender ->
                scannerLauncher.launch(IntentSenderRequest.Builder(intentSender).build())
            }
            .addOnFailureListener { e ->
                isScanningActive = false
                Toast.makeText(requireContext(), "Failed to start scanner: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun processScannedImage(uri: Uri) {
        val image = InputImage.fromFilePath(requireContext(), uri)
        val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

        recognizer.process(image)
            .addOnSuccessListener { visionText ->
                val extractedText = visionText.text
                if (extractedText.isNotBlank()) {
                    val title = extractTitleFromText(extractedText)
                    val bundle = Bundle().apply {
                        putString("contract_text", extractedText)
                        putString("title", title)
                    }
                    findNavController().navigate(R.id.action_scan_to_results, bundle)
                } else {
                    Toast.makeText(requireContext(), "No text detected. Try again.", Toast.LENGTH_LONG).show()
                }
            }
            .addOnFailureListener { e ->
                Toast.makeText(requireContext(), "OCR Error: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }
    
    private fun extractTitleFromText(text: String): String {
        val lines = text.lines().filter { it.trim().isNotEmpty() }
        if (lines.isEmpty()) {
            val timestamp = SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault()).format(Date())
            return "Scanned Document - $timestamp"
        }
        
        val firstLine = lines[0].trim()
        val title = when {
            firstLine.length > 50 -> firstLine.take(47) + "..."
            firstLine.length < 5 && lines.size > 1 -> lines[1].trim().take(50)
            else -> firstLine
        }
        
        return title.ifEmpty {
            val timestamp = SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault()).format(Date())
            "Scanned Document - $timestamp"
        }
    }

    private fun loadUserProfile() {
        val user = FirebaseAuth.getInstance().currentUser ?: return
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

    override fun onDestroyView() {
        super.onDestroyView()
        stopScanning()
        _binding = null
    }
    
    private fun stopScanning() {
        isScanningActive = false
        // Google Document Scanner handles its own cleanup
        // This flag helps track scanning state for debugging
    }
}