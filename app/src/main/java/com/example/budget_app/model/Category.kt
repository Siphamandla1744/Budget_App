package com.example.budget_app.model

data class Category(
    val category_name: String = "",
    val color: String = "#757575",
    val iconRes: Int = android.R.drawable.ic_menu_gallery,
    val type: String = "Expense", // "Expense" or "Income"
    val budget: Double = 0.0
)
