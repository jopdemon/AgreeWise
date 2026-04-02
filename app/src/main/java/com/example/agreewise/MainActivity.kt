package com.example.agreewise

import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.setupWithNavController
import com.example.agreewise.databinding.ActivityMainBinding
import com.google.android.material.bottomnavigation.BottomNavigationView

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val navView: BottomNavigationView = binding.bottomNav

        // More robust way to find NavController when using FragmentContainerView
        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        val navController = navHostFragment.navController
        
        // Add listener to stop scanning sessions and manage Navbar visibility
        navController.addOnDestinationChangedListener { _, destination, _ ->
            when (destination.id) {
                R.id.navigation_home, R.id.navigation_history, R.id.navigation_settings, R.id.navigation_results -> {
                    stopAllScanningSessions()
                    binding.bottomNav.visibility = android.view.View.VISIBLE
                }
                R.id.navigation_scan, R.id.navigation_upload, R.id.navigation_ar_scan, R.id.navigation_paste -> {
                    binding.bottomNav.visibility = android.view.View.GONE
                }
                else -> {
                    binding.bottomNav.visibility = android.view.View.VISIBLE
                }
            }
        }
        
        val appBarConfiguration = AppBarConfiguration(
            setOf(
                R.id.navigation_home, R.id.navigation_history, R.id.navigation_settings
            )
        )
        // setupActionBarWithNavController(navController, appBarConfiguration)
        navView.setupWithNavController(navController)
    }
    
    private fun stopAllScanningSessions() {
        // Stop any ongoing camera/scanning sessions and analysis
        try {
            // This will be called when navigating to Home, History, or Settings
            // Individual fragments handle their own cleanup in onDestroyView
            Log.d("MainActivity", "Stopping all scanning sessions and analysis")
            
            // Force garbage collection to help clear analysis state
            System.gc()
        } catch (e: Exception) {
            Log.e("MainActivity", "Error stopping scanning sessions: ${e.message}")
        }
    }
}
