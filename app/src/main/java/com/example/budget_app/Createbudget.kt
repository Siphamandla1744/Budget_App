package com.example.budget_app

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.GravityCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.budget_app.model.Goal
import com.google.android.material.navigation.NavigationView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*

class Createbudget : AppCompatActivity() {
    
    private lateinit var drawerLayout: DrawerLayout
    private lateinit var auth: FirebaseAuth
    private lateinit var database: FirebaseDatabase
    private lateinit var rvGoals: RecyclerView
    private lateinit var goalAdapter: GoalAdapter
    private lateinit var tvEmptyGoals: TextView
    private val goalList = mutableListOf<Goal>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_createbudget)

        auth = FirebaseAuth.getInstance()
        database = FirebaseDatabase.getInstance()
        drawerLayout = findViewById(R.id.drawer_layout)
        rvGoals = findViewById(R.id.rvGoals)
        tvEmptyGoals = findViewById(R.id.tvEmptyGoals)
        val navView: NavigationView = findViewById(R.id.nav_view)

        // Setup RecyclerView
        rvGoals.layoutManager = LinearLayoutManager(this)
        goalAdapter = GoalAdapter(goalList) { goal ->
            // Handle goal click (e.g., edit or add money)
            Toast.makeText(this, "Goal: ${goal.name}", Toast.LENGTH_SHORT).show()
        }
        rvGoals.adapter = goalAdapter

        // Initialize Views
        val ivMenu = findViewById<ImageView>(R.id.ivMenu)
        val tvAccountsTab = findViewById<TextView>(R.id.tvAccountsTab)
        val btnCreateBudgets = findViewById<TextView>(R.id.btnCreateBudgets)
        val btnCreateGoals = findViewById<TextView>(R.id.btnCreateGoals)

        // Navigation
        tvAccountsTab.setOnClickListener {
            startActivity(Intent(this, MainActivity::class.java))
            finish()
        }

        btnCreateBudgets.setOnClickListener {
            startActivity(Intent(this, Category::class.java))
        }

        btnCreateGoals.setOnClickListener {
            startActivity(Intent(this, activity_creategoal::class.java))
        }

        ivMenu.setOnClickListener {
            drawerLayout.openDrawer(GravityCompat.START)
        }

        navView.setNavigationItemSelectedListener { menuItem ->
            when (menuItem.itemId) {
                R.id.nav_home -> {
                    startActivity(Intent(this, MainActivity::class.java))
                    finish()
                }
                R.id.nav_categories -> startActivity(Intent(this, Category::class.java))
                R.id.nav_goals -> drawerLayout.closeDrawer(GravityCompat.START)
                R.id.nav_history -> startActivity(Intent(this, TransactionHistoryActivity::class.java))
                R.id.nav_logout -> {
                    auth.signOut()
                    startActivity(Intent(this, activity_login::class.java))
                    finish()
                }
            }
            drawerLayout.closeDrawer(GravityCompat.START)
            true
        }

        fetchGoals()
    }

    private fun fetchGoals() {
        val userId = auth.currentUser?.uid ?: return
        val goalsRef = database.getReference("users").child(userId).child("goals")

        goalsRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                goalList.clear()
                for (data in snapshot.children) {
                    val goal = data.getValue(Goal::class.java)
                    if (goal != null) {
                        goalList.add(goal)
                    }
                }
                
                if (goalList.isEmpty()) {
                    tvEmptyGoals.visibility = View.VISIBLE
                    rvGoals.visibility = View.GONE
                } else {
                    tvEmptyGoals.visibility = View.GONE
                    rvGoals.visibility = View.VISIBLE
                }
                goalAdapter.notifyDataSetChanged()
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(this@Createbudget, "Error: ${error.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    override fun onBackPressed() {
        if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
            drawerLayout.closeDrawer(GravityCompat.START)
        } else {
            super.onBackPressed()
        }
    }
}