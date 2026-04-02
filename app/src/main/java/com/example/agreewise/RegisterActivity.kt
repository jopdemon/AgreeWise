package com.example.agreewise

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.agreewise.databinding.ActivityRegisterBinding
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.database.FirebaseDatabase

class RegisterActivity : AppCompatActivity() {

    private lateinit var binding: ActivityRegisterBinding
    private lateinit var auth: FirebaseAuth

    companion object {
        private const val TAG = "RegisterActivity"
        private const val RC_SIGN_IN = 9001
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRegisterBinding.inflate(layoutInflater)
        setContentView(binding.root)

        auth = FirebaseAuth.getInstance()
        
        animateViews()

        binding.btnRegister.setOnClickListener {
            val name = binding.nameEditText.text.toString().trim()
            val email = binding.emailEditText.text.toString().trim()
            val password = binding.passwordEditText.text.toString().trim()
            val confirmPassword = binding.confirmPasswordEditText.text.toString().trim()

            binding.nameLayout.error = null
            binding.emailLayout.error = null
            binding.passwordLayout.error = null
            binding.confirmPasswordLayout.error = null

            var isValid = true

            if (name.isEmpty()) {
                binding.nameLayout.error = "Username is required"
                isValid = false
            }
            if (email.isEmpty()) {
                binding.emailLayout.error = "Email is required"
                isValid = false
            }
            if (password.isEmpty()) {
                binding.passwordLayout.error = "Password is required"
                isValid = false
            }
            if (confirmPassword.isEmpty()) {
                binding.confirmPasswordLayout.error = "Confirm password"
                isValid = false
            } else if (password != confirmPassword) {
                binding.confirmPasswordLayout.error = "Passwords do not match"
                isValid = false
            }
            if (!binding.checkboxTerms.isChecked) {
                Toast.makeText(this, "Please agree to the Terms of Service and Privacy Policy", Toast.LENGTH_SHORT).show()
                isValid = false
            }

            if (isValid) {
                setLoadingState(true)
                auth.createUserWithEmailAndPassword(email, password)
                    .addOnCompleteListener(this) { task ->
                        if (task.isSuccessful) {
                            val user = auth.currentUser
                            user?.let {
                                saveUserProfileToFirebase(it.uid, name, email, "")
                            } ?: run {
                                setLoadingState(false)
                                Toast.makeText(this, "Registration Successful", Toast.LENGTH_SHORT).show()
                                startActivity(Intent(this, MainActivity::class.java))
                                finish()
                            }
                        } else {
                            setLoadingState(false)
                            Toast.makeText(this, "Registration Failed: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
                        }
                    }
            }
        }

        binding.textLogin.setOnClickListener {
            finish()
        }

        binding.btnGoogle.setOnClickListener {
            signInWithGoogle()
        }
    }

    private fun setLoadingState(isLoading: Boolean) {
        binding.btnRegister.isEnabled = !isLoading
        binding.btnGoogle.isEnabled = !isLoading
        binding.textLogin.isEnabled = !isLoading
        
        if (isLoading) {
            binding.btnRegister.text = "Registering..."
        } else {
            binding.btnRegister.text = "Continue"
        }
    }

    private fun animateViews() {
        val views = listOf(
            binding.textSignUpTitle,
            binding.nameLayout,
            binding.emailLayout,
            binding.passwordLayout,
            binding.confirmPasswordLayout,
            binding.haveAccountLayout,
            binding.btnRegister,
            binding.checkboxTerms,
            binding.btnGoogle
        )

        views.forEachIndexed { index, view ->
            view.alpha = 0f
            view.translationY = 50f
            view.animate()
                .alpha(1f)
                .translationY(0f)
                .setStartDelay((index * 50).toLong())
                .setDuration(400)
                .start()
        }
    }

    private fun signInWithGoogle() {
        setLoadingState(true)
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
                val errorCode = (e as? com.google.android.gms.common.api.ApiException)?.statusCode
                val errorMessage = when (errorCode) {
                    7 -> "Network Error: Check Internet connection"
                    10 -> "Developer Error: SHA-1 mismatch or Client ID issue (Code 10)"
                    12500 -> "Configuration Error: Check google-services.json (Code 12500)"
                    12501 -> "User Canceled sign-in"
                    else -> e.message ?: "Unknown Error"
                }
                Log.e(TAG, "Google Sign-in failed: $errorMessage", e)
                Toast.makeText(this, "Google Sign-in failed: $errorMessage", Toast.LENGTH_LONG).show()
                setLoadingState(false)
            }
        }
    }

    private fun firebaseAuthWithGoogle(idToken: String) {
        setLoadingState(true)
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        auth.signInWithCredential(credential)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    val user = auth.currentUser
                    user?.let {
                        val photoUrl = it.photoUrl?.toString() ?: ""
                        saveUserProfileToFirebase(it.uid, it.displayName, it.email, photoUrl)
                    } ?: run {
                        setLoadingState(false)
                    }
                } else {
                    setLoadingState(false)
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
        
        FirebaseDatabase.getInstance()
            .getReference("users")
            .child(uid)
            .setValue(userMap)
            .addOnCompleteListener {
                setLoadingState(false)
                Toast.makeText(this, "Registration Successful", Toast.LENGTH_SHORT).show()
                startActivity(Intent(this, MainActivity::class.java))
                finish()
            }
    }
}