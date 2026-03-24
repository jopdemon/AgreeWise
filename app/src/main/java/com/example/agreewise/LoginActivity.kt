package com.example.agreewise

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.agreewise.databinding.ActivityLoginBinding
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.database.FirebaseDatabase

class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding
    private lateinit var auth: FirebaseAuth
    
    companion object {
        private const val TAG = "LoginActivity"
        private const val RC_SIGN_IN = 9001
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        auth = FirebaseAuth.getInstance()

        binding.btnLogin.setOnClickListener {
            val email = binding.emailEditText.text.toString()
            val password = binding.passwordEditText.text.toString()

            if (email.isNotEmpty() && password.isNotEmpty()) {
                auth.signInWithEmailAndPassword(email, password)
                    .addOnCompleteListener(this) { task ->
                        if (task.isSuccessful) {
                            startActivity(Intent(this, MainActivity::class.java))
                            finish()
                        } else {
                            Toast.makeText(this, "Authentication Failed: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
                        }
                    }
            } else {
                Toast.makeText(this, "Please enter email and password", Toast.LENGTH_SHORT).show()
            }
        }

        binding.btnGoogle.setOnClickListener {
            signInWithGoogle()
        }

        binding.textRegister.setOnClickListener {
            startActivity(Intent(this, RegisterActivity::class.java))
        }
    }

    private fun signInWithGoogle() {
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id)) 
            .requestEmail()
            .build()

        val googleSignInClient = GoogleSignIn.getClient(this, gso)
        val signInIntent = googleSignInClient.signInIntent
        startActivityForResult(signInIntent, RC_SIGN_IN)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == RC_SIGN_IN) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(data)
            try {
                val account = task.getResult(Exception::class.java)!!
                firebaseAuthWithGoogle(account.idToken!!)
            } catch (e: Exception) {
                Toast.makeText(this, "Google sign in failed: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun firebaseAuthWithGoogle(idToken: String) {
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        auth.signInWithCredential(credential)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    val user = auth.currentUser
                    Log.d(TAG, "Google Sign-In successful. User: ${user?.uid}")
                    user?.let {
                        Log.d(TAG, "Display Name: ${it.displayName}")
                        Log.d(TAG, "Email: ${it.email}")
                        Log.d(TAG, "Photo URL: ${it.photoUrl}")
                        
                        val photoUrl = it.photoUrl?.toString()
                        if (photoUrl != null) {
                            Log.d(TAG, "Saving profile with photo URL: $photoUrl")
                            saveUserProfileToFirebase(it.uid, it.displayName, it.email, photoUrl)
                        } else {
                            Log.w(TAG, "Photo URL is null, saving without photo")
                            saveUserProfileToFirebase(it.uid, it.displayName, it.email, "")
                        }
                    }
                    startActivity(Intent(this, MainActivity::class.java))
                    finish()
                } else {
                    Log.e(TAG, "Firebase auth with Google failed: ${task.exception?.message}")
                    Toast.makeText(this, "Firebase auth with Google failed", Toast.LENGTH_SHORT).show()
                }
            }
    }

    private fun saveUserProfileToFirebase(uid: String, displayName: String?, email: String?, photoUrl: String) {
        val userMap = hashMapOf(
            "displayName" to (displayName ?: ""),
            "email" to (email ?: ""),
            "photoUrl" to photoUrl
        )
        Log.d(TAG, "Attempting to save user profile to Firebase for UID: $uid")
        Log.d(TAG, "Profile data: $userMap")
        
        FirebaseDatabase.getInstance()
            .getReference("users")
            .child(uid)
            .setValue(userMap)
            .addOnSuccessListener {
                Log.d(TAG, "User profile saved successfully to Firebase")
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Failed to save user profile to Firebase: ${e.message}", e)
            }
    }
}
