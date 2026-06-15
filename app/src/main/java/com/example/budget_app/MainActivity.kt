package com.example.budget_app

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.budget_app.transaction_adapter.TransactionAdapter
import com.example.budget_app.model.Account
import com.example.budget_app.model.transactions
import com.github.mikephil.charting.charts.PieChart
import com.github.mikephil.charting.data.PieData
import com.github.mikephil.charting.data.PieDataSet
import com.github.mikephil.charting.data.PieEntry
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import java.util.Calendar
import java.util.Locale

class MainActivity : AppCompatActivity() {

    private val TAG = "DASHBOARD_ENGINE"
    
    private var auth: FirebaseAuth? = null
    private var database: FirebaseDatabase? = null
    
    // UI Elements (Nullable for Zero-Assumption binding)
    private var pieChart: PieChart? = null
    private var rvRecentTransactions: RecyclerView? = null
    private var transactionAdapter: TransactionAdapter? = null
    private val transactionList = mutableListOf<transactions>()
    private var llAccountContainer: LinearLayout? = null
    private var tvMonthlyTotal: TextView? = null
    private var ivTopProfile: ImageView? = null
    private var tvWelcomeHeader: TextView? = null
    private var tvXpStatus: TextView? = null
    private var pbLevelProgress: ProgressBar? = null

    private var tvHighestExpenseName: TextView? = null
    private var vHighestExpenseColor: View? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d(TAG, "MainActivity lifecycle started.")

        // GLOBAL DIAGNOSTIC TRAP: Captures any initialization failure and displays it on-screen
        try {
            // STEP 1: Apply Theme before anything else
            applySavedTheme()

            // STEP 2: Force layout inflation first with ZERO logic
            try {
                setContentView(R.layout.activity_main)
            } catch (inflationError: Exception) {
                // If inflation fails, we are likely missing a view ID or resource
                showDiagnosticOverlay("XML INFLATION ERROR", inflationError)
                return
            }

            // STEP 3: Safe View Binding
            bindViewsInternal()
            
            // STEP 4: Inject Safe Placeholder States
            applyInitialUIFallbacks()

            // STEP 5: Auth & Engine Initialization
            auth = FirebaseAuth.getInstance()
            database = FirebaseDatabase.getInstance()

            val sessionUser = auth?.currentUser
            if (sessionUser == null) {
                Log.w(TAG, "Auth check failed: User is null.")
                forceSafeLogout()
                return
            }

            // STEP 6: Component Wiring
            setupNavigationHandlers()
            
            // STEP 7: Async Data Fetch (Must not block UI thread)
            startDataEngine(sessionUser.uid)

        } catch (globalException: Exception) {
            Log.e(TAG, "ULTIMATE STARTUP FAILURE", globalException)
            // INSTEAD OF REDIRECTING, WE TRAP THE ERROR AND SHOW IT
            showDiagnosticOverlay("INITIALIZATION CRASH", globalException)
        }
    }

    private fun showDiagnosticOverlay(title: String, e: Throwable) {
        val stackTrace = Log.getStackTraceString(e)
        Log.e("DIAGNOSTIC", "$title: $stackTrace")
        
        runOnUiThread {
            try {
                val dialog = androidx.appcompat.app.AlertDialog.Builder(this)
                    .setTitle("TRAPPED ERROR: $title")
                    .setMessage("The app failed to start. Review the trace below:\n\n$stackTrace")
                    .setCancelable(false)
                    .setPositiveButton("Copy Error") { _, _ ->
                        val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                        val clip = android.content.ClipData.newPlainText("App Crash Trace", stackTrace)
                        clipboard.setPrimaryClip(clip)
                        Toast.makeText(this, "Copied to clipboard", Toast.LENGTH_SHORT).show()
                    }
                    .setNegativeButton("Attempt Logout") { _, _ ->
                        forceSafeLogout()
                    }
                    .create()
                dialog.show()
                
                // Also set a temporary view in case the dialog fails
                val tv = TextView(this)
                tv.text = stackTrace
                tv.setTextColor(android.graphics.Color.RED)
                tv.setBackgroundColor(android.graphics.Color.BLACK)
                tv.setPadding(32, 32, 32, 32)
                tv.isVerticalScrollBarEnabled = true
                setContentView(tv)
            } catch (inner: Exception) {
                Log.e("DIAGNOSTIC", "Failed to show dialog", inner)
            }
        }
    }

    private fun applySavedTheme() {
        try {
            val sharedPrefs = getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
            val isDarkMode = sharedPrefs.getBoolean("dark_mode_enabled", true)
            if (isDarkMode) {
                androidx.appcompat.app.AppCompatDelegate.setDefaultNightMode(androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_YES)
            } else {
                androidx.appcompat.app.AppCompatDelegate.setDefaultNightMode(androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_NO)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Theme application failed", e)
        }
    }

    private fun bindViewsInternal() {
        try {
            pieChart = findViewById(R.id.pieChart)
            rvRecentTransactions = findViewById(R.id.rvRecentTransactions)
            llAccountContainer = findViewById(R.id.llAccountContainer)
            tvWelcomeHeader = findViewById(R.id.tvTopTitle)
            tvMonthlyTotal = findViewById(R.id.tvMonthlyTotal)
            tvHighestExpenseName = findViewById(R.id.tvHighestExpenseName)
            vHighestExpenseColor = findViewById(R.id.vHighestExpenseColor)

            rvRecentTransactions?.layoutManager = LinearLayoutManager(this)
            transactionAdapter = TransactionAdapter(transactionList, { t -> launchEditor(t) }, {})
            rvRecentTransactions?.adapter = transactionAdapter
            
            findViewById<CardView>(R.id.cvAddAccount)?.setOnClickListener {
                startActivity(Intent(this, AddAccountActivity::class.java))
            }
            findViewById<CardView>(R.id.cvTrackSpending)?.setOnClickListener {
                startActivity(Intent(this, AddExpenseActivity::class.java))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Binding failed: ${e.message}")
        }
    }

    private fun applyInitialUIFallbacks() {
        // Guarantee no empty text fields
        tvMonthlyTotal?.text = "R 0.00"
        tvWelcomeHeader?.text = "Welcome back!"
        pieChart?.setNoDataText("Loading budget data...")
    }

    private fun startDataEngine(uid: String) {
        // Fire independent async tasks with internal safety
        loadProfileData(uid)
        loadAccountData(uid)
        loadTransactionData(uid)
        loadGamificationData(uid)
    }

    private fun loadGamificationData(uid: String) {
        database?.getReference("users")?.child(uid)?.child("gamification")
            ?.addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    if (isFinishing || isDestroyed) return
                    val xp = snapshot.child("xp").getValue(Int::class.java) ?: 0
                    val level = snapshot.child("level").getValue(Int::class.java) ?: 1
                    
                    runOnUiThread {
                        // We could add these views to activity_main.xml or just log for now
                        // To be safe, we'll try to find them first
                        Log.d(TAG, "Gamification synced: Level $level, $xp XP")
                    }
                }
                override fun onCancelled(error: DatabaseError) {}
            })
    }

    private fun loadProfileData(uid: String) {
        database?.getReference("users")?.child(uid)?.child("profile")
            ?.addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    if (isFinishing || isDestroyed) return
                    val name = snapshot.child("username").getValue(String::class.java) ?: "User"
                    val imgUrl = snapshot.child("profileImageUrl").getValue(String::class.java)
                    
                    runOnUiThread {
                        tvWelcomeHeader?.text = "Welcome back, $name!"
                        if (!imgUrl.isNullOrEmpty() && ivTopProfile != null) {
                            com.bumptech.glide.Glide.with(this@MainActivity)
                                .load(imgUrl).circleCrop().placeholder(R.drawable.pic1).into(ivTopProfile!!)
                        }
                    }
                }
                override fun onCancelled(error: DatabaseError) {}
            })
    }

    private fun loadAccountData(uid: String) {
        database?.getReference("users")?.child(uid)?.child("accounts")
            ?.addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    if (isFinishing || isDestroyed) return
                    runOnUiThread {
                        llAccountContainer?.removeAllViews()
                        for (data in snapshot.children) {
                            val acc = data.getValue(Account::class.java)
                            if (acc != null) createAccountView(acc)
                        }
                        // Always ensure the "Add" card exists
                        val addView = layoutInflater.inflate(R.layout.item_add_account_card, llAccountContainer, false)
                        addView.setOnClickListener { startActivity(Intent(this@MainActivity, AddAccountActivity::class.java)) }
                        llAccountContainer?.addView(addView)
                    }
                }
                override fun onCancelled(error: DatabaseError) {}
            })
    }

    private fun createAccountView(acc: Account) {
        try {
            val card = CardView(this).apply {
                layoutParams = LinearLayout.LayoutParams(600, 300).apply { setMargins(0, 0, 24, 0) }
                radius = 70f
                setCardBackgroundColor(Color.BLACK)
                val l = LinearLayout(this@MainActivity).apply { orientation = LinearLayout.VERTICAL; setPadding(48, 48, 48, 48) }
                val n = TextView(this@MainActivity).apply { text = acc.name; setTextColor(Color.WHITE); textSize = 14f }
                val b = TextView(this@MainActivity).apply { 
                    text = String.format(Locale.getDefault(), "R %.2f", acc.balance)
                    setTextColor(Color.WHITE); textSize = 22f; setTypeface(null, android.graphics.Typeface.BOLD) 
                }
                l.addView(n); l.addView(b); addView(l)
                setOnClickListener {
                    val i = Intent(this@MainActivity, AddAccountActivity::class.java)
                    i.putExtra("ACCOUNT_ID", acc.accountId)
                    startActivity(i)
                }
            }
            llAccountContainer?.addView(card, 0) // Add to start
        } catch (e: Exception) { Log.e(TAG, "Account Card Failure") }
    }

    private fun loadTransactionData(uid: String) {
        database?.getReference("users")?.child(uid)?.child("transactions")
            ?.addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    if (isFinishing || isDestroyed) return
                    transactionList.clear()
                    for (data in snapshot.children) {
                        val t = data.getValue(transactions::class.java) ?: continue
                        transactionList.add(t)
                    }
                    runOnUiThread {
                        transactionList.reverse()
                        transactionAdapter?.notifyDataSetChanged()
                        
                        // CHART BINDING: Refresh visualization whenever transactions change
                        updatePieChart(transactionList)
                    }
                }
                override fun onCancelled(error: DatabaseError) {}
            })
    }

    private fun updatePieChart(list: List<transactions>) {
        if (pieChart == null) return

        val categoryTotals = mutableMapOf<String, Double>()
        val month = Calendar.getInstance().get(Calendar.MONTH) + 1
        var monthlyTotal = 0.0
        
        for (t in list) {
            val dateParts = t.transaction_date.split("/")
            if (dateParts.size >= 2 && dateParts[1].toIntOrNull() == month && t.category.type == "Expense") {
                val catName = t.category.category_name
                categoryTotals[catName] = categoryTotals.getOrDefault(catName, 0.0) + t.transaction_amamount
                monthlyTotal += t.transaction_amamount
            }
        }

        if (categoryTotals.isEmpty()) {
            pieChart?.clear()
            pieChart?.setNoDataText("No expenses this month")
            pieChart?.invalidate()
            tvHighestExpenseName?.text = "N/A"
            tvMonthlyTotal?.text = "R 0.00"
            return
        }

        // Update Monthly Total in Header
        animateCounter(tvMonthlyTotal, monthlyTotal)

        val entries = ArrayList<PieEntry>()
        var highestCat = ""
        var maxAmount = 0.0
        
        for ((name, total) in categoryTotals) {
            entries.add(PieEntry(total.toFloat(), name))
            if (total > maxAmount) {
                maxAmount = total
                highestCat = name
            }
        }

        val colors = com.github.mikephil.charting.utils.ColorTemplate.MATERIAL_COLORS.toMutableList()
        val dataSet = PieDataSet(entries, "")
        dataSet.colors = colors
        dataSet.setDrawValues(false) // Clean donut look

        val data = PieData(dataSet)
        pieChart?.data = data
        
        // Donut configuration
        pieChart?.isDrawHoleEnabled = true
        pieChart?.holeRadius = 75f
        pieChart?.setHoleColor(Color.WHITE)
        pieChart?.setTransparentCircleRadius(80f)
        
        // Center text styling
        val centerText = android.text.SpannableString("Spending\n\n${String.format(Locale.getDefault(), "%,.0f\n%s", maxAmount, highestCat)}")
        pieChart?.centerText = centerText
        pieChart?.setCenterTextSize(16f)
        pieChart?.setCenterTextColor(Color.WHITE)
        
        pieChart?.description?.isEnabled = false
        pieChart?.legend?.isEnabled = false
        pieChart?.animateY(1000)
        pieChart?.invalidate()

        // Update indicator under chart
        tvHighestExpenseName?.text = highestCat
        val colorIndex = entries.indexOfFirst { it.label == highestCat }
        if (colorIndex != -1) {
            vHighestExpenseColor?.setBackgroundColor(colors[colorIndex % colors.size])
        }
    }

    private fun animateCounter(view: TextView?, target: Double) {
        if (view == null) return
        view.text = String.format(Locale.getDefault(), "R %.2f", target)
        view.animate().scaleX(1.1f).scaleY(1.1f).setDuration(100).withEndAction {
            view.animate().scaleX(1f).scaleY(1f).setDuration(100).start()
        }.start()
    }

    private fun setupNavigationHandlers() {
        findViewById<ImageView>(R.id.btnTopNotifications)?.setOnClickListener { startActivity(Intent(this, NotificationsActivity::class.java)) }
        findViewById<CardView>(R.id.btnTopProfile)?.setOnClickListener { startActivity(Intent(this, SettingsActivity::class.java)) }
        
        NavigationHelper.setupBottomNavigation(this, 1)
    }

    private fun launchEditor(t: transactions) {
        val i = Intent(this, AddExpenseActivity::class.java)
        i.putExtra("TRANSACTION_KEY", t.transaction_id)
        startActivity(i)
    }

    private fun forceSafeLogout() {
        val i = Intent(this, activity_login::class.java)
        i.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(i)
        finish()
    }

    private fun handleInitializationCollapse(e: Exception, fatal: Boolean) {
        Log.e(TAG, "COLLAPSE CRASH DETECTED", e)
        if (fatal) {
            Toast.makeText(this, "A critical error occurred. Please restart the app.", Toast.LENGTH_LONG).show()
            // Log full stack trace for the user to find in logcat
            e.printStackTrace()
            forceSafeLogout()
        } else {
            Toast.makeText(this, "Syncing data in background...", Toast.LENGTH_SHORT).show()
        }
    }
}