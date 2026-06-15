package com.example.budget_app

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.google.android.material.bottomsheet.BottomSheetDialogFragment

class MenuPopupFragment : BottomSheetDialogFragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.layout_popup_menu, container, false)

        // SAFE ROUTING: Intent handlers with context validation and clearing flags
        view.findViewById<View>(R.id.menuAnalytics).setOnClickListener {
            safeStartActivity(ReportsActivity::class.java)
        }

        view.findViewById<View>(R.id.menuCategories).setOnClickListener {
            safeStartActivity(Category::class.java)
        }

        view.findViewById<View>(R.id.menuBudget).setOnClickListener {
            safeStartActivity(BudgetOverviewActivity::class.java)
        }

        view.findViewById<View>(R.id.menuHistory).setOnClickListener {
            safeStartActivity(TransactionHistoryActivity::class.java)
        }

        view.findViewById<View>(R.id.btnClose).setOnClickListener {
            dismiss()
        }

        return view
    }

    private fun safeStartActivity(target: Class<*>) {
        try {
            val intent = Intent(requireContext(), target)
            intent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
            startActivity(intent)
            dismiss()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    
    override fun getTheme(): Int = R.style.CustomBottomSheetDialog
}