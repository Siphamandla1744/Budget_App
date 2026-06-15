package com.example.budget_app

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth

@SuppressLint("CustomSplashScreen")
class activity_splash : AppCompatActivity() {

    private val TAG = "APP_ROUTING"
    private val TIMEOUT_MS = 2500L

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        try {
            enableEdgeToEdge()
            setContentView(R.layout.activity_splash2)
            Log.d(TAG, "Splash started. Initializing session verification...")
            
            // Safe asynchronous check with a deterministic fallback
            Handler(Looper.getMainLooper()).postDelayed({
                verifyAndRoute()
            }, TIMEOUT_MS)

        } catch (e: Exception) {
            Log.e(TAG, "Critical Splash Crash: ${e.message}")
            // Emergency fallback route
            safeNavigate(activity_login::class.java)
        }
    }

    private fun verifyAndRoute() {
        if (isFinishing || isDestroyed) return

        try {
            val user = FirebaseAuth.getInstance().currentUser
            if (user != null) {
                Log.i(TAG, "Active session detected for UID: ${user.uid}. Routing to MainActivity.")
                safeNavigate(MainActivity::class.java)
            } else {
                Log.i(TAG, "No session found. Routing to LoginActivity.")
                safeNavigate(activity_login::class.java)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Routing Logic Failed: ${e.message}")
            safeNavigate(activity_login::class.java)
        }
    }

    private fun safeNavigate(target: Class<*>) {
        try {
            val intent = Intent(this, target)
            // CRITICAL: Wipe back-stack to prevent navigation loops
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            finish()
        } catch (e: Exception) {
            Log.e(TAG, "Intent Transition Failed: ${e.message}")
        }
    }
}