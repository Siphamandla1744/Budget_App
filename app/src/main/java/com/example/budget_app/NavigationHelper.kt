package com.example.budget_app

import android.content.Context
import android.content.Intent
import android.view.View
import androidx.appcompat.app.AppCompatActivity

object NavigationHelper {

    fun setupBottomNavigation(activity: AppCompatActivity, currentId: Int) {
        val well1 = activity.findViewById<View>(R.id.nav_well_1)
        val well2 = activity.findViewById<View>(R.id.nav_well_2)
        val well3 = activity.findViewById<View>(R.id.nav_well_3)
        val well4 = activity.findViewById<View>(R.id.nav_well_4)

        val wells = listOf(well1, well2, well3, well4)
        
        // Reset selections
        wells.forEach { it?.isSelected = false }
        
        // Set current selection based on activity type or index
        when (activity) {
            is MainActivity -> well1?.isSelected = true
            is ReportsActivity -> well2?.isSelected = true // Although we show popup, well2 could be active
            is Createbudget -> well3?.isSelected = true
            is GamificationActivity -> well4?.isSelected = true
        }

        well1?.setOnClickListener {
            if (activity !is MainActivity) {
                activity.startActivity(Intent(activity, MainActivity::class.java))
                activity.finish()
            }
        }

        well2?.setOnClickListener {
            // INTERCEPT: Show stylized popup menu instead of standard navigation
            val menuPopup = MenuPopupFragment()
            menuPopup.show(activity.supportFragmentManager, "menu_popup")
        }

        well3?.setOnClickListener {
            if (activity !is Createbudget) {
                activity.startActivity(Intent(activity, Createbudget::class.java))
            }
        }

        well4?.setOnClickListener {
            if (activity !is GamificationActivity) {
                activity.startActivity(Intent(activity, GamificationActivity::class.java))
            }
        }
    }
}