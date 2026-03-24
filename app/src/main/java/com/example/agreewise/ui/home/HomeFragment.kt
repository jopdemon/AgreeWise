package com.example.agreewise.ui.home

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.bumptech.glide.Glide
import com.example.agreewise.R
import com.example.agreewise.databinding.FragmentHomeBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        loadUserProfile()

        binding.cardPaste.setOnClickListener {
            findNavController().navigate(R.id.action_home_to_paste)
        }

        binding.cardUpload.setOnClickListener {
            findNavController().navigate(R.id.action_home_to_upload)
        }

        binding.btnContractHistory.setOnClickListener {
            findNavController().navigate(R.id.navigation_history)
        }

        // Updated to navigate to AR Scan instead of standard Scan
        binding.cardScan.setOnClickListener {
            findNavController().navigate(R.id.action_home_to_ar_scan)
        }

        binding.userProfile.setOnClickListener {
            findNavController().navigate(R.id.navigation_settings)
        }
    }

    private fun loadUserProfile() {
        val user = FirebaseAuth.getInstance().currentUser ?: return
        
        Log.d(TAG, "Loading profile for user: ${user.displayName}")
        Log.d(TAG, "Photo URL: ${user.photoUrl}")
        
        // Try Firebase Auth photo URL first
        user.photoUrl?.let { photoUrl ->
            Log.d(TAG, "Loading profile picture from Firebase Auth")
            if (isAdded) {
                Glide.with(requireContext())
                    .load(photoUrl)
                    .circleCrop()
                    .placeholder(R.drawable.agreewise_transparentlogo)
                    .error(R.drawable.agreewise_transparentlogo)
                    .into(binding.userProfile)
            }
        } ?: run {
            // Fallback to database
            Log.d(TAG, "No photo URL in Firebase Auth, checking database")
            val userId = user.uid
            val database = FirebaseDatabase.getInstance().getReference("users").child(userId)
            
            database.addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val photoUrl = snapshot.child("photoUrl").getValue(String::class.java)
                    if (!photoUrl.isNullOrEmpty() && isAdded) {
                        Log.d(TAG, "Loading profile picture from database: $photoUrl")
                        Glide.with(requireContext())
                            .load(photoUrl)
                            .circleCrop()
                            .placeholder(R.drawable.agreewise_transparentlogo)
                            .error(R.drawable.agreewise_transparentlogo)
                            .into(binding.userProfile)
                    } else {
                        Log.w(TAG, "No photo URL found, using placeholder")
                        if (isAdded) {
                            binding.userProfile.setImageResource(R.drawable.agreewise_transparentlogo)
                        }
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    Log.e(TAG, "Error loading profile from database: ${error.message}", error.toException())
                    if (isAdded) {
                        binding.userProfile.setImageResource(R.drawable.agreewise_transparentlogo)
                    }
                }
            })
        }
    }
    
    companion object {
        private const val TAG = "HomeFragment"
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}