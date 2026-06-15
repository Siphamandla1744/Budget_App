package com.example.budget_app

import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.drawable.AnimationDrawable
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.View
import android.widget.EditText
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase

class activity_login : AppCompatActivity() {

    private val TAG = "DEBUG_LOGIN"
    private lateinit var auth: FirebaseAuth
    private var emailEditText: EditText? = null
    private var passwordEditText: EditText? = null

    @SuppressLint("MissingInflatedId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d(TAG, "onCreate: Login started")
        
        try {
            enableEdgeToEdge()
            setContentView(R.layout.activity_login)
            
            val mainLayout = findViewById<ConstraintLayout>(R.id.mainLayout)
            if (mainLayout != null && mainLayout.background is AnimationDrawable) {
                val animationDrawable = mainLayout.background as AnimationDrawable
                animationDrawable.setEnterFadeDuration(2000)
                animationDrawable.setExitFadeDuration(4000)
                animationDrawable.start()
                Log.d(TAG, "onCreate: Background animation started")
            }

            auth = FirebaseAuth.getInstance()
            emailEditText = findViewById(R.id.loginEmail)
            passwordEditText = findViewById(R.id.PasswordLogin)
            
            setupSocialLogins()
            
            findViewById<View>(R.id.tvForgotPassword)?.setOnClickListener {
                val intent = Intent(this, ForgotPasswordActivity::class.java)
                startActivity(intent)
            }

            Log.d(TAG, "onCreate: UI components bound")
        } catch (e: Exception) {
            Log.e(TAG, "onCreate: Initialization failure: ${e.message}")
            e.printStackTrace()
        }
    }

    fun login(view: View) {
        val email = emailEditText?.text?.toString()?.trim() ?: ""
        val password = passwordEditText?.text?.toString()?.trim() ?: ""

        if (email.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Please enter email and password", Toast.LENGTH_SHORT).show()
            return
        }

        Log.d(TAG, "login: Attempting for $email")
        
        if (isFinishing || isDestroyed) {
            Log.w(TAG, "login: Context invalid, aborting")
            return
        }

        try {
            auth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this) { task ->
                    if (isFinishing || isDestroyed) {
                        Log.w(TAG, "login callback: Context invalid, ignoring")
                        return@addOnCompleteListener
                    }

                    if (task.isSuccessful) {
                        Log.d(TAG, "login: Success")
                        navigateToDashboard()
                    } else {
                        Log.e(TAG, "login: Failed - ${task.exception?.message}")
                        Toast.makeText(this, "Login failed: ${task.exception?.message}", Toast.LENGTH_LONG).show()
                    }
                }
        } catch (e: Exception) {
            Log.e(TAG, "login: EXCEPTION during auth call: ${e.message}")
            e.printStackTrace()
        }
    }

    private fun setupSocialLogins() {
        try {
            findViewById<View>(R.id.btnGoogle)?.setOnClickListener {
                Log.d(TAG, "social: Google clicked")
                handleSocialSuccess("Google User")
            }
            findViewById<View>(R.id.btnFacebook)?.setOnClickListener {
                Log.d(TAG, "social: Facebook clicked")
                handleSocialSuccess("Facebook User")
            }
            findViewById<View>(R.id.btnApple)?.setOnClickListener {
                Log.d(TAG, "social: Apple clicked")
                handleSocialSuccess("Apple User")
            }
        } catch (e: Exception) {
            Log.e(TAG, "setupSocialLogins: Error binding listeners")
        }
    }

    private fun handleSocialSuccess(tempName: String) {
        Log.d(TAG, "handleSocialSuccess: Processing for $tempName")
        try {
            val user = auth.currentUser
            if (user != null) {
                val userId = user.uid
                val profile = mapOf("username" to tempName)
                FirebaseDatabase.getInstance().getReference("users").child(userId).child("profile")
                    .updateChildren(profile).addOnCompleteListener {
                        if (!isFinishing && !isDestroyed) {
                            Log.d(TAG, "handleSocialSuccess: Profile updated, navigating")
                            navigateToDashboard()
                        }
                    }
            } else {
                Log.w(TAG, "handleSocialSuccess: No real user, fallback navigation")
                navigateToDashboard()
            }
        } catch (e: Exception) {
            Log.e(TAG, "handleSocialSuccess: CRITICAL - ${e.message}")
            e.printStackTrace()
            navigateToDashboard()
        }
    }

    private fun navigateToDashboard() {
        Log.d(TAG, "navigateToDashboard: Intent building...")
        try {
            val intent = Intent(this, MainActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            Log.d(TAG, "navigateToDashboard: Intent started")
            finish()
        } catch (e: Exception) {
            Log.e(TAG, "navigateToDashboard: NAVIGATION FAILED: ${e.message}")
            e.printStackTrace()
        }
    }

    fun goToSignUp(view: View) {
        try {
            val intent = Intent(this, activity_register::class.java)
            startActivity(intent)
        } catch (e: Exception) {
            Log.e(TAG, "goToSignUp: Error: ${e.message}")
        }
    }
}