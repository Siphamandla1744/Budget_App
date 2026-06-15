package com.example.budget_app.model

data class transactions(
    val transaction_id: String = "",
    val transaction_name: String = "",
    val category: Category = Category(),
    val transaction_amamount: Double = 0.0,
    val transaction_date: String = "",
    val accountId: String = "",
    val receiptUrl: String? = null
)
