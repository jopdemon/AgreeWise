package com.example.agreewise.ui.upload

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.bumptech.glide.Glide
import com.example.agreewise.R
import com.example.agreewise.databinding.FragmentUploadBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.itextpdf.kernel.pdf.PdfDocument
import com.itextpdf.kernel.pdf.PdfReader
import com.itextpdf.kernel.pdf.canvas.parser.PdfTextExtractor
import org.apache.poi.xwpf.usermodel.XWPFDocument
import java.io.InputStream

class UploadFragment : Fragment() {

    private var _binding: FragmentUploadBinding? = null
    private val binding get() = _binding!!
    private var selectedFileUri: Uri? = null
    private var selectedFileName: String? = null

    private val filePickerLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            selectedFileUri = result.data?.data
            selectedFileName = getFileName(result.data?.data)
            binding.textFileName.text = selectedFileName ?: "File selected"
            binding.buttonAnalyzeFile.isEnabled = true
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentUploadBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        loadUserProfile()

        binding.btnBack.setOnClickListener {
            findNavController().navigateUp()
        }

        binding.cardBrowse.setOnClickListener {
            val intent = Intent(Intent.ACTION_GET_CONTENT).apply {
                type = "*/*"
                val mimeTypes = arrayOf(
                    "application/pdf",
                    "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
                )
                putExtra(Intent.EXTRA_MIME_TYPES, mimeTypes)
            }
            filePickerLauncher.launch(intent)
        }

        binding.buttonAnalyzeFile.setOnClickListener {
            selectedFileUri?.let { uri ->
                extractTextAndNavigate(uri)
            }
        }
    }

    private fun extractTextAndNavigate(uri: Uri) {
        try {
            val contentResolver = requireContext().contentResolver
            val mimeType = contentResolver.getType(uri)
            val inputStream = contentResolver.openInputStream(uri)

            val extractedText = when {
                mimeType == "application/pdf" -> extractTextFromPdf(inputStream)
                mimeType?.contains("wordprocessingml.document") == true -> extractTextFromDocx(inputStream)
                else -> ""
            }

            if (extractedText.isNotBlank()) {
                val fileName = selectedFileName ?: "Uploaded File"
                val bundle = Bundle().apply {
                    putString("contract_text", extractedText)
                    putString("title", fileName)
                }
                findNavController().navigate(R.id.action_upload_to_results, bundle)
            } else {
                Toast.makeText(requireContext(), "Could not extract text from file", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            Log.e("UploadFragment", "Error extracting text", e)
            Toast.makeText(requireContext(), "Error: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun extractTextFromPdf(inputStream: InputStream?): String {
        val reader = PdfReader(inputStream)
        val pdfDoc = PdfDocument(reader)
        val stringBuilder = StringBuilder()
        for (i in 1..pdfDoc.numberOfPages) {
            stringBuilder.append(PdfTextExtractor.getTextFromPage(pdfDoc.getPage(i)))
        }
        pdfDoc.close()
        return stringBuilder.toString()
    }

    private fun extractTextFromDocx(inputStream: InputStream?): String {
        val document = XWPFDocument(inputStream)
        val stringBuilder = StringBuilder()
        document.paragraphs.forEach { paragraph ->
            stringBuilder.append(paragraph.text).append("\n")
        }
        document.close()
        return stringBuilder.toString()
    }

    private fun getFileName(uri: Uri?): String? {
        uri ?: return null
        val cursor = requireContext().contentResolver.query(uri, null, null, null, null)
        return cursor?.use {
            if (it.moveToFirst()) {
                val nameIndex = it.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                if (nameIndex != -1) it.getString(nameIndex) else uri.lastPathSegment
            } else {
                uri.lastPathSegment
            }
        } ?: uri.lastPathSegment
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
        _binding = null
    }
}