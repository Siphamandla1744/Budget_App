package com.example.budget_app

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.GravityCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.budget_app.Transaction_Adapter.TransactionAdapter
import com.example.budget_app.model.Account
import com.example.budget_app.model.Category
import com.example.budget_app.model.transactions
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.google.android.material.navigation.NavigationView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*

class TransactionHistoryActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var database: FirebaseDatabase
    private lateinit var drawerLayout: DrawerLayout
    private lateinit var rvTransactionHistory: RecyclerView
    private lateinit var transactionAdapter: TransactionAdapter
    private lateinit var etSearch: EditText
    private lateinit var chipGroupCategories: ChipGroup
    private lateinit var tvTotalAmount: TextView
    private lateinit var tvEmptyState: TextView

    private val allTransactions = mutableListOf<transactions>()
    private val allKeys = mutableListOf<String>()
    private val filteredTransactions = mutableListOf<transactions>()
    
    private var selectedCategory: String = "All"
    private var searchQuery: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_transaction_history)

        auth = FirebaseAuth.getInstance()
        database = FirebaseDatabase.getInstance()
        drawerLayout = findViewById(R.id.drawer_layout)
        rvTransactionHistory = findViewById(R.id.rvTransactionHistory)
        etSearch = findViewById(R.id.etSearch)
        chipGroupCategories = findViewById(R.id.chipGroupCategories)
        tvTotalAmount = findViewById(R.id.tvTotalAmount)
        tvEmptyState = findViewById(R.id.tvEmptyState)
        val navView: NavigationView = findViewById(R.id.nav_view)

        rvTransactionHistory.layoutManager = LinearLayoutManager(this)
        transactionAdapter = TransactionAdapter(
            filteredTransactions,
            onTransactionClick = { transaction -> showEditTransaction(transaction) },
            onTransactionLongClick = { transaction -> showDeleteConfirmation(transaction) }
        )
        rvTransactionHistory.adapter = transactionAdapter

        etSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                searchQuery = s.toString().lowercase()
                applyFilters()
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        findViewById<ImageView>(R.id.ivMenu).setOnClickListener {
            drawerLayout.openDrawer(GravityCompat.START)
        }

        fetchTransactionsAndCategories()
    }

    private fun fetchTransactionsAndCategories() {
        val userId = auth.currentUser?.uid ?: return
        val userRef = database.getReference("users").child(userId)

        userRef.child("categories").addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                chipGroupCategories.removeAllViews()
                addChip("All")
                for (data in snapshot.children) {
                    val cat = data.getValue(Category::class.java)
                    cat?.let { addChip(it.category_name) }
                }
            }
            override fun onCancelled(error: DatabaseError) {}
        })

        userRef.child("transactions").addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                allTransactions.clear()
                allKeys.clear()
                for (data in snapshot.children) {
                    val transaction = data.getValue(transactions::class.java)
                    if (transaction != null) {
                        allTransactions.add(transaction)
                        allKeys.add(data.key ?: "")
                    }
                }
                allTransactions.reverse()
                allKeys.reverse()
                applyFilters()
            }
            override fun onCancelled(error: DatabaseError) {}
        })
    }

    private fun addChip(name: String) {
        val chip = Chip(this).apply {
            text = name
            isCheckable = true
            if (name == "All") isChecked = true
            setOnClickListener {
                selectedCategory = name
                applyFilters()
            }
        }
        chipGroupCategories.addView(chip)
    }

    private fun applyFilters() {
        filteredTransactions.clear()
        var total = 0.0
        for (trans in allTransactions) {
            val matchesCategory = selectedCategory == "All" || trans.category.category_name == selectedCategory
            val matchesSearch = trans.transaction_name.lowercase().contains(searchQuery)
            if (matchesCategory && matchesSearch) {
                filteredTransactions.add(trans)
                total += trans.transaction_amamount
            }
        }
        transactionAdapter.updateList(filteredTransactions)
        tvTotalAmount.text = "R ${String.format("%.2f", total)}"
        tvEmptyState.visibility = if (filteredTransactions.isEmpty()) View.VISIBLE else View.GONE
    }

    private fun showDeleteConfirmation(transaction: transactions) {
        val key = allKeys[allTransactions.indexOf(transaction)]
        AlertDialog.Builder(this)
            .setTitle("Delete Transaction")
            .setMessage("Refund amount to account?")
            .setPositiveButton("Delete & Refund") { _, _ ->
                val userId = auth.currentUser?.uid ?: return@setPositiveButton
                
                // 1. Refund to account balance
                refundTransaction(transaction)
                
                // 2. Delete transaction record
                database.getReference("users").child(userId).child("transactions").child(key).removeValue()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun refundTransaction(trans: transactions) {
        val userId = auth.currentUser?.uid ?: return
        val accRef = database.getReference("users").child(userId).child("accounts").child(trans.accountId)
        
        accRef.runTransaction(object : com.google.firebase.database.Transaction.Handler {
            override fun doTransaction(currentData: com.google.firebase.database.MutableData): com.google.firebase.database.Transaction.Result {
                val acc = currentData.getValue(Account::class.java) ?: return com.google.firebase.database.Transaction.success(currentData)
                val isExpense = trans.category.type == "Expense"
                val refundAmount = if (isExpense) trans.transaction_amamount else -trans.transaction_amamount
                currentData.value = acc.copy(balance = acc.balance + refundAmount)
                return com.google.firebase.database.Transaction.success(currentData)
            }
            override fun onComplete(e: DatabaseError?, c: Boolean, s: DataSnapshot?) {}
        })
    }

    private fun showEditTransaction(transaction: transactions) {
        val key = allKeys[allTransactions.indexOf(transaction)]
        val intent = Intent(this, AddExpenseActivity::class.java)
        intent.putExtra("TRANSACTION_KEY", key)
        startActivity(intent)
    }
}