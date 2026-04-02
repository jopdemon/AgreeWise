package com.example.agreewise.ui.home

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.bumptech.glide.Glide
import com.example.agreewise.R
import com.example.agreewise.databinding.FragmentHomeBinding
import com.google.ar.core.ArCoreApk
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

        binding.cardScan.setOnClickListener {
            checkArAvailability()
        }

        binding.userProfile.setOnClickListener {
            findNavController().navigate(R.id.navigation_settings)
        }
    }

    private fun checkArAvailability() {
        val availability = ArCoreApk.getInstance().checkAvailability(requireContext())
        if (availability.isTransient) {
            // Re-query after 200ms
            Handler(Looper.getMainLooper()).postDelayed({
                checkArAvailability()
            }, 200)
            return
        }

        if (availability.isSupported) {
            findNavController().navigate(R.id.action_home_to_ar_scan)
        } else {
            // Unsupported or needs install - Fallback to standard scanner
            Log.d(TAG, "AR not supported: $availability. Falling back to standard scanner.")
            findNavController().navigate(R.id.action_home_to_scan)
        }
    }

    private fun loadUserProfile() {
        val user = FirebaseAuth.getInstance().currentUser ?: return
        
        user.photoUrl?.let { photoUrl ->
            if (isAdded) {
                Glide.with(requireContext())
                    .load(photoUrl)
                    .circleCrop()
                    .placeholder(R.drawable.agreewise_transparentlogo)
                    .error(R.drawable.agreewise_transparentlogo)
                    .into(binding.userProfile)
            }
        } ?: run {
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
                    } else {
                        if (isAdded) {
                            binding.userProfile.setImageResource(R.drawable.agreewise_transparentlogo)
                        }
                    }
                }

                override fun onCancelled(error: DatabaseError) {
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