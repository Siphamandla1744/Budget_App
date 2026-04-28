package com.example.budget_app

import android.app.DatePickerDialog
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.budget_app.model.Account
import com.example.budget_app.model.Category
import com.example.budget_app.model.transactions
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import java.util.*

class AddExpenseActivity : AppCompatActivity() {

    private lateinit var etAmount: TextInputEditText
    private lateinit var etDescription: TextInputEditText
    private lateinit var spinnerCategory: Spinner
    private lateinit var spinnerAccount: Spinner
    private lateinit var etDate: TextInputEditText
    private lateinit var btnSaveExpense: Button
    private lateinit var tvAddExpenseTitle: TextView
    
    private lateinit var auth: FirebaseAuth
    private lateinit var database: FirebaseDatabase

    private var transactionKey: String? = null
    private var isEditMode = false
    
    private val categoriesList = mutableListOf<Category>()
    private val categoryNames = mutableListOf<String>()
    
    private val accountsList = mutableListOf<Account>()
    private val accountNames = mutableListOf<String>()

    private lateinit var categoryAdapter: ArrayAdapter<String>
    private lateinit var accountAdapter: ArrayAdapter<String>

    private var oldTransaction: transactions? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_expense)

        auth = FirebaseAuth.getInstance()
        database = FirebaseDatabase.getInstance()

        etAmount = findViewById(R.id.etAmount)
        etDescription = findViewById(R.id.etDescription)
        spinnerCategory = findViewById(R.id.spinnerCategory)
        spinnerAccount = findViewById(R.id.spinnerAccount)
        etDate = findViewById(R.id.etDate)
        btnSaveExpense = findViewById(R.id.btnSaveExpense)
        tvAddExpenseTitle = findViewById(R.id.tvAddExpenseTitle)

        categoryAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, categoryNames)
        categoryAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerCategory.adapter = categoryAdapter

        accountAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, accountNames)
        accountAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerAccount.adapter = accountAdapter

        fetchCategoriesAndAccounts()

        transactionKey = intent.getStringExtra("TRANSACTION_KEY")
        if (transactionKey != null) {
            isEditMode = true
            tvAddExpenseTitle.text = "Edit Transaction"
            btnSaveExpense.text = "Update Transaction"
            // Details will be fetched after spinners are loaded in fetchCategoriesAndAccounts
        }

        etDate.setOnClickListener { showDatePicker() }
        btnSaveExpense.setOnClickListener { saveExpense() }
    }

    private fun fetchCategoriesAndAccounts() {
        val userId = auth.currentUser?.uid ?: return
        val userRef = database.getReference("users").child(userId)

        // Fetch Categories
        userRef.child("categories").addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                categoriesList.clear()
                categoryNames.clear()
                for (data in snapshot.children) {
                    val cat = data.getValue(Category::class.java)
                    cat?.let { 
                        categoriesList.add(it)
                        categoryNames.add(it.category_name)
                    }
                }
                categoryAdapter.notifyDataSetChanged()
                
                // Fetch Accounts
                userRef.child("accounts").addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        accountsList.clear()
                        accountNames.clear()
                        for (data in snapshot.children) {
                            val acc = data.getValue(Account::class.java)
                            acc?.let { 
                                accountsList.add(it)
                                accountNames.add(it.name)
                            }
                        }
                        accountAdapter.notifyDataSetChanged()
                        
                        if (isEditMode) fetchTransactionDetails()
                    }
                    override fun onCancelled(error: DatabaseError) {}
                })
            }
            override fun onCancelled(error: DatabaseError) {}
        })
    }

    private fun fetchTransactionDetails() {
        val userId = auth.currentUser?.uid ?: return
        database.getReference("users").child(userId).child("transactions").child(transactionKey!!)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    oldTransaction = snapshot.getValue(transactions::class.java)
                    oldTransaction?.let { trans ->
                        etAmount.setText(trans.transaction_amamount.toString())
                        etDescription.setText(trans.transaction_name)
                        etDate.setText(trans.transaction_date)
                        
                        val catIdx = categoryNames.indexOf(trans.category.category_name)
                        if (catIdx != -1) spinnerCategory.setSelection(catIdx)
                        
                        val accIdx = accountsList.indexOfFirst { it.accountId == trans.accountId }
                        if (accIdx != -1) spinnerAccount.setSelection(accIdx)
                    }
                }
                override fun onCancelled(error: DatabaseError) {}
            })
    }

    private fun showDatePicker() {
        val c = Calendar.getInstance()
        DatePickerDialog(this, { _, y, m, d ->
            etDate.setText("$d/${m + 1}/$y")
        }, c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH)).show()
    }

    private fun saveExpense() {
        val amount = etAmount.text.toString().toDoubleOrNull() ?: 0.0
        val desc = etDescription.text.toString().trim()
        val date = etDate.text.toString().trim()
        
        if (amount <= 0 || desc.isEmpty() || date.isEmpty() || accountsList.isEmpty()) {
            Toast.makeText(this, "Please fill all fields and ensure an account exists", Toast.LENGTH_SHORT).show()
            return
        }

        val selectedCategory = categoriesList[spinnerCategory.selectedItemPosition]
        val selectedAccount = accountsList[spinnerAccount.selectedItemPosition]
        val userId = auth.currentUser?.uid ?: return

        val transRef = if (isEditMode) {
            database.getReference("users").child(userId).child("transactions").child(transactionKey!!)
        } else {
            database.getReference("users").child(userId).child("transactions").push()
        }

        val newTransaction = transactions(
            transaction_id = transRef.key ?: "",
            transaction_name = desc,
            category = selectedCategory,
            transaction_amamount = amount,
            transaction_date = date,
            accountId = selectedAccount.accountId
        )

        // Update Balance logic
        if (isEditMode && oldTransaction != null) {
            // 1. Revert old transaction balance
            updateAccountBalance(oldTransaction!!.accountId, oldTransaction!!.transaction_amamount, oldTransaction!!.category.type, revert = true)
        }
        
        // 2. Apply new transaction balance
        updateAccountBalance(newTransaction.accountId, newTransaction.transaction_amamount, newTransaction.category.type, revert = false)

        transRef.setValue(newTransaction).addOnCompleteListener {
            Toast.makeText(this, "Transaction Saved", Toast.LENGTH_SHORT).show()
            finish()
        }
    }

    private fun updateAccountBalance(accId: String, amount: Double, type: String, revert: Boolean) {
        val userId = auth.currentUser?.uid ?: return
        val accRef = database.getReference("users").child(userId).child("accounts").child(accId)
        
        accRef.runTransaction(object : com.google.firebase.database.Transaction.Handler {
            override fun doTransaction(currentData: com.google.firebase.database.MutableData): com.google.firebase.database.Transaction.Result {
                val acc = currentData.getValue(Account::class.java) ?: return com.google.firebase.database.Transaction.success(currentData)
                
                val isExpense = type == "Expense"
                val adjustment = if (revert) {
                    if (isExpense) amount else -amount
                } else {
                    if (isExpense) -amount else amount
                }
                
                val updatedAccount = acc.copy(balance = acc.balance + adjustment)
                currentData.value = updatedAccount
                return com.google.firebase.database.Transaction.success(currentData)
            }
            override fun onComplete(error: DatabaseError?, committed: Boolean, snapshot: DataSnapshot?) {}
        })
    }
}