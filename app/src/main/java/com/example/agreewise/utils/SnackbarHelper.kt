package com.example.agreewise.utils

import android.view.View
import android.widget.TextView
import androidx.core.content.ContextCompat
import com.example.agreewise.R
import com.google.android.material.snackbar.Snackbar

object SnackbarHelper {

    fun showErrorMessage(view: View, message: String, anchorView: View? = null) {
        val snackbar = Snackbar.make(view, message, Snackbar.LENGTH_LONG)
        val snackbarView = snackbar.view
        
        // Custom background color
        snackbarView.backgroundTintList = ContextCompat.getColorStateList(view.context, R.color.risk_high)
        
        // Custom text color and style
        val textView = snackbarView.findViewById<TextView>(com.google.android.material.R.id.snackbar_text)
        textView.setTextColor(ContextCompat.getColor(view.context, R.color.white))
        textView.textSize = 14f
        
        // Add a "Dismiss" action for a premium experience
        snackbar.setAction("Dismiss") {
            snackbar.dismiss()
        }
        snackbar.setActionTextColor(ContextCompat.getColor(view.context, R.color.white))
        
        // Set an anchor view (e.g., above a button) if provided
        if (anchorView != null) {
            snackbar.anchorView = anchorView
        }
        
        // Add a slight elevation for a premium look
        snackbarView.elevation = 8f
        
        snackbar.show()
    }
}
