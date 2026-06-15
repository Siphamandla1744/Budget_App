package com.example.budget_app

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*

class NotificationsActivity : AppCompatActivity() {
    private lateinit var auth: FirebaseAuth
    private lateinit var database: FirebaseDatabase
    private lateinit var ivTopProfile: ImageView
    private lateinit var rvNotifications: RecyclerView
    private val notificationList = mutableListOf<NotificationItem>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_notifications)

        auth = FirebaseAuth.getInstance()
        database = FirebaseDatabase.getInstance()
        
        ivTopProfile = findViewById(R.id.ivTopProfile)
        rvNotifications = findViewById(R.id.rvNotifications)
        rvNotifications.layoutManager = LinearLayoutManager(this)
        
        setupTopBar()
        setupBottomNavigation()
        loadUserProfile()
        loadNotifications()
    }

    private fun setupTopBar() {
        findViewById<ImageView>(R.id.btnTopNotifications).setOnClickListener {
            // Already here
        }
        findViewById<CardView>(R.id.btnTopProfile).setOnClickListener {
            startActivity(Intent(this, SettingsActivity::class.java))
        }
        findViewById<TextView>(R.id.tvTopTitle).text = "Notifications"
    }

    private fun loadUserProfile() {
        val userId = auth.currentUser?.uid ?: return
        database.getReference("users").child(userId).child("profile")
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val imageUrl = snapshot.child("profileImageUrl").getValue(String::class.java)
                    if (!imageUrl.isNullOrEmpty()) {
                        com.bumptech.glide.Glide.with(this@NotificationsActivity).load(imageUrl).into(ivTopProfile)
                    }
                }
                override fun onCancelled(error: DatabaseError) {}
            })
    }

    private fun loadNotifications() {
        val userId = auth.currentUser?.uid ?: return
        database.getReference("users").child(userId).child("notifications")
            .orderByChild("timestamp")
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    notificationList.clear()
                    for (data in snapshot.children) {
                        val id = data.child("id").getValue(String::class.java) ?: ""
                        val type = data.child("type").getValue(String::class.java) ?: "System"
                        val message = data.child("message").getValue(String::class.java) ?: ""
                        val timestamp = data.child("timestamp").getValue(String::class.java) ?: ""
                        notificationList.add(0, NotificationItem(id, type, message, timestamp))
                    }
                    rvNotifications.adapter = NotificationAdapter(notificationList)
                }
                override fun onCancelled(error: DatabaseError) {}
            })
    }

    private fun setupBottomNavigation() {
        NavigationHelper.setupBottomNavigation(this, 0)
    }

    data class NotificationItem(val id: String, val type: String, val message: String, val timestamp: String)

    inner class NotificationAdapter(private val items: List<NotificationItem>) :
        RecyclerView.Adapter<NotificationAdapter.ViewHolder>() {

        inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val tvType: TextView = view.findViewById(R.id.tvNotificationType)
            val tvMessage: TextView = view.findViewById(R.id.tvNotificationMessage)
            val tvTime: TextView = view.findViewById(R.id.tvNotificationTime)
            val ivIcon: ImageView = view.findViewById(R.id.ivNotificationIcon)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.notification_list_item, parent, false)
            return ViewHolder(view)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val item = items[position]
            holder.tvType.text = item.type
            holder.tvMessage.text = item.message
            holder.tvTime.text = item.timestamp

            when (item.type) {
                "System Event" -> holder.ivIcon.setImageResource(android.R.drawable.ic_dialog_info)
                "Transaction Event" -> holder.ivIcon.setImageResource(android.R.drawable.ic_menu_save)
                "Gamification Milestone" -> holder.ivIcon.setImageResource(android.R.drawable.btn_star_big_on)
                else -> holder.ivIcon.setImageResource(android.R.drawable.ic_popup_reminder)
            }
        }

        override fun getItemCount() = items.size
    }
}
