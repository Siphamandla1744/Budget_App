package com.example.budget_app

import android.app.DatePickerDialog
import android.os.Bundle
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.budget_app.model.Goal
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import java.util.*

class activity_creategoal : AppCompatActivity() {

    private lateinit var etGoalName: TextInputEditText
    private lateinit var etTargetAmount: TextInputEditText
    private lateinit var etCurrentAmount: TextInputEditText
    private lateinit var etTargetDate: TextInputEditText
    private lateinit var btnSaveGoal: Button
    
    private lateinit var auth: FirebaseAuth
    private lateinit var database: FirebaseDatabase

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_creategoal)

        auth = FirebaseAuth.getInstance()
        database = FirebaseDatabase.getInstance()

        // Initialize UI components
        etGoalName = findViewById(R.id.etGoalName)
        etTargetAmount = findViewById(R.id.etTargetAmount)
        etCurrentAmount = findViewById(R.id.etCurrentAmount)
        etTargetDate = findViewById(R.id.etTargetDate)
        btnSaveGoal = findViewById(R.id.btnSaveGoal)

        // Date Picker
        etTargetDate.setOnClickListener {
            showDatePicker()
        }

        // Set click listener for Save Button
        btnSaveGoal.setOnClickListener {
            saveGoal()
        }
    }

    private fun showDatePicker() {
        val calendar = Calendar.getInstance()
        val year = calendar.get(Calendar.YEAR)
        val month = calendar.get(Calendar.MONTH)
        val day = calendar.get(Calendar.DAY_OF_MONTH)

        val datePickerDialog = DatePickerDialog(this, { _, selectedYear, selectedMonth, selectedDay ->
            val date = "$selectedDay/${selectedMonth + 1}/$selectedYear"
            etTargetDate.setText(date)
        }, year, month, day)

        datePickerDialog.show()
    }

    private fun saveGoal() {
        val name = etGoalName.text.toString().trim()
        val targetAmountStr = etTargetAmount.text.toString().trim()
        val currentAmountStr = etCurrentAmount.text.toString().trim()
        val date = etTargetDate.text.toString().trim()

        if (name.isEmpty() || targetAmountStr.isEmpty() || date.isEmpty()) {
            Toast.makeText(this, "Please fill in required fields", Toast.LENGTH_SHORT).show()
            return
        }

        val targetAmount = targetAmountStr.toDoubleOrNull() ?: 0.0
        val currentAmount = currentAmountStr.toDoubleOrNull() ?: 0.0
        val userId = auth.currentUser?.uid ?: return
        val goalRef = database.getReference("users").child(userId).child("goals").push()

        val goal = Goal(
            goalId = goalRef.key ?: "",
            name = name,
            targetAmount = targetAmount,
            currentAmount = currentAmount,
            targetDate = date
        )

        goalRef.setValue(goal).addOnCompleteListener { task ->
            if (task.isSuccessful) {
                Toast.makeText(this, "Goal Created Successfully", Toast.LENGTH_SHORT).show()
                finish()
            } else {
                Toast.makeText(this, "Failed to create goal", Toast.LENGTH_SHORT).show()
            }
        }
    }
}