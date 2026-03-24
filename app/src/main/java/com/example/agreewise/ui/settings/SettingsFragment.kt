package com.example.agreewise.ui.settings

import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.FileProvider
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*
import com.bumptech.glide.Glide
import com.example.agreewise.LoginActivity
import com.example.agreewise.R
import com.example.agreewise.databinding.FragmentSettingsBinding
import com.example.agreewise.utils.ThemeManager
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class SettingsFragment : Fragment() {

    private var _binding: FragmentSettingsBinding? = null
    private val binding get() = _binding!!

    private val imagePickerLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == android.app.Activity.RESULT_OK) {
            result.data?.data?.let { imageUri ->
                uploadProfileImage(imageUri)
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSettingsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        loadUserProfile()
        setupDarkModeToggle()

        binding.btnBack.setOnClickListener {
            findNavController().navigate(R.id.navigation_home)
        }

        binding.btnLogout.setOnClickListener {
            FirebaseAuth.getInstance().signOut()
            val intent = Intent(requireContext(), LoginActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            requireActivity().finish()
        }

        binding.btnUpdateProfile.setOnClickListener {
            showUpdateProfileDialog()
        }

        binding.btnUploadProfile.setOnClickListener {
            showImagePickerDialog()
        }
    }

    private fun loadUserProfile() {
        val user = FirebaseAuth.getInstance().currentUser ?: return
        
        binding.settingsUserName.text = user.displayName ?: "AgreeWise User"
        binding.settingsUserEmail.text = user.email ?: "No email linked"

        // Try Firebase Auth photo URL first
        user.photoUrl?.let { photoUri ->
            if (isAdded) {
                Glide.with(requireContext())
                    .load(photoUri)
                    .circleCrop()
                    .placeholder(R.drawable.agreewise_transparentlogo)
                    .into(binding.settingsUserProfile)
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
                            .into(binding.settingsUserProfile)
                    }
                }

                override fun onCancelled(error: DatabaseError) {}
            })
        }
    }

    private fun setupDarkModeToggle() {
        val isDarkMode = ThemeManager.isDarkModeEnabled(requireContext())
        binding.switchDarkMode.isChecked = isDarkMode

        binding.switchDarkMode.setOnCheckedChangeListener { _, isChecked ->
            ThemeManager.setDarkMode(requireContext(), isChecked)
        }
    }

    private fun showUpdateProfileDialog() {
        val user = FirebaseAuth.getInstance().currentUser ?: return
        
        val dialogView = LayoutInflater.from(requireContext())
            .inflate(R.layout.dialog_update_profile, null)
        
        val nameEditText = dialogView.findViewById<EditText>(R.id.edit_name)
        nameEditText.setText(user.displayName ?: "")
        
        AlertDialog.Builder(requireContext())
            .setTitle("Update Profile")
            .setView(dialogView)
            .setPositiveButton("Update") { _, _ ->
                val newName = nameEditText.text.toString().trim()
                if (newName.isNotEmpty()) {
                    updateUserProfile(newName)
                } else {
                    Toast.makeText(requireContext(), "Name cannot be empty", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun updateUserProfile(newName: String) {
        val user = FirebaseAuth.getInstance().currentUser ?: return
        
        val profileUpdates = UserProfileChangeRequest.Builder()
            .setDisplayName(newName)
            .build()
        
        user.updateProfile(profileUpdates)
            .addOnCompleteListener { task ->
                if (task.isSuccessful && isAdded) {
                    binding.settingsUserName.text = newName
                    
                    val database = FirebaseDatabase.getInstance()
                        .getReference("users")
                        .child(user.uid)
                    database.child("displayName").setValue(newName)
                    
                    Toast.makeText(requireContext(), "Profile updated successfully", Toast.LENGTH_SHORT).show()
                } else if (isAdded) {
                    Toast.makeText(requireContext(), "Failed to update profile", Toast.LENGTH_SHORT).show()
                }
            }
    }

    private fun showImagePickerDialog() {
        val options = arrayOf("Choose from Gallery")
        
        AlertDialog.Builder(requireContext())
            .setTitle("Select Profile Picture")
            .setItems(options) { _, which ->
                when (which) {
                    0 -> openGallery()
                }
            }
            .show()
    }

    private fun openGallery() {
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        imagePickerLauncher.launch(intent)
    }

    private fun uploadProfileImage(imageUri: Uri) {
        val user = FirebaseAuth.getInstance().currentUser ?: return
        
        // Update Firebase Auth profile with the image URI
        val profileUpdates = UserProfileChangeRequest.Builder()
            .setPhotoUri(imageUri)
            .build()
        
        user.updateProfile(profileUpdates)
            .addOnCompleteListener { task ->
                if (task.isSuccessful && isAdded) {
                    // Save to Firebase Database for persistence across sign-ins
                    val database = FirebaseDatabase.getInstance()
                        .getReference("users")
                        .child(user.uid)
                    database.child("photoUrl").setValue(imageUri.toString())
                    
                    // Update UI
                    Glide.with(requireContext())
                        .load(imageUri)
                        .circleCrop()
                        .placeholder(R.drawable.agreewise_transparentlogo)
                        .into(binding.settingsUserProfile)
                    
                    Toast.makeText(requireContext(), "Profile picture updated successfully", Toast.LENGTH_SHORT).show()
                } else if (isAdded) {
                    Toast.makeText(requireContext(), "Failed to update profile picture", Toast.LENGTH_SHORT).show()
                }
            }
    }

    private fun getImageUriFromBitmap(bitmap: Bitmap): Uri {
        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val imageName = "profile_$timestamp.jpg"
        
        val storageDir = File(requireContext().getExternalFilesDir(Environment.DIRECTORY_PICTURES), "profile_images")
        if (!storageDir.exists()) {
            storageDir.mkdirs()
        }
        
        val imageFile = File(storageDir, imageName)
        
        return try {
            val fileOutputStream = FileOutputStream(imageFile)
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, fileOutputStream)
            fileOutputStream.close()
            
            FileProvider.getUriForFile(
                requireContext(),
                "${requireContext().packageName}.fileprovider",
                imageFile
            )
        } catch (e: IOException) {
            e.printStackTrace()
            // Fallback to a content URI
            Uri.parse("android.resource://com.example.agreewise/drawable/agreewise_transparentlogo")
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}