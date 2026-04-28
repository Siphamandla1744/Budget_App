package com.example.budget_app

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth

class activity_splash : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_splash2)

        auth = FirebaseAuth.getInstance()

        // Delay for splash screen (3 seconds)
        Handler(Looper.getMainLooper()).postDelayed({

            // Check if user is logged in
            val currentUser = auth.currentUser
            if (currentUser != null) {
                // User already logged in → go to MainActivity
                startActivity(Intent(this, MainActivity::class.java))
            } else {
                // User not logged in → go to Login
                startActivity(Intent(this, activity_login::class.java))
            }

            finish()

        }, 3000)
    }
}