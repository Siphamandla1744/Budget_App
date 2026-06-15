package com.example.budget_app

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class SettingsActivity : AppCompatActivity() {
    private lateinit var auth: FirebaseAuth
    private lateinit var database: FirebaseDatabase
    private lateinit var etEditUsername: EditText
    private lateinit var tvUsername: TextView
    private lateinit var ivProfile: ImageView
    private lateinit var switchNotifications: androidx.appcompat.widget.SwitchCompat
    private lateinit var switchTheme: androidx.appcompat.widget.SwitchCompat

    private val pickImage = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val imageUri: Uri? = result.data?.data
            if (imageUri != null) {
                ivProfile.setImageURI(imageUri)
                saveProfileImage(imageUri)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_settings)

        auth = FirebaseAuth.getInstance()
        database = FirebaseDatabase.getInstance()

        etEditUsername = findViewById(R.id.etEditUsername)
        tvUsername = findViewById(R.id.tvUsername)
        ivProfile = findViewById(R.id.ivProfile)
        switchNotifications = findViewById(R.id.switchNotifications)
        switchTheme = findViewById(R.id.switchTheme)

        loadUserData()
        loadPreferences()
        setupBottomNavigation()

        findViewById<Button>(R.id.btnLogout).setOnClickListener {
            auth.signOut()
            val intent = Intent(this, activity_login::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
        }
        
        etEditUsername.setOnEditorActionListener { _, _, _ ->
            saveUsername()
            true
        }

        ivProfile.setOnClickListener {
            val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
            pickImage.launch(intent)
        }

        switchNotifications.setOnCheckedChangeListener { _, isChecked ->
            savePreference("notifications_enabled", isChecked)
        }

        switchTheme.setOnCheckedChangeListener { _, isChecked ->
            savePreference("dark_mode_enabled", isChecked)
            
            if (isChecked) {
                androidx.appcompat.app.AppCompatDelegate.setDefaultNightMode(androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_YES)
            } else {
                androidx.appcompat.app.AppCompatDelegate.setDefaultNightMode(androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_NO)
            }

            Toast.makeText(this, if (isChecked) "Dark Mode Enabled" else "Light Mode Enabled", Toast.LENGTH_SHORT).show()
        }
    }

    private fun loadPreferences() {
        val sharedPrefs = getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
        switchNotifications.isChecked = sharedPrefs.getBoolean("notifications_enabled", true)
        switchTheme.isChecked = sharedPrefs.getBoolean("dark_mode_enabled", true)
    }

    private fun savePreference(key: String, value: Boolean) {
        val sharedPrefs = getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
        sharedPrefs.edit().putBoolean(key, value).apply()
    }

    private fun loadUserData() {
        val userId = auth.currentUser?.uid ?: return
        database.getReference("users").child(userId).child("profile")
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val name = snapshot.child("username").getValue(String::class.java) ?: "User"
                    val imageUrl = snapshot.child("profileImageUrl").getValue(String::class.java)
                    
                    tvUsername.text = name
                    etEditUsername.setText(name)
                    
                    if (!imageUrl.isNullOrEmpty()) {
                        Glide.with(this@SettingsActivity).load(imageUrl).into(ivProfile)
                    }
                }
                override fun onCancelled(error: DatabaseError) {}
            })
    }

    private fun saveUsername() {
        val newName = etEditUsername.text.toString().trim()
        if (newName.isEmpty()) return

        val userId = auth.currentUser?.uid ?: return
        database.getReference("users").child(userId).child("profile").child("username").setValue(newName)
            .addOnSuccessListener {
                tvUsername.text = newName
                Toast.makeText(this, "Username updated", Toast.LENGTH_SHORT).show()
            }
    }

    private fun saveProfileImage(uri: Uri) {
        val userId = auth.currentUser?.uid ?: return
        database.getReference("users").child(userId).child("profile").child("profileImageUrl").setValue(uri.toString())
            .addOnSuccessListener {
                Toast.makeText(this, "Profile image updated", Toast.LENGTH_SHORT).show()
            }
    }

    private fun setupBottomNavigation() {
        NavigationHelper.setupBottomNavigation(this, 0)
    }
}