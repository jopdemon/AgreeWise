package com.example.agreewise.ui.paste

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.bumptech.glide.Glide
import com.example.agreewise.R
import com.example.agreewise.databinding.FragmentPasteBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class PasteFragment : Fragment() {

    private var _binding: FragmentPasteBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentPasteBinding.inflate(inflater, container, false)
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

        binding.buttonAnalyze.setOnClickListener {
            val text = binding.editTextPaste.text.toString()
            if (text.isNotBlank()) {
                val timestamp = SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault()).format(Date())
                val bundle = Bundle().apply {
                    putString("contract_text", text)
                    putString("title", "Pasted Text - $timestamp")
                }
                findNavController().navigate(R.id.action_paste_to_results, bundle)
            }
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
        _binding = null
    }
}