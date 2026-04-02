package com.example.agreewise.ui.history

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.example.agreewise.R
import com.example.agreewise.databinding.FragmentHistoryBinding
import com.example.agreewise.model.HistoryItem
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import java.util.Locale

class HistoryFragment : Fragment() {

    private var _binding: FragmentHistoryBinding? = null
    private val binding get() = _binding!!

    private var allHistoryItems = listOf<HistoryItem>()
    private lateinit var historyAdapter: HistoryAdapter
    private var valueEventListener: ValueEventListener? = null

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
        setupSearch()
        loadHistoryFromFirebase()
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
                    } else if (isAdded) {
                        binding.userProfile.setImageResource(R.drawable.agreewise_transparentlogo)
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

    private fun setupRecyclerView() {
        historyAdapter = HistoryAdapter(
            items = emptyList(),
            onDeleteClick = { item -> confirmDelete(item) },
            onItemClick = { item -> navigateToResults(item) }
        )
        binding.recyclerHistory.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerHistory.adapter = historyAdapter
    }

    private fun setupSearch() {
        binding.editSearch.addTextChangedListener { text ->
            val query = text?.toString()?.lowercase(Locale.getDefault()) ?: ""
            filterList(query)
        }
    }

    private fun filterList(query: String) {
        val filteredList = if (query.isEmpty()) {
            allHistoryItems
        } else {
            allHistoryItems.filter {
                it.title.lowercase(Locale.getDefault()).contains(query) ||
                it.content.lowercase(Locale.getDefault()).contains(query)
            }
        }
        
        historyAdapter.updateList(filteredList)
        
        if (filteredList.isEmpty()) {
            binding.textEmptyHistory.visibility = View.VISIBLE
            binding.recyclerHistory.visibility = View.GONE
        } else {
            binding.textEmptyHistory.visibility = View.GONE
            binding.recyclerHistory.visibility = View.VISIBLE
        }
    }

    private fun confirmDelete(item: HistoryItem) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Delete Contract")
            .setMessage("Are you sure you want to permanently delete '${item.title}' from your history?")
            .setPositiveButton("Delete") { _, _ ->
                deleteItemFromFirebase(item)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun deleteItemFromFirebase(item: HistoryItem) {
        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return
        FirebaseDatabase.getInstance().getReference("history").child(userId).child(item.id)
            .removeValue()
            .addOnFailureListener {
                Log.e("HistoryFragment", "Failed to delete item: ${it.message}")
            }
    }

    private fun navigateToResults(historyItem: HistoryItem) {
        val bundle = Bundle().apply {
            putString("contract_text", historyItem.content)
            putString("title", historyItem.title)
            putString("explanation", historyItem.explanation)
            putInt("score", historyItem.riskScore)
            putString("risk_level", historyItem.riskLevel)
        }
        findNavController().navigate(R.id.action_history_to_results, bundle)
    }

    private fun loadHistoryFromFirebase() {
        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return
        val database = FirebaseDatabase.getInstance().getReference("history").child(userId)

        valueEventListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val historyList = mutableListOf<HistoryItem>()
                for (itemSnapshot in snapshot.children) {
                    val item = itemSnapshot.getValue(HistoryItem::class.java)
                    item?.let { historyList.add(it) }
                }
                
                if (isAdded) {
                    allHistoryItems = historyList.reversed() // newest first
                    // Re-apply current search filter to new data
                    val currentQuery = binding.editSearch.text.toString().lowercase(Locale.getDefault())
                    filterList(currentQuery)
                }
            }

            override fun onCancelled(error: DatabaseError) {}
        }
        
        // Use addValueEventListener, but we will remove it in onDestroyView
        database.addValueEventListener(valueEventListener!!)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        
        // Prevent listener memory leak and multi-firing bugs
        val userId = FirebaseAuth.getInstance().currentUser?.uid
        if (userId != null && valueEventListener != null) {
            FirebaseDatabase.getInstance().getReference("history").child(userId)
                .removeEventListener(valueEventListener!!)
        }
        
        _binding = null
    }
}