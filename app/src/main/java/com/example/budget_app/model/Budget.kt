package com.example.budget_app.model

data class Budget(
    val budgetId: String = "",
    val categoryName: String = "",
    val amount: Double = 0.0,
    val period: String = "Monthly"
)
