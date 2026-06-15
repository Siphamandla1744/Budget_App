package com.example.budget_app

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import com.example.budget_app.model.transactions
import com.github.mikephil.charting.charts.BarChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import com.github.mikephil.charting.utils.ColorTemplate
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import java.util.Calendar
import java.util.Locale

class ReportsActivity : AppCompatActivity() {
    private lateinit var auth: FirebaseAuth
    private lateinit var database: FirebaseDatabase
    private lateinit var ivTopProfile: ImageView
    private lateinit var barChart: BarChart
    private lateinit var tvTotalIncome: TextView
    private lateinit var tvTotalExpenses: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_reports)

        auth = FirebaseAuth.getInstance()
        database = FirebaseDatabase.getInstance()
        ivTopProfile = findViewById(R.id.ivTopProfile)
        barChart = findViewById(R.id.reportsBarChart)
        tvTotalIncome = findViewById(R.id.tvTotalIncome)
        tvTotalExpenses = findViewById(R.id.tvTotalExpenses)

        setupTopBar()
        setupBottomNavigation()
        loadUserProfile()
        loadTransactionData()
    }

    private fun loadTransactionData() {
        val userId = auth.currentUser?.uid ?: return
        
        // REACTIVE ENGINE: Real-time listener ensures instant UI updates
        database.getReference("users").child(userId).child("transactions")
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    var totalIncome = 0.0
                    var totalExpenses = 0.0
                    val categoryTotals = mutableMapOf<String, Double>()
                    val month = Calendar.getInstance().get(Calendar.MONTH) + 1

                    for (data in snapshot.children) {
                        val t = data.getValue(transactions::class.java) ?: continue
                        val dateParts = t.transaction_date.split("/")
                        if (dateParts.size >= 2 && dateParts[1].toIntOrNull() == month) {
                            if (t.category.type == "Expense") {
                                totalExpenses += t.transaction_amamount
                                val catName = t.category.category_name
                                categoryTotals[catName] = categoryTotals.getOrDefault(catName, 0.0) + t.transaction_amamount
                            } else {
                                totalIncome += t.transaction_amamount
                            }
                        }
                    }

                    // Dynamic Variable Trackers: Slide counter metrics to new balances instantly
                    animateCounter(tvTotalIncome, totalIncome)
                    animateCounter(tvTotalExpenses, totalExpenses)
                    
                    updateBarChart(categoryTotals)
                }
                override fun onCancelled(error: DatabaseError) {}
            })
    }

    private fun animateCounter(view: TextView, target: Double) {
        view.text = String.format(Locale.getDefault(), "R %.2f", target)
        // Simple scale animation to indicate update
        view.animate().scaleX(1.1f).scaleY(1.1f).setDuration(100).withEndAction {
            view.animate().scaleX(1f).scaleY(1f).setDuration(100).start()
        }.start()
    }

    private fun updateBarChart(categoryTotals: Map<String, Double>) {
        if (categoryTotals.isEmpty()) {
            barChart.clear()
            barChart.setNoDataText("No expenses this month")
            barChart.invalidate()
            return
        }

        val entries = ArrayList<BarEntry>()
        val labels = ArrayList<String>()
        var index = 0f
        
        for ((name, total) in categoryTotals) {
            entries.add(BarEntry(index, total.toFloat()))
            labels.add(name)
            index++
        }

        val dataSet = BarDataSet(entries, "Spending by Category")
        dataSet.colors = ColorTemplate.MATERIAL_COLORS.toList()
        dataSet.valueTextColor = Color.WHITE
        dataSet.valueTextSize = 10f

        val data = BarData(dataSet)
        barChart.data = data
        
        // Chart Styling
        barChart.description.isEnabled = false
        barChart.legend.isEnabled = false
        barChart.setPinchZoom(false)
        barChart.setDrawGridBackground(false)
        barChart.setDrawBarShadow(false)
        
        val xAxis = barChart.xAxis
        xAxis.position = XAxis.XAxisPosition.BOTTOM
        xAxis.setDrawGridLines(false)
        xAxis.granularity = 1f
        xAxis.valueFormatter = IndexAxisValueFormatter(labels)
        xAxis.labelRotationAngle = -45f
        xAxis.textColor = Color.WHITE
        
        barChart.axisLeft.setDrawGridLines(false)
        barChart.axisLeft.textColor = Color.WHITE
        barChart.axisRight.isEnabled = false
        
        barChart.animateY(1000)
        barChart.invalidate()
    }

    private fun setupTopBar() {
        findViewById<CardView>(R.id.btnTopProfile).setOnClickListener {
            startActivity(Intent(this, SettingsActivity::class.java))
        }
    }

    private fun loadUserProfile() {
        val userId = auth.currentUser?.uid ?: return
        database.getReference("users").child(userId).child("profile")
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val imageUrl = snapshot.child("profileImageUrl").getValue(String::class.java)
                    if (!imageUrl.isNullOrEmpty()) {
                        com.bumptech.glide.Glide.with(this@ReportsActivity).load(imageUrl).circleCrop().into(ivTopProfile)
                    }
                }
                override fun onCancelled(error: DatabaseError) {}
            })
    }

    private fun setupBottomNavigation() {
        NavigationHelper.setupBottomNavigation(this, 2)
    }
}