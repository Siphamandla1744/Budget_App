package com.example.budget_app

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ImageView
import android.widget.ProgressBar
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
import com.example.budget_app.model.transactions
import com.github.mikephil.charting.charts.PieChart
import com.github.mikephil.charting.data.PieData
import com.github.mikephil.charting.data.PieDataSet
import com.github.mikephil.charting.data.PieEntry
import com.github.mikephil.charting.utils.ColorTemplate
import com.google.android.material.navigation.NavigationView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import java.text.SimpleDateFormat
import java.util.*

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
    private lateinit var chipGroupFilter: com.google.android.material.chip.ChipGroup

    private var isExpensesTabSelected = true
    private var currentFilter = "Month"
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
        rvCategories.layoutManager = androidx.recyclerview.widget.LinearLayoutManager(this)
        tabExpenses = findViewById(R.id.tabExpenses)
        tabIncome = findViewById(R.id.tabIncome)
        tvExpensesTab = findViewById(R.id.tvExpensesTab)
        tvIncomeTab = findViewById(R.id.tvIncomeTab)
        indicatorExpenses = findViewById(R.id.indicatorExpenses)
        indicatorIncome = findViewById(R.id.indicatorIncome)
        chipGroupFilter = findViewById(R.id.chipGroupFilter)
        val ivMenu = findViewById<ImageView>(R.id.ivMenu)
        val fabAddCategory = findViewById<ImageView>(R.id.fab_add_category)
        val tvMergeCategories = findViewById<TextView>(R.id.tvMergeCategories)

        ivMenu.setOnClickListener {
            drawerLayout.openDrawer(GravityCompat.START)
        }

        fabAddCategory.setOnClickListener {
            if (isExpensesTabSelected) {
                showAddCategoryDialog()
            } else {
                // If in Income tab, show a generic Income logging dialog as requested
                showQuickIncomeLogDialog()
            }
        }

        tvMergeCategories?.setOnClickListener {
            showMergeCategoriesDialog()
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
        setupFilters()
        setupBottomNavigation()
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

    private fun setupFilters() {
        chipGroupFilter.setOnCheckedChangeListener { group, checkedId ->
            currentFilter = when (checkedId) {
                R.id.chipDay -> "Day"
                R.id.chipWeek -> "Week"
                else -> "Month"
            }
            fetchSpendingAndCategories()
        }
    }

    private fun updateTabUI() {
        val activeColor = Color.parseColor("#00E5FF") // neon_blue
        val inactiveColor = Color.parseColor("#FFFFFF") // white

        if (isExpensesTabSelected) {
            tvExpensesTab.setTextColor(activeColor)
            tvExpensesTab.alpha = 1.0f
            indicatorExpenses.visibility = View.VISIBLE
            tvIncomeTab.setTextColor(inactiveColor)
            tvIncomeTab.alpha = 0.5f
            indicatorIncome.visibility = View.INVISIBLE
        } else {
            tvExpensesTab.setTextColor(inactiveColor)
            tvExpensesTab.alpha = 0.5f
            indicatorExpenses.visibility = View.INVISIBLE
            tvIncomeTab.setTextColor(activeColor)
            tvIncomeTab.alpha = 1.0f
            indicatorIncome.visibility = View.VISIBLE
        }
    }

    private fun fetchSpendingAndCategories() {
        val userId = auth.currentUser?.uid ?: return
        val transRef = database.getReference("users").child(userId).child("transactions")
        val catRef = database.getReference("users").child(userId).child("categories")

        // DECUPLED ENGINE: Listeners operate independently to ensure UI refresh on any change
        transRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                categorySpending.clear()
                val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                val now = Calendar.getInstance()

                for (data in snapshot.children) {
                    val trans = data.getValue(transactions::class.java)
                    if (trans != null) {
                        try {
                            val transDate = sdf.parse(trans.transaction_date)
                            val cal = Calendar.getInstance()
                            if (transDate != null) cal.time = transDate

                            val isInFilter = when (currentFilter) {
                                "Day" -> now.get(Calendar.DAY_OF_YEAR) == cal.get(Calendar.DAY_OF_YEAR) && now.get(Calendar.YEAR) == cal.get(Calendar.YEAR)
                                "Week" -> now.get(Calendar.WEEK_OF_YEAR) == cal.get(Calendar.WEEK_OF_YEAR) && now.get(Calendar.YEAR) == cal.get(Calendar.YEAR)
                                else -> now.get(Calendar.MONTH) == cal.get(Calendar.MONTH) && now.get(Calendar.YEAR) == cal.get(Calendar.YEAR)
                            }

                            if (isInFilter) {
                                val catName = trans.category.category_name
                                val current = categorySpending.getOrDefault(catName, 0.0)
                                categorySpending[catName] = current + trans.transaction_amamount
                            }
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }
                }
                updateCategoryList()
                updatePieChart()
            }
            override fun onCancelled(error: DatabaseError) {}
        })

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
                }
                
                // FORCE REFRESH: Re-bind adapter with new data
                updateCategoryList()
                updatePieChart()
            }
            override fun onCancelled(error: DatabaseError) {}
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
            pieChart.setNoDataText("No data available")
            pieChart.setNoDataTextColor(Color.WHITE)
            pieChart.invalidate()
            return
        }

        val dataSet = PieDataSet(entries, "")
        dataSet.colors = colors
        dataSet.setDrawValues(false)

        val data = PieData(dataSet)
        pieChart.data = data
        pieChart.description.isEnabled = false
        
        // Donut configuration
        pieChart.isDrawHoleEnabled = true
        pieChart.holeRadius = 75f
        pieChart.setHoleColor(Color.TRANSPARENT)
        
        val centerTitle = if (isExpensesTabSelected) "Expenses" else "Income"
        var highestCat = ""
        var maxAmount = 0.0
        
        // Filter spending by current tab categories
        val currentCatNames = categories.map { it.name }
        for ((name, spent) in categorySpending) {
            if (name in currentCatNames && spent > maxAmount) {
                maxAmount = spent
                highestCat = name
            }
        }
        
        val centerText = android.text.SpannableString("$centerTitle\n\n${String.format(Locale.getDefault(), "%,.0f\n%s", maxAmount, highestCat)}")
        pieChart.centerText = centerText
        pieChart.setCenterTextSize(18f)
        pieChart.setCenterTextColor(Color.WHITE)
        
        pieChart.animateY(1000)
        pieChart.legend.isEnabled = false
        pieChart.setDrawEntryLabels(false)
        pieChart.invalidate()
    }

    private fun seedDefaultCategories() {
        val userId = auth.currentUser?.uid ?: return
        val catRef = database.getReference("users").child(userId).child("categories")

        // ATOMIC SEED: Only push if the database is truly null/empty to prevent duplicates
        catRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (!snapshot.exists() || snapshot.childrenCount == 0L) {
                    val defaults = listOf(
                        com.example.budget_app.model.Category("Food & Drink", "#FF9800", android.R.drawable.ic_menu_gallery, "Expense"),
                        com.example.budget_app.model.Category("Shopping", "#E91E63", android.R.drawable.ic_menu_gallery, "Expense"),
                        com.example.budget_app.model.Category("Transport", "#FFEB3B", android.R.drawable.ic_menu_gallery, "Expense"),
                        com.example.budget_app.model.Category("Rent", "#2196F3", android.R.drawable.ic_menu_gallery, "Expense"),
                        com.example.budget_app.model.Category("Grocery", "#9C27B0", android.R.drawable.ic_menu_gallery, "Expense"),
                        com.example.budget_app.model.Category("Salary", "#4CAF50", android.R.drawable.ic_menu_gallery, "Income"),
                        com.example.budget_app.model.Category("Gift", "#00E5FF", android.R.drawable.ic_menu_gallery, "Income"),
                        com.example.budget_app.model.Category("Dividends", "#D500F9", android.R.drawable.ic_menu_gallery, "Income")
                    )

                    for (cat in defaults) {
                        catRef.push().setValue(cat)
                    }
                }
            }
            override fun onCancelled(error: DatabaseError) {}
        })
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
                if (existingId == null) {
                    NotificationRepository.logEvent(userId, "Transaction Event", "New category created: $name")
                }
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

    private fun showLogTransactionDialog(item: CategoryItem) {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_add_category, null)
        val etAmount = dialogView.findViewById<EditText>(R.id.etCategoryBudget) // Reusing field for amount
        val etDesc = dialogView.findViewById<EditText>(R.id.etCategoryName) // Reusing field for desc
        
        etAmount.hint = "Amount"
        etDesc.hint = "Description"
        etAmount.setText("")
        etDesc.setText("")

        val type = if (isExpensesTabSelected) "Expense" else "Income"

        AlertDialog.Builder(this)
            .setTitle("Log $type: ${item.name}")
            .setView(dialogView)
            .setPositiveButton("Log") { _, _ ->
                val amount = etAmount.text.toString().toDoubleOrNull() ?: 0.0
                val desc = etDesc.text.toString().trim()
                if (amount > 0 && desc.isNotEmpty()) {
                    val category = com.example.budget_app.model.Category(item.name, item.colorCode, item.iconRes, type, item.budget)
                    CategoryRepository.logExpense(category, amount, desc) { success ->
                        if (success) {
                            Toast.makeText(this, "$type Logged", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showQuickIncomeLogDialog() {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_add_category, null)
        val etAmount = dialogView.findViewById<EditText>(R.id.etCategoryBudget)
        val etDesc = dialogView.findViewById<EditText>(R.id.etCategoryName)
        
        etAmount.hint = "Amount"
        etDesc.hint = "Source (e.g. Salary, Gift)"
        etAmount.setText("")
        etDesc.setText("")

        AlertDialog.Builder(this)
            .setTitle("Quick Income Log")
            .setView(dialogView)
            .setPositiveButton("Log") { _, _ ->
                val amount = etAmount.text.toString().toDoubleOrNull() ?: 0.0
                val desc = etDesc.text.toString().trim()
                if (amount > 0 && desc.isNotEmpty()) {
                    val category = com.example.budget_app.model.Category("General Income", "#4CAF50", android.R.drawable.ic_menu_gallery, "Income")
                    CategoryRepository.logExpense(category, amount, desc) { success ->
                        if (success) {
                            Toast.makeText(this, "Income Logged", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showMergeCategoriesDialog() {
        val categories = if (isExpensesTabSelected) expenseCategories else incomeCategories
        if (categories.size < 2) {
            Toast.makeText(this, "Need at least 2 categories to merge", Toast.LENGTH_SHORT).show()
            return
        }

        val names = categories.map { it.name }.toTypedArray()
        var selectedSourceIdx = 0

        AlertDialog.Builder(this)
            .setTitle("Merge Category From...")
            .setSingleChoiceItems(names, 0) { _, which -> selectedSourceIdx = which }
            .setPositiveButton("Next") { _, _ ->
                val source = categories[selectedSourceIdx]
                val remainingNames = categories.filter { it.id != source.id }.map { it.name }.toTypedArray()
                var selectedTargetIdx = 0
                
                AlertDialog.Builder(this)
                    .setTitle("Merge into...")
                    .setSingleChoiceItems(remainingNames, 0) { _, which -> selectedTargetIdx = which }
                    .setPositiveButton("Merge") { _, _ ->
                        val targetName = remainingNames[selectedTargetIdx]
                        val target = categories.find { it.name == targetName }
                        if (target != null) {
                            CategoryRepository.mergeCategories(source.id, target.id) { success ->
                                if (success) Toast.makeText(this, "Categories Merged", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                    .show()
            }
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
            val pbProgress: ProgressBar = view.findViewById(R.id.pbCategoryProgress)
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
                holder.tvSubtitle.text = "Spent: R${String.format("%.2f", spent)} / Budget: R${String.format("%.2f", item.budget)}"
                
                holder.pbProgress.visibility = View.VISIBLE
                val progress = ((spent / item.budget) * 100).toInt()
                holder.pbProgress.progress = progress.coerceIn(0, 100)
                
                if (spent > item.budget) {
                    holder.tvSubtitle.setTextColor(Color.RED)
                    holder.tvSubtitle.alpha = 1.0f
                } else {
                    holder.tvSubtitle.setTextColor(Color.WHITE)
                    holder.tvSubtitle.alpha = 0.7f
                }
            } else {
                holder.tvSubtitle.text = "Spent: R${String.format("%.2f", spent)}"
                holder.tvSubtitle.setTextColor(Color.WHITE)
                holder.tvSubtitle.alpha = 0.7f
                holder.pbProgress.visibility = View.GONE
            }

            try {
                holder.cvIconBackground.setCardBackgroundColor(Color.parseColor(item.colorCode))
            } catch (e: Exception) {
                holder.cvIconBackground.setCardBackgroundColor(Color.GRAY)
            }

            holder.itemView.setOnClickListener {
                showLogTransactionDialog(item)
            }

            holder.itemView.setOnLongClickListener {
                deleteCategory(item.id)
                true
            }
        }

        override fun getItemCount() = items.size
    }

    private fun setupBottomNavigation() {
        val well1 = findViewById<View>(R.id.nav_well_1)
        val well2 = findViewById<View>(R.id.nav_well_2)
        val well3 = findViewById<View>(R.id.nav_well_3)
        val well4 = findViewById<View>(R.id.nav_well_4)
        
        well2.isSelected = true

        well1.setOnClickListener { startActivity(Intent(this, MainActivity::class.java)) }
        well2.setOnClickListener { /* Already here */ }
        well3.setOnClickListener { startActivity(Intent(this, Createbudget::class.java)) }
        well4.setOnClickListener { startActivity(Intent(this, GamificationActivity::class.java)) }
    }

    override fun onBackPressed() {
        if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
            drawerLayout.closeDrawer(GravityCompat.START)
        } else {
            super.onBackPressed()
        }
    }
}