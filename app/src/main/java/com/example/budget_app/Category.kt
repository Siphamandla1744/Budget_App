package com.example.budget_app

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import androidx.core.view.GravityCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.recyclerview.widget.RecyclerView
import com.example.budget_app.model.Category
import com.example.budget_app.model.transactions
import com.github.mikephil.charting.charts.PieChart
import com.github.mikephil.charting.data.PieData
import com.github.mikephil.charting.data.PieDataSet
import com.github.mikephil.charting.data.PieEntry
import com.github.mikephil.charting.utils.ColorTemplate
import com.google.android.material.navigation.NavigationView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*

class Category : AppCompatActivity() {

    private lateinit var rvCategories: RecyclerView
    private lateinit var tabExpenses: View
    private lateinit var tabIncome: View
    private lateinit var tvExpensesTab: TextView
    private lateinit var tvIncomeTab: TextView
    private lateinit var indicatorExpenses: View
    private lateinit var indicatorIncome: View
    private lateinit var drawerLayout: DrawerLayout
    private lateinit var pieChart: PieChart
    private lateinit var auth: FirebaseAuth
    private lateinit var database: FirebaseDatabase

    private var isExpensesTabSelected = true
    private val expenseCategories = mutableListOf<CategoryItem>()
    private val incomeCategories = mutableListOf<CategoryItem>()
    private val categorySpending = mutableMapOf<String, Double>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_category)

        auth = FirebaseAuth.getInstance()
        database = FirebaseDatabase.getInstance()
        drawerLayout = findViewById(R.id.drawer_layout)
        pieChart = findViewById(R.id.categoryPieChart)
        val navView: NavigationView = findViewById(R.id.nav_view)

        // Initialize Views
        rvCategories = findViewById(R.id.rv_categories)
        tabExpenses = findViewById(R.id.tabExpenses)
        tabIncome = findViewById(R.id.tabIncome)
        tvExpensesTab = findViewById(R.id.tvExpensesTab)
        tvIncomeTab = findViewById(R.id.tvIncomeTab)
        indicatorExpenses = findViewById(R.id.indicatorExpenses)
        indicatorIncome = findViewById(R.id.indicatorIncome)
        val ivMenu = findViewById<ImageView>(R.id.ivMenu)
        val fabAddCategory = findViewById<ImageView>(R.id.fab_add_category)

        ivMenu.setOnClickListener {
            drawerLayout.openDrawer(GravityCompat.START)
        }

        fabAddCategory.setOnClickListener {
            showAddCategoryDialog()
        }

        navView.setNavigationItemSelectedListener { menuItem ->
            when (menuItem.itemId) {
                R.id.nav_home -> {
                    startActivity(Intent(this, MainActivity::class.java))
                    finish()
                }
                R.id.nav_categories -> drawerLayout.closeDrawer(GravityCompat.START)
                R.id.nav_goals -> startActivity(Intent(this, activity_creategoal::class.java))
                R.id.nav_history -> startActivity(Intent(this, TransactionHistoryActivity::class.java))
                R.id.nav_help -> Toast.makeText(this, "Help Centre", Toast.LENGTH_SHORT).show()
                R.id.nav_logout -> {
                    auth.signOut()
                    startActivity(Intent(this, activity_login::class.java))
                    finish()
                }
            }
            drawerLayout.closeDrawer(GravityCompat.START)
            true
        }

        setupTabs()
        fetchSpendingAndCategories()

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }

    private fun setupTabs() {
        tabExpenses.setOnClickListener {
            if (!isExpensesTabSelected) {
                isExpensesTabSelected = true
                updateTabUI()
                updateCategoryList()
                updatePieChart()
            }
        }

        tabIncome.setOnClickListener {
            if (isExpensesTabSelected) {
                isExpensesTabSelected = false
                updateTabUI()
                updateCategoryList()
                updatePieChart()
            }
        }
    }

    private fun updateTabUI() {
        val activeColor = Color.parseColor("#2E7D32")
        val inactiveColor = Color.parseColor("#757575")

        if (isExpensesTabSelected) {
            tvExpensesTab.setTextColor(activeColor)
            indicatorExpenses.visibility = View.VISIBLE
            tvIncomeTab.setTextColor(inactiveColor)
            indicatorIncome.visibility = View.INVISIBLE
        } else {
            tvExpensesTab.setTextColor(inactiveColor)
            indicatorExpenses.visibility = View.INVISIBLE
            tvIncomeTab.setTextColor(activeColor)
            indicatorIncome.visibility = View.VISIBLE
        }
    }

    private fun fetchSpendingAndCategories() {
        val userId = auth.currentUser?.uid ?: return
        val transRef = database.getReference("users").child(userId).child("transactions")
        val catRef = database.getReference("users").child(userId).child("categories")

        // First fetch transactions to calculate spending
        transRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                categorySpending.clear()
                for (data in snapshot.children) {
                    val trans = data.getValue(transactions::class.java)
                    if (trans != null) {
                        val catName = trans.category.category_name
                        val current = categorySpending.getOrDefault(catName, 0.0)
                        categorySpending[catName] = current + trans.transaction_amamount
                    }
                }
                // Then fetch categories
                catRef.addValueEventListener(object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        expenseCategories.clear()
                        incomeCategories.clear()

                        for (data in snapshot.children) {
                            val cat = data.getValue(com.example.budget_app.model.Category::class.java)
                            if (cat != null) {
                                val item = CategoryItem(data.key ?: "", cat.category_name, cat.color, cat.iconRes, cat.budget)
                                if (cat.type == "Expense") {
                                    expenseCategories.add(item)
                                } else {
                                    incomeCategories.add(item)
                                }
                            }
                        }

                        if (expenseCategories.isEmpty() && incomeCategories.isEmpty()) {
                            seedDefaultCategories()
                        } else {
                            updateCategoryList()
                            updatePieChart()
                        }
                    }

                    override fun onCancelled(error: DatabaseError) {}
                })
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(this@Category, "Error: ${error.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun updatePieChart() {
        val entries = ArrayList<PieEntry>()
        val categories = if (isExpensesTabSelected) expenseCategories else incomeCategories
        val colors = ArrayList<Int>()

        for (cat in categories) {
            val spent = categorySpending.getOrDefault(cat.name, 0.0)
            if (spent > 0) {
                entries.add(PieEntry(spent.toFloat(), cat.name))
                try {
                    colors.add(Color.parseColor(cat.colorCode))
                } catch (e: Exception) {
                    colors.add(Color.GRAY)
                }
            }
        }

        if (entries.isEmpty()) {
            pieChart.clear()
            pieChart.setNoDataText("No data available for this period")
            pieChart.invalidate()
            return
        }

        val dataSet = PieDataSet(entries, "")
        dataSet.colors = colors
        dataSet.valueTextColor = Color.WHITE
        dataSet.valueTextSize = 12f
        dataSet.sliceSpace = 3f

        val data = PieData(dataSet)
        pieChart.data = data
        pieChart.description.isEnabled = false
        pieChart.centerText = if (isExpensesTabSelected) "Expenses" else "Income"
        pieChart.setCenterTextSize(18f)
        pieChart.setHoleColor(Color.TRANSPARENT)
        pieChart.animateY(1000)
        pieChart.legend.isEnabled = false
        pieChart.setDrawEntryLabels(true)
        pieChart.setEntryLabelColor(Color.WHITE)
        pieChart.setEntryLabelTextSize(10f)
        pieChart.invalidate()
    }

    private fun seedDefaultCategories() {
        val userId = auth.currentUser?.uid ?: return
        val catRef = database.getReference("users").child(userId).child("categories")

        val defaults = listOf(
            com.example.budget_app.model.Category("Food & Drink", "#FF9800", android.R.drawable.ic_menu_gallery, "Expense"),
            com.example.budget_app.model.Category("Shopping", "#E91E63", android.R.drawable.ic_menu_gallery, "Expense"),
            com.example.budget_app.model.Category("Transport", "#FFEB3B", android.R.drawable.ic_menu_gallery, "Expense"),
            com.example.budget_app.model.Category("Salary", "#4CAF50", android.R.drawable.ic_menu_gallery, "Income")
        )

        for (cat in defaults) {
            catRef.push().setValue(cat)
        }
    }

    private fun updateCategoryList() {
        val categories = if (isExpensesTabSelected) {
            expenseCategories
        } else {
            incomeCategories
        }
        rvCategories.adapter = CategoryAdapter(categories)
    }

    private fun showAddCategoryDialog(itemToEdit: CategoryItem? = null) {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_add_category, null)
        val etName = dialogView.findViewById<EditText>(R.id.etCategoryName)
        val etBudget = dialogView.findViewById<EditText>(R.id.etCategoryBudget)
        
        var selectedColor = itemToEdit?.colorCode ?: "#757575"
        if (itemToEdit != null) {
            etName.setText(itemToEdit.name)
            if (itemToEdit.budget > 0) {
                etBudget.setText(itemToEdit.budget.toString())
            }
        }
        
        dialogView.findViewById<View>(R.id.colorRed).setOnClickListener { selectedColor = "#F44336" }
        dialogView.findViewById<View>(R.id.colorBlue).setOnClickListener { selectedColor = "#2196F3" }
        dialogView.findViewById<View>(R.id.colorGreen).setOnClickListener { selectedColor = "#4CAF50" }
        dialogView.findViewById<View>(R.id.colorOrange).setOnClickListener { selectedColor = "#FF9800" }
        dialogView.findViewById<View>(R.id.colorPurple).setOnClickListener { selectedColor = "#9C27B0" }

        AlertDialog.Builder(this)
            .setTitle(if (itemToEdit == null) "Add Category" else "Edit Category")
            .setView(dialogView)
            .setPositiveButton(if (itemToEdit == null) "Add" else "Update") { _, _ ->
                val name = etName.text.toString().trim()
                val budget = etBudget.text.toString().toDoubleOrNull() ?: 0.0
                if (name.isNotEmpty()) {
                    saveCategoryToFirebase(name, selectedColor, budget, itemToEdit?.id)
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun saveCategoryToFirebase(name: String, color: String, budget: Double, existingId: String? = null) {
        val userId = auth.currentUser?.uid ?: return
        val catRef = if (existingId == null) {
            database.getReference("users").child(userId).child("categories").push()
        } else {
            database.getReference("users").child(userId).child("categories").child(existingId)
        }
        
        val type = if (isExpensesTabSelected) "Expense" else "Income"
        val category = com.example.budget_app.model.Category(name, color, android.R.drawable.ic_menu_gallery, type, budget)
        
        catRef.setValue(category).addOnCompleteListener { task ->
            if (task.isSuccessful) {
                Toast.makeText(this, if (existingId == null) "Category Added" else "Category Updated", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun deleteCategory(id: String) {
        val userId = auth.currentUser?.uid ?: return
        AlertDialog.Builder(this)
            .setTitle("Delete Category")
            .setMessage("Are you sure you want to delete this category?")
            .setPositiveButton("Delete") { _, _ ->
                database.getReference("users").child(userId).child("categories").child(id).removeValue()
                    .addOnCompleteListener { task ->
                        if (task.isSuccessful) {
                            Toast.makeText(this, "Category Deleted", Toast.LENGTH_SHORT).show()
                        }
                    }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    data class CategoryItem(val id: String, val name: String, val colorCode: String, val iconRes: Int, val budget: Double)

    inner class CategoryAdapter(private val items: List<CategoryItem>) :
        RecyclerView.Adapter<CategoryAdapter.ViewHolder>() {

        inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val tvName: TextView = view.findViewById(R.id.tvCategoryName)
            val tvSubtitle: TextView = view.findViewById(R.id.tvCategorySubtitle)
            val ivIcon: ImageView = view.findViewById(R.id.ivCategoryIcon)
            val cvIconBackground: CardView = view.findViewById(R.id.cvIconBackground)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.category_list_item, parent, false)
            return ViewHolder(view)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val item = items[position]
            holder.tvName.text = item.name
            holder.ivIcon.setImageResource(item.iconRes)
            
            val spent = categorySpending.getOrDefault(item.name, 0.0)
            if (item.budget > 0) {
                val remaining = item.budget - spent
                holder.tvSubtitle.text = "Spent: R${String.format("%.2f", spent)} / Budget: R${String.format("%.2f", item.budget)}"
                if (remaining < 0) {
                    holder.tvSubtitle.setTextColor(Color.RED)
                } else {
                    holder.tvSubtitle.setTextColor(Color.parseColor("#757575"))
                }
            } else {
                holder.tvSubtitle.text = "Spent: R${String.format("%.2f", spent)}"
                holder.tvSubtitle.setTextColor(Color.parseColor("#757575"))
            }

            try {
                holder.cvIconBackground.setCardBackgroundColor(Color.parseColor(item.colorCode))
            } catch (e: Exception) {
                holder.cvIconBackground.setCardBackgroundColor(Color.GRAY)
            }

            holder.itemView.setOnClickListener {
                showAddCategoryDialog(item)
            }

            holder.itemView.setOnLongClickListener {
                deleteCategory(item.id)
                true
            }
        }

        override fun getItemCount() = items.size
    }

    override fun onBackPressed() {
        if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
            drawerLayout.closeDrawer(GravityCompat.START)
        } else {
            super.onBackPressed()
        }
    }
}