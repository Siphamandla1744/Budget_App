package com.example.budget_app

import com.google.firebase.database.FirebaseDatabase
import java.text.SimpleDateFormat
import java.util.*

object NotificationRepository {
    private val database = FirebaseDatabase.getInstance()

    fun logEvent(userId: String, type: String, message: String) {
        val notifRef = database.getReference("users").child(userId).child("notifications").push()
        
        val sdf = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
        val currentDateTime = sdf.format(Date())

        val notification = mapOf(
            "id" to (notifRef.key ?: ""),
            "type" to type,
            "message" to message,
            "timestamp" to currentDateTime
        )

        notifRef.setValue(notification)
    }
}