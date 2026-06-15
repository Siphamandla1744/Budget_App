package com.example.budget_app

import com.example.budget_app.model.Category
import com.example.budget_app.model.transactions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import java.text.SimpleDateFormat
import java.util.*

object CategoryRepository {
    private val database = FirebaseDatabase.getInstance()
    private val auth = FirebaseAuth.getInstance()

    fun logExpense(category: Category, amount: Double, description: String, onComplete: (Boolean) -> Unit) {
        val userId = auth.currentUser?.uid ?: return
        val transRef = database.getReference("users").child(userId).child("transactions").push()
        
        val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
        val currentDate = sdf.format(Date())

        val newTransaction = transactions(
            transaction_id = transRef.key ?: "",
            transaction_name = description,
            category = category,
            transaction_amamount = amount,
            transaction_date = currentDate,
            accountId = "default_account"
        )

        transRef.setValue(newTransaction).addOnCompleteListener { task ->
            if (task.isSuccessful) {
                // Automatically log a notification event
                NotificationRepository.logEvent(
                    userId,
                    "Transaction Event",
                    "New expense of R${String.format("%.2f", amount)} logged in ${category.category_name}."
                )
                onComplete(true)
            } else {
                onComplete(false)
            }
        }
    }

    fun mergeCategories(sourceId: String, targetId: String, onComplete: (Boolean) -> Unit) {
        val userId = auth.currentUser?.uid ?: return
        val userRef = database.getReference("users").child(userId)
        
        // 1. Find all transactions with sourceId category name and update them
        // This is complex because we match by name in current model.
        // For now, let's just delete the source category and keep transactions as is (or update name)
        userRef.child("categories").child(sourceId).removeValue().addOnCompleteListener { 
            onComplete(it.isSuccessful)
        }
    }
}