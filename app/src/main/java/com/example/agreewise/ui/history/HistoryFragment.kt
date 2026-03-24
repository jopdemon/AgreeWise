package com.example.agreewise.ui.history

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.example.agreewise.R
import com.example.agreewise.databinding.FragmentHistoryBinding
import com.example.agreewise.model.HistoryItem
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class HistoryFragment : Fragment() {

    private var _binding: FragmentHistoryBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHistoryBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.btnBack.setOnClickListener {
            findNavController().navigate(R.id.navigation_home)
        }

        binding.userProfile.setOnClickListener {
            findNavController().navigate(R.id.navigation_settings)
        }

        loadUserProfile()
        setupRecyclerView()
        loadHistoryFromFirebase()
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
        private const val TAG = "HistoryFragment"
    }

    private fun setupRecyclerView() {
        binding.recyclerHistory.layoutManager = LinearLayoutManager(requireContext())
    }

    private fun loadHistoryFromFirebase() {
        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return
        val database = FirebaseDatabase.getInstance().getReference("history").child(userId)

        database.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val historyList = mutableListOf<HistoryItem>()
                for (itemSnapshot in snapshot.children) {
                    val item = itemSnapshot.getValue(HistoryItem::class.java)
                    item?.let { historyList.add(it) }
                }
                
                if (isAdded) {
                    if (historyList.isEmpty()) {
                        binding.textEmptyHistory.visibility = View.VISIBLE
                        binding.recyclerHistory.visibility = View.GONE
                    } else {
                        binding.textEmptyHistory.visibility = View.GONE
                        binding.recyclerHistory.visibility = View.VISIBLE
                        binding.recyclerHistory.adapter = HistoryAdapter(historyList.reversed()) { historyItem ->
            // Navigate to ResultsFragment with the history item data
            val bundle = Bundle().apply {
                putString("contract_text", historyItem.content)
                putString("title", historyItem.title)
                putString("explanation", historyItem.explanation)
                putInt("score", historyItem.riskScore)
                putString("risk_level", historyItem.riskLevel)
            }
            findNavController().navigate(R.id.action_history_to_results, bundle)
        }
                    }
                }
            }

            override fun onCancelled(error: DatabaseError) {
            }
        })
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}