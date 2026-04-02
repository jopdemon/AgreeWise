package com.example.agreewise.ui.scan

import android.util.Log
import android.widget.Toast
import com.google.ar.core.exceptions.*
import com.google.ar.sceneform.ux.ArFragment

/**
 * A custom implementation of ArFragment that handles session exceptions.
 * This prevents the app from crashing when ARCore is missing, outdated, or incompatible.
 */
class CustomArFragment : ArFragment() {

    override fun handleSessionException(sessionException: UnavailableException?) {
        val message = when (sessionException) {
            is UnavailableArcoreNotInstalledException -> "Please install Google Play Services for AR"
            is UnavailableApkTooOldException -> "Please update Google Play Services for AR"
            is UnavailableSdkTooOldException -> "Please update this app"
            is UnavailableDeviceNotCompatibleException -> "This device does not support ARCORE"
            is UnavailableUserDeclinedInstallationException -> "Please install Google Play Services for AR to use this feature"
            else -> "Failed to initialize AR: ${sessionException?.message ?: "Unknown error"}"
        }

        Log.e("CustomArFragment", "ARCore Session Error: $message", sessionException)
        
        // Show user-friendly message
        Toast.makeText(requireContext(), message, Toast.LENGTH_LONG).show()

        // Optionally, you can also inform the parent fragment or activity 
        // if you want to navigate away from the AR screen.
    }
}
