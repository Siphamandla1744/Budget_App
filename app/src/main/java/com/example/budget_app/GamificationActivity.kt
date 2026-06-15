package com.example.budget_app

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*

class GamificationActivity : AppCompatActivity() {
    private lateinit var auth: FirebaseAuth
    private lateinit var database: FirebaseDatabase
    private lateinit var pbLevel: ProgressBar
    private lateinit var tvXp: TextView
    private lateinit var tvLevelName: TextView
    private lateinit var ivTopProfile: ImageView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        try {
            enableEdgeToEdge()
            setContentView(R.layout.activity_gamification)
            
            auth = FirebaseAuth.getInstance()
            database = FirebaseDatabase.getInstance()

            pbLevel = findViewById(R.id.pbLevel)
            tvXp = findViewById(R.id.tvXp)
            tvLevelName = findViewById(R.id.tvLevelName)
            ivTopProfile = findViewById(R.id.ivTopProfile)

            setupTopBar()
            setupBottomNavigation()
            loadGamificationData()
            loadUserProfile()
            setupClickListeners()
        } catch (e: Exception) {
            android.util.Log.e("GAMIFICATION_CRASH", "Initialization failure", e)
            Toast.makeText(this, "Profile stats temporarily unavailable", Toast.LENGTH_SHORT).show()
            // Don't finish() or logout, just let the user see the layout even if empty
        }
    }

    private fun setupTopBar() {
        findViewById<ImageView>(R.id.btnTopNotifications).setOnClickListener {
            startActivity(Intent(this, NotificationsActivity::class.java))
        }
        findViewById<CardView>(R.id.btnTopProfile).setOnClickListener {
            startActivity(Intent(this, SettingsActivity::class.java))
        }
        findViewById<TextView>(R.id.tvTopTitle).text = "Achievements"
    }

    private fun loadUserProfile() {
        val userId = auth.currentUser?.uid ?: return
        database.getReference("users").child(userId).child("profile")
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val imageUrl = snapshot.child("profileImageUrl").getValue(String::class.java)
                    if (!imageUrl.isNullOrEmpty()) {
                        com.bumptech.glide.Glide.with(this@GamificationActivity).load(imageUrl).into(ivTopProfile)
                    }
                }
                override fun onCancelled(error: DatabaseError) {}
            })
    }

    private fun loadGamificationData() {
        val userId = auth.currentUser?.uid ?: return
        database.getReference("users").child(userId).child("gamification")
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val xp = snapshot.child("xp").getValue(Int::class.java) ?: 0
                    val level = snapshot.child("level").getValue(Int::class.java) ?: 1
                    
                    val xpThreshold = level * 200
                    val prevThreshold = (level - 1) * 200
                    val progress = if (xpThreshold > prevThreshold) {
                        ((xp - prevThreshold).toFloat() / (xpThreshold - prevThreshold) * 100).toInt()
                    } else 0
                    
                    tvXp.text = "$xp / $xpThreshold XP"
                    pbLevel.progress = progress.coerceIn(0, 100)
                    tvLevelName.text = "Level $level Finance Master"
                    
                    updateBadges(level)
                }
                override fun onCancelled(error: DatabaseError) {}
            })
    }

    private fun updateBadges(level: Int) {
        // Unlock badges based on level
        val badge3 = findViewById<CardView>(R.id.badge3) // Rich Kid
        val ivBadge3Icon = findViewById<ImageView>(R.id.ivBadge3Icon)
        if (level >= 6) {
            badge3.alpha = 1.0f
            ivBadge3Icon.setImageResource(android.R.drawable.btn_star_big_on)
        }
    }

    private fun setupClickListeners() {
        findViewById<CardView>(R.id.badge1).setOnClickListener {
            showBadgeInfo("First Save", "You've made your first successful saving deposit!")
        }
        findViewById<CardView>(R.id.badge2).setOnClickListener {
            showBadgeInfo("Budgeter", "You've successfully maintained a budget for 7 days.")
        }
        findViewById<CardView>(R.id.reward1).setOnClickListener {
            Toast.makeText(this, "Reward: Premium Theme unlocked at Level 10!", Toast.LENGTH_LONG).show()
        }
    }

    private fun showBadgeInfo(title: String, desc: String) {
        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle(title)
            .setMessage(desc)
            .setPositiveButton("Awesome", null)
            .show()
    }

    private fun setupBottomNavigation() {
        val well1 = findViewById<View>(R.id.nav_well_1)
        val well2 = findViewById<View>(R.id.nav_well_2)
        val well3 = findViewById<View>(R.id.nav_well_3)
        val well4 = findViewById<View>(R.id.nav_well_4)
        
        val wells = listOf(well1, well2, well3, well4)
        well4.isSelected = true

        well1.setOnClickListener {
            startActivity(Intent(this, MainActivity::class.java))
        }
        well2.setOnClickListener {
            startActivity(Intent(this, ReportsActivity::class.java))
        }
        well3.setOnClickListener {
            startActivity(Intent(this, Createbudget::class.java))
        }
        well4.setOnClickListener {
            // Already here
        }
    }
}