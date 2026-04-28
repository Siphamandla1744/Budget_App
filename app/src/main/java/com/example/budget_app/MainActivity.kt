package com.example.budget_app

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import androidx.core.view.GravityCompat
import androidx.core.view.ViewCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.budget_app.Transaction_Adapter.TransactionAdapter
import com.example.budget_app.model.Account
import com.example.budget_app.model.transactions
import com.github.mikephil.charting.charts.PieChart
import com.github.mikephil.charting.data.PieData
import com.github.mikephil.charting.data.PieDataSet
import com.github.mikephil.charting.data.PieEntry
import com.github.mikephil.charting.utils.ColorTemplate
import com.google.android.material.navigation.NavigationView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*

class MainActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var database: FirebaseDatabase
    private lateinit var drawerLayout: DrawerLayout
    private lateinit var pieChart: PieChart
    private lateinit var rvRecentTransactions: RecyclerView
    private lateinit var transactionAdapter: TransactionAdapter
    private val transactionList = mutableListOf<transactions>()
    private lateinit var llAccountContainer: LinearLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        auth = FirebaseAuth.getInstance()
        database = FirebaseDatabase.getInstance()
        drawerLayout = findViewById(R.id.drawer_layout)
        pieChart = findViewById(R.id.pieChart)
        rvRecentTransactions = findViewById(R.id.rvRecentTransactions)
        llAccountContainer = findViewById(R.id.llAccountContainer)
        val navView: NavigationView = findViewById(R.id.nav_view)

        // Setup RecyclerView
        rvRecentTransactions.layoutManager = LinearLayoutManager(this)
        transactionAdapter = TransactionAdapter(
            transactionList,
            onTransactionClick = { transaction ->
                // Start AddExpenseActivity in Edit Mode (needs TRANSACTION_KEY)
                // We'd need the key here. In fetchTransactions, we can store keys.
                Toast.makeText(this, "Click history for full management", Toast.LENGTH_SHORT).show()
            },
            onTransactionLongClick = { /* Optional: delete from home */ }
        )
        rvRecentTransactions.adapter = transactionAdapter

        // Handle window insets
        val mainView = findViewById<View>(R.id.main_scroll_view)
        ViewCompat.setOnApplyWindowInsetsListener(mainView) { v, insets ->
            val systemBars = insets.getInsets(androidx.core.view.WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // Click Listeners
        findViewById<ImageView>(R.id.ivMenu).setOnClickListener {
            drawerLayout.openDrawer(GravityCompat.START)
        }

        findViewById<TextView>(R.id.tvAddAccount).setOnClickListener {
            startActivity(Intent(this, AddAccountActivity::class.java))
        }

        findViewById<CardView>(R.id.cvTrackSpending).setOnClickListener {
            startActivity(Intent(this, AddExpenseActivity::class.java))
        }

        findViewById<TextView>(R.id.tvBudgetTab).setOnClickListener {
            startActivity(Intent(this, Createbudget::class.java))
        }

        navView.setNavigationItemSelectedListener { menuItem ->
            when (menuItem.itemId) {
                R.id.nav_home -> drawerLayout.closeDrawer(GravityCompat.START)
                R.id.nav_categories -> startActivity(Intent(this, Category::class.java))
                R.id.nav_goals -> startActivity(Intent(this, activity_creategoal::class.java))
                R.id.nav_history -> startActivity(Intent(this, TransactionHistoryActivity::class.java))
                R.id.nav_help -> Toast.makeText(this, "Help Centre", Toast.LENGTH_SHORT).show()
                R.id.nav_logout -> {
                    auth.signOut()
                    startActivity(Intent(this, activity_login::class.java))
                    finish()
                }
            }
            drawerLayout.closeDrawer(GravityCompat.START)
            true
        }

        fetchTransactions()
        fetchAccounts()
    }

    private fun fetchAccounts() {
        val userId = auth.currentUser?.uid ?: return
        val accountsRef = database.getReference("users").child(userId).child("accounts")

        accountsRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                llAccountContainer.removeAllViews()
                
                for (data in snapshot.children) {
                    val account = data.getValue(Account::class.java)
                    if (account != null) {
                        addAccountCard(account)
                    }
                }

                val addAccountText = TextView(this@MainActivity).apply {
                    text = "+ Add Account"
                    setTextColor(Color.BLACK)
                    textSize = 16f
                    setPadding(32, 8, 32, 8)
                    setOnClickListener {
                        startActivity(Intent(this@MainActivity, AddAccountActivity::class.java))
                    }
                }
                llAccountContainer.addView(addAccountText)
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(this@MainActivity, "Failed to load accounts", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun addAccountCard(account: Account) {
        val cardView = CardView(this).apply {
            layoutParams = LinearLayout.LayoutParams(450, 180).apply {
                setMargins(0, 0, 24, 0)
            }
            radius = 60f
            setCardBackgroundColor(Color.BLACK)
            setContentPadding(32, 16, 32, 16)
            
            // Manage Account Click
            setOnClickListener {
                val intent = Intent(this@MainActivity, AddAccountActivity::class.java)
                intent.putExtra("ACCOUNT_ID", account.accountId)
                startActivity(intent)
            }
        }

        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
        }

        val tvName = TextView(this).apply {
            text = account.name
            setTextColor(Color.WHITE)
            textSize = 14f
        }

        val tvBalance = TextView(this).apply {
            text = "R ${String.format("%.2f", account.balance)}"
            setTextColor(Color.WHITE)
            textSize = 18f
            setTypeface(null, android.graphics.Typeface.BOLD)
        }

        layout.addView(tvName)
        layout.addView(tvBalance)
        cardView.addView(layout)
        llAccountContainer.addView(cardView)
    }

    private fun fetchTransactions() {
        val userId = auth.currentUser?.uid ?: return
        val transRef = database.getReference("users").child(userId).child("transactions")

        transRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                transactionList.clear()
                val categoryTotals = mutableMapOf<String, Float>()

                for (data in snapshot.children) {
                    val transaction = data.getValue(transactions::class.java)
                    if (transaction != null) {
                        transactionList.add(transaction)
                        val catName = transaction.category.category_name
                        val amount = transaction.transaction_amamount.toFloat()
                        categoryTotals[catName] = categoryTotals.getOrDefault(catName, 0f) + amount
                    }
                }
                
                transactionList.reverse() // Newest first
                transactionAdapter.notifyDataSetChanged()
                updatePieChart(categoryTotals)
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(this@MainActivity, "Error: ${error.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun updatePieChart(categoryTotals: Map<String, Float>) {
        val entries = ArrayList<PieEntry>()
        for ((name, total) in categoryTotals) {
            entries.add(PieEntry(total, name))
        }

        val dataSet = PieDataSet(entries, "")
        dataSet.colors = ColorTemplate.MATERIAL_COLORS.toList()
        dataSet.valueTextColor = Color.BLACK
        dataSet.valueTextSize = 12f

        val data = PieData(dataSet)
        pieChart.data = data
        pieChart.description.isEnabled = false
        pieChart.centerText = "Spending"
        pieChart.setCenterTextSize(16f)
        pieChart.animateY(1000)
        pieChart.invalidate()
    }

    override fun onBackPressed() {
        if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
            drawerLayout.closeDrawer(GravityCompat.START)
        } else {
            super.onBackPressed()
        }
    }
}