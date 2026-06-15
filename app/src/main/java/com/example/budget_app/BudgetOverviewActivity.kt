package com.example.budget_app

import android.os.Bundle
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import com.example.budget_app.model.transactions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import java.util.Calendar
import java.util.Locale

class BudgetOverviewActivity : AppCompatActivity() {
    private lateinit var auth: FirebaseAuth
    private lateinit var database: FirebaseDatabase
    private lateinit var tvBalance: TextView
    private lateinit var tvTotalIncome: TextView
    private lateinit var tvTotalExpenses: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_budget_overview)

        auth = FirebaseAuth.getInstance()
        database = FirebaseDatabase.getInstance()
        
        tvBalance = findViewById(R.id.tvBalance)
        tvTotalIncome = findViewById(R.id.tvTotalIncome)
        tvTotalExpenses = findViewById(R.id.tvTotalExpenses)

        loadBudgetData()
        NavigationHelper.setupBottomNavigation(this, 1)
    }

    private fun loadBudgetData() {
        val userId = auth.currentUser?.uid ?: return
        database.getReference("users").child(userId).child("transactions")
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    var totalIncome = 0.0
                    var totalExpenses = 0.0
                    val month = Calendar.getInstance().get(Calendar.MONTH) + 1

                    for (data in snapshot.children) {
                        val t = data.getValue(transactions::class.java) ?: continue
                        val dateParts = t.transaction_date.split("/")
                        if (dateParts.size >= 2 && dateParts[1].toIntOrNull() == month) {
                            if (t.category.type == "Expense") {
                                totalExpenses += t.transaction_amamount
                            } else {
                                totalIncome += t.transaction_amamount
                            }
                        }
                    }

                    val balance = totalIncome - totalExpenses
                    tvBalance.text = String.format(Locale.getDefault(), "R %.2f", balance)
                    tvTotalIncome.text = String.format(Locale.getDefault(), "R %.2f", totalIncome)
                    tvTotalExpenses.text = String.format(Locale.getDefault(), "R %.2f", totalExpenses)
                }
                override fun onCancelled(error: DatabaseError) {}
            })
    }
}