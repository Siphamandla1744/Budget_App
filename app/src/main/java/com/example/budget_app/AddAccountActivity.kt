package com.example.budget_app

import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.example.budget_app.model.Account
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class AddAccountActivity : AppCompatActivity() {

    private lateinit var etAccountName: TextInputEditText
    private lateinit var etBalance: TextInputEditText
    private lateinit var btnSaveAccount: Button
    private lateinit var btnDeleteAccount: Button
    private lateinit var tvTitle: TextView
    
    private lateinit var auth: FirebaseAuth
    private lateinit var database: FirebaseDatabase

    private var accountId: String? = null
    private var isEditMode = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_account)

        auth = FirebaseAuth.getInstance()
        database = FirebaseDatabase.getInstance()

        etAccountName = findViewById(R.id.etAccountName)
        etBalance = findViewById(R.id.etBalance)
        btnSaveAccount = findViewById(R.id.btnSaveAccount)
        btnDeleteAccount = findViewById(R.id.btnDeleteAccount)
        tvTitle = findViewById(R.id.tvTitle)

        accountId = intent.getStringExtra("ACCOUNT_ID")
        if (accountId != null) {
            isEditMode = true
            tvTitle.text = "Edit Account"
            btnSaveAccount.text = "Update Account"
            btnDeleteAccount.visibility = android.view.View.VISIBLE
            fetchAccountDetails(accountId!!)
        }

        btnSaveAccount.setOnClickListener {
            saveAccount()
        }

        btnDeleteAccount.setOnClickListener {
            showDeleteConfirmation()
        }
    }

    private fun fetchAccountDetails(id: String) {
        val userId = auth.currentUser?.uid ?: return
        database.getReference("users").child(userId).child("accounts").child(id)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val account = snapshot.getValue(Account::class.java)
                    if (account != null) {
                        etAccountName.setText(account.name)
                        etBalance.setText(account.balance.toString())
                    }
                }
                override fun onCancelled(error: DatabaseError) {}
            })
    }

    private fun saveAccount() {
        val name = etAccountName.text.toString().trim()
        val balanceStr = etBalance.text.toString().trim()

        if (name.isEmpty() || balanceStr.isEmpty()) {
            Toast.makeText(this, "Please fill in all fields", Toast.LENGTH_SHORT).show()
            return
        }

        val balance = balanceStr.toDoubleOrNull() ?: 0.0
        val userId = auth.currentUser?.uid ?: return
        
        val accountRef = if (isEditMode && accountId != null) {
            database.getReference("users").child(userId).child("accounts").child(accountId!!)
        } else {
            database.getReference("users").child(userId).child("accounts").push()
        }

        val account = Account(
            accountId = accountRef.key ?: "",
            name = name,
            balance = balance,
            type = "Custom"
        )

        accountRef.setValue(account).addOnCompleteListener { task ->
            if (task.isSuccessful) {
                val msg = if (isEditMode) "Account updated" else "Account added"
                Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
                finish()
            } else {
                Toast.makeText(this, "Failed to save account", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun showDeleteConfirmation() {
        AlertDialog.Builder(this)
            .setTitle("Delete Account")
            .setMessage("Are you sure you want to delete this account?")
            .setPositiveButton("Delete") { _, _ ->
                val userId = auth.currentUser?.uid ?: return@setPositiveButton
                accountId?.let {
                    database.getReference("users").child(userId).child("accounts").child(it).removeValue()
                        .addOnCompleteListener { task ->
                            if (task.isSuccessful) {
                                Toast.makeText(this, "Account deleted", Toast.LENGTH_SHORT).show()
                                finish()
                            }
                        }
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
}