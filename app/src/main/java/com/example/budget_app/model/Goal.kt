package com.example.budget_app.model

data class Goal(
    val goalId: String = "",
    val name: String = "",
    val targetAmount: Double = 0.0,
    val currentAmount: Double = 0.0,
    val targetDate: String = ""
)