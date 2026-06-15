package com.example.budget_app

import android.app.DatePickerDialog
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.util.Base64
import android.view.View
import android.widget.*
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
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
import java.io.ByteArrayOutputStream
import java.util.*

class AddExpenseActivity : AppCompatActivity() {

    private lateinit var etAmount: TextInputEditText
    private lateinit var etDescription: TextInputEditText
    private lateinit var spinnerCategory: Spinner
    private lateinit var spinnerAccount: Spinner
    private lateinit var etDate: TextInputEditText
    private lateinit var btnSelectImage: Button
    private lateinit var ivReceiptPreview: ImageView
    private lateinit var btnDeleteReceipt: Button
    private lateinit var btnSaveExpense: Button
    private lateinit var tvAddExpenseTitle: TextView
    private lateinit var progressBar: ProgressBar
    
    private lateinit var auth: FirebaseAuth
    private lateinit var database: FirebaseDatabase

    private var transactionKey: String? = null
    private var isEditMode = false
    private var imageUri: Uri? = null
    private var existingReceiptBase64: String? = null
    private var isReceiptDeleted = false
    
    private val categoriesList = mutableListOf<Category>()
    private val categoryNames = mutableListOf<String>()
    
    private val accountsList = mutableListOf<Account>()
    private val accountNames = mutableListOf<String>()

    private lateinit var categoryAdapter: ArrayAdapter<String>
    private lateinit var accountAdapter: ArrayAdapter<String>

    private var oldTransaction: transactions? = null

    private val pickImage = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let {
            imageUri = it
            isReceiptDeleted = false
            ivReceiptPreview.visibility = View.VISIBLE
            btnDeleteReceipt.visibility = View.VISIBLE
            ivReceiptPreview.setImageURI(it)
        }
    }

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
        btnSelectImage = findViewById(R.id.btnSelectImage)
        ivReceiptPreview = findViewById(R.id.ivReceiptPreview)
        btnDeleteReceipt = findViewById(R.id.btnDeleteReceipt)
        btnSaveExpense = findViewById(R.id.btnSaveExpense)
        tvAddExpenseTitle = findViewById(R.id.tvAddExpenseTitle)
        progressBar = findViewById(R.id.progressBar)

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
        }

        etDate.setOnClickListener { showDatePicker() }
        btnSelectImage.setOnClickListener { pickImage.launch("image/*") }
        
        btnDeleteReceipt.setOnClickListener {
            showDeleteReceiptConfirmation()
        }
        
        btnSaveExpense.setOnClickListener { saveExpense() }

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                finish()
            }
        })
    }

    private fun showDeleteReceiptConfirmation() {
        AlertDialog.Builder(this)
            .setTitle("Remove Receipt")
            .setMessage("Are you sure you want to remove the receipt image?")
            .setPositiveButton("Remove") { _, _ ->
                imageUri = null
                existingReceiptBase64 = null
                isReceiptDeleted = true
                ivReceiptPreview.visibility = View.GONE
                btnDeleteReceipt.visibility = View.GONE
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun fetchCategoriesAndAccounts() {
        val userId = auth.currentUser?.uid ?: return
        val userRef = database.getReference("users").child(userId)

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
                        
                        if (!trans.receiptUrl.isNullOrEmpty()) {
                            existingReceiptBase64 = trans.receiptUrl
                            ivReceiptPreview.visibility = View.VISIBLE
                            btnDeleteReceipt.visibility = View.VISIBLE
                            
                            try {
                                if (trans.receiptUrl.startsWith("http")) {
                                    com.bumptech.glide.Glide.with(this@AddExpenseActivity).load(trans.receiptUrl).into(ivReceiptPreview)
                                } else {
                                    val imageBytes = Base64.decode(trans.receiptUrl, Base64.DEFAULT)
                                    val decodedImage = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
                                    ivReceiptPreview.setImageBitmap(decodedImage)
                                }
                            } catch (e: Exception) {
                                ivReceiptPreview.visibility = View.GONE
                            }
                        }
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
        val amountStr = etAmount.text.toString().trim()
        val desc = etDescription.text.toString().trim()
        val date = etDate.text.toString().trim()
        
        if (amountStr.isEmpty() || desc.isEmpty() || date.isEmpty() || accountsList.isEmpty() || categoriesList.isEmpty()) {
            Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show()
            return
        }

        val amount = amountStr.toDoubleOrNull() ?: 0.0
        val selectedCategory = categoriesList[spinnerCategory.selectedItemPosition]
        
        if (selectedCategory.type == "Expense" && selectedCategory.budget <= 0) {
            Toast.makeText(this, "Category has no budget!", Toast.LENGTH_LONG).show()
            return
        }

        btnSaveExpense.isEnabled = false
        progressBar.visibility = View.VISIBLE
        
        var receiptToSave: String? = if (isReceiptDeleted) null else existingReceiptBase64
        
        if (imageUri != null) {
            receiptToSave = convertUriToBase64(imageUri!!)
        }
        
        finalizeSave(amount, desc, date, receiptToSave)
    }

    private fun convertUriToBase64(uri: Uri): String? {
        return try {
            val inputStream = contentResolver.openInputStream(uri)
            val originalBitmap = BitmapFactory.decodeStream(inputStream)
            
            val width = originalBitmap.width
            val height = originalBitmap.height
            val ratio = width.toFloat() / height.toFloat()
            val newWidth = if (width > height) 400 else (400 * ratio).toInt()
            val newHeight = if (height > width) 400 else (400 / ratio).toInt()
            
            val scaledBitmap = Bitmap.createScaledBitmap(originalBitmap, newWidth, newHeight, true)
            
            val outputStream = ByteArrayOutputStream()
            scaledBitmap.compress(Bitmap.CompressFormat.JPEG, 50, outputStream)
            val byteArray = outputStream.toByteArray()
            Base64.encodeToString(byteArray, Base64.DEFAULT)
        } catch (e: Exception) {
            null
        }
    }

    private fun finalizeSave(amount: Double, desc: String, date: String, receiptUrl: String?) {
        val userId = auth.currentUser?.uid ?: return
        val selectedCategory = categoriesList[spinnerCategory.selectedItemPosition]
        val selectedAccount = accountsList[spinnerAccount.selectedItemPosition]

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
            accountId = selectedAccount.accountId,
            receiptUrl = receiptUrl
        )

        // Perform balance updates before saving the transaction
        if (isEditMode && oldTransaction != null) {
            updateAccountBalance(oldTransaction!!.accountId, oldTransaction!!.transaction_amamount, oldTransaction!!.category.type, revert = true)
        }
        
        updateAccountBalance(newTransaction.accountId, newTransaction.transaction_amamount, newTransaction.category.type, revert = false)

        transRef.setValue(newTransaction).addOnCompleteListener { task ->
            if (isFinishing || isDestroyed) return@addOnCompleteListener
            
            if (task.isSuccessful) {
                if (!isEditMode) {
                    awardXP(10) // Only award XP for NEW transactions
                }
                Toast.makeText(this, "Transaction Saved", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "Failed to save transaction: ${task.exception?.message}", Toast.LENGTH_LONG).show()
            }
            progressBar.visibility = View.GONE
            btnSaveExpense.isEnabled = true
            finish()
        }
    }

    private fun awardXP(amount: Int) {
        val userId = auth.currentUser?.uid ?: return
        val gamificationRef = database.getReference("users").child(userId).child("gamification")
        
        gamificationRef.runTransaction(object : com.google.firebase.database.Transaction.Handler {
            override fun doTransaction(currentData: com.google.firebase.database.MutableData): com.google.firebase.database.Transaction.Result {
                var xp = currentData.child("xp").getValue(Int::class.java) ?: 0
                var level = currentData.child("level").getValue(Int::class.java) ?: 1
                
                xp += amount
                
                // Level up logic: each level requires level * 200 XP
                val xpThreshold = level * 200
                if (xp >= xpThreshold) {
                    level++
                }
                
                currentData.child("xp").value = xp
                currentData.child("level").value = level
                
                return com.google.firebase.database.Transaction.success(currentData)
            }
            override fun onComplete(error: DatabaseError?, committed: Boolean, snapshot: DataSnapshot?) {}
        })
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
