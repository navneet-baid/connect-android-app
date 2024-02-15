package `in`.app.connect

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.BaseAdapter
import android.widget.ImageView
import android.widget.ListView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.google.firebase.database.ChildEventListener
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import `in`.app.connect.utils.ConnectAppApplication
import `in`.app.connect.utils.SessionManager
import java.util.HashMap


class NotificationActivity : AppCompatActivity() {
    private lateinit var listView: ListView
    private lateinit var swipeRefreshLayout: SwipeRefreshLayout
    private lateinit var notificationAdapter: ArrayAdapter<String>
    private lateinit var notificationsRef: DatabaseReference
    private lateinit var userRef: DatabaseReference
    private val notificationsList = ArrayList<Notification>() // Modify the data structure as needed
    lateinit var sessionManager: SessionManager
    private var userDetails: HashMap<String, Any> = HashMap()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.fragment_notification)
        (application as ConnectAppApplication).startAppUsageTracking()

        val toolbar = findViewById<androidx.appcompat.widget.Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)

        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Notifications"

        listView = findViewById(R.id.notificationListView)
        swipeRefreshLayout = findViewById(R.id.swipeRefreshLayout)


        sessionManager = SessionManager(this@NotificationActivity)
        userDetails = sessionManager.getUserDetailFromSession()
        // Initialize Firebase database reference to the notifications node
        notificationsRef = FirebaseDatabase.getInstance().reference.child("notifications")
            .child(userDetails[sessionManager.KEY_PHONENUMBER].toString())
        userRef = FirebaseDatabase.getInstance().reference.child("Users")

        val notificationAdapter = NotificationAdapter(
            this@NotificationActivity,
            notificationsList,
            userDetails,
            sessionManager
        )
        listView.adapter = notificationAdapter
        // Set an
        // to fetch and update notifications
        notificationsRef.addChildEventListener(object : ChildEventListener {
            override fun onChildAdded(snapshot: DataSnapshot, previousChildName: String?) {
                val id = snapshot.key // The unique push key of the notification
                val title = snapshot.child("title").value.toString()
                val body = snapshot.child("body").value.toString()
                val senderPhoneNumber = snapshot.child("senderPhoneNumber").value.toString()
                val fullName = snapshot.child("userName").value.toString()
                val date = snapshot.child("date").value as Long
                val isUnread = snapshot.child("isUnread").value as Boolean
                userRef.child(senderPhoneNumber).child("images/0")
                    .addListenerForSingleValueEvent(object : ValueEventListener {
                        override fun onDataChange(snapshot: DataSnapshot) {
                            if (snapshot.exists()) {
                                val imageUrl = snapshot.getValue(String::class.java)
                                // Check if a notification with the same title, id, and phoneNumber already exists in the list
                                val existingNotification = notificationsList.find {
                                    it.title == title && it.body == body && it.phoneNumber == senderPhoneNumber
                                }
                                if (existingNotification != null) {
                                    // If an existing notification is found, update it with the latest date
                                    if (existingNotification.date < date) {
                                        existingNotification.date = date
                                        existingNotification.isUnread = isUnread
                                        existingNotification.imageUrl = imageUrl
                                    }
                                } else {
                                    // If no existing notification is found, create a new one
                                    val notification = Notification(
                                        id,
                                        title,
                                        body,
                                        senderPhoneNumber,
                                        fullName,
                                        date,
                                        isUnread,
                                        imageUrl
                                    )
                                    // Add the notification to the list
                                    notificationsList.add(notification)
                                }

                                // Sort the notifications list by date in descending order
                                notificationsList.sortByDescending { it.date }

                                // Notify the adapter that data has changed
                                notificationAdapter.notifyDataSetChanged()

                            } else {
                                // Handle the case where the data doesn't exist
                            }
                        }

                        override fun onCancelled(databaseError: DatabaseError) {
                            // Handle any errors that might occur during the fetch
                        }
                    })
            }

            override fun onChildChanged(snapshot: DataSnapshot, previousChildName: String?) {
                // Handle changes to existing notifications here
                val id = snapshot.key
                val isUnread = snapshot.child("isUnread").value as Boolean

                // Find the corresponding notification in the list
                val notification = notificationsList.find { it.id == id }

                // Update the isUnread status for the notification
                notification?.isUnread = isUnread

                // Notify the adapter that data has changed
                notificationAdapter.notifyDataSetChanged()
            }

            override fun onChildRemoved(snapshot: DataSnapshot) {
                // Handle notification removal here
                val id = snapshot.key

                // Find the corresponding notification in the list and remove it
                val removedNotification = notificationsList.find { it.id == id }
                removedNotification?.let { notificationsList.remove(it) }

                // Notify the adapter that data has changed
                notificationAdapter.notifyDataSetChanged()
            }

            override fun onChildMoved(snapshot: DataSnapshot, previousChildName: String?) {
                // Handle the movement of notifications (if needed)
            }

            override fun onCancelled(databaseError: DatabaseError) {
                // Handle any errors that might occur during the operation
            }
        })


        swipeRefreshLayout.setOnRefreshListener {
            // Clear the existing notifications list
            notificationsList.clear()

            // Notify the adapter that the data has changed (this will clear the list in the UI)
            notificationAdapter.notifyDataSetChanged()

            // Fetch notifications again from Firebase
            notificationsRef.addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    // Process the retrieved data and add it to the notificationsList
                    for (childSnapshot in snapshot.children) {
                        val id = childSnapshot.key
                        val title = childSnapshot.child("title").value.toString()
                        val body = childSnapshot.child("body").value.toString()
                        val senderPhoneNumber =
                            childSnapshot.child("senderPhoneNumber").value.toString()
                        val fullName = childSnapshot.child("userName").value.toString()
                        val date = childSnapshot.child("date").value as Long
                        val isUnread = childSnapshot.child("isUnread").value as Boolean
                        userRef.child(senderPhoneNumber).child("images/0")
                            .addListenerForSingleValueEvent(object : ValueEventListener {
                                override fun onDataChange(snapshot: DataSnapshot) {
                                    if (snapshot.exists()) {
                                        val imageUrl = snapshot.getValue(String::class.java)
                                        // Check if a notification with the same title, id, and phoneNumber already exists in the list
                                        val existingNotification = notificationsList.find {
                                            it.title == title && it.body == body && it.phoneNumber == senderPhoneNumber
                                        }
                                        if (existingNotification != null) {
                                            // If an existing notification is found, update it with the latest date
                                            if (existingNotification.date < date) {
                                                existingNotification.date = date
                                                existingNotification.isUnread = isUnread
                                                existingNotification.imageUrl = imageUrl
                                            }
                                        } else {
                                            // If no existing notification is found, create a new one
                                            val notification = Notification(
                                                id,
                                                title,
                                                body,
                                                senderPhoneNumber,
                                                fullName,
                                                date,
                                                isUnread,
                                                imageUrl
                                            )
                                            // Add the notification to the list
                                            notificationsList.add(notification)
                                        }

                                        // Sort the notifications list by date in descending order
                                        notificationsList.sortByDescending { it.date }

                                        // Notify the adapter that data has changed
                                        notificationAdapter.notifyDataSetChanged()
                                        notificationAdapter.notifyDataSetChanged()
                                    } else {
                                        // Handle the case where the data doesn't exist
                                    }
                                }

                                override fun onCancelled(databaseError: DatabaseError) {
                                    // Handle any errors that might occur during the fetch
                                }
                            })


                    }

                    // Notify the adapter that the data has changed (this will update the UI)
                    notificationAdapter.notifyDataSetChanged()

                    // Complete the refresh animation
                    swipeRefreshLayout.isRefreshing = false
                }

                override fun onCancelled(databaseError: DatabaseError) {
                    // Handle errors
                    // Complete the refresh animation
                    swipeRefreshLayout.isRefreshing = false
                }
            })
        }

    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> {
                onBackPressed()
                return true
            }
        }
        return super.onOptionsItemSelected(item)
    }

    data class Notification(
        val id: String?,
        val title: String?,
        val body: String?,
        val phoneNumber: String?,
        val fullName: String?,
        var date: Long,
        var isUnread: Boolean,
        var imageUrl: String?
    )

    class NotificationAdapter(
        private val context: Context,
        private val notifications: List<Notification>,
        private val userDetails: HashMap<String, Any>,
        private val sessionManager: SessionManager
    ) : BaseAdapter() {
        override fun getCount(): Int {
            return notifications.size
        }

        override fun getItem(position: Int): Any {
            return notifications[position]
        }

        override fun getItemId(position: Int): Long {
            return position.toLong()
        }

        override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
            val view: View
            val holder: ViewHolder

            if (convertView == null) {
                view = LayoutInflater.from(context)
                    .inflate(R.layout.notification_list_item, parent, false)
                holder = ViewHolder(view)
                view.tag = holder
            } else {
                view = convertView
                holder = view.tag as ViewHolder
            }

            val notification = notifications[position]

            // Bind data to the views in the list item layout
            holder.titleTextView.text = notification.title
            holder.bodyTextView.text = notification.body

            // Format and set the time/date
            val formattedTime = formatTime(notification.date)
            holder.timeTextView.text = formattedTime

            if (notification.isUnread) {
                holder.readStatus.visibility = View.VISIBLE
            } else {
                holder.readStatus.visibility = View.GONE
            }
            loadImageWithGlide(holder.profileImageView, notification.imageUrl!!)
            view.setOnClickListener {
                // Create an Intent to open the NotificationDetailActivity
                val intent = Intent(context, PopupViewProfile::class.java)
                // You can pass data to the new activity if needed
                intent.putExtra("phoneNumber", notification.phoneNumber)
                if (notification.isUnread) {
                    val notificationsRef =
                        FirebaseDatabase.getInstance().reference.child("notifications")
                            .child(userDetails[sessionManager.KEY_PHONENUMBER].toString())
                            .child(notification.id.toString())
                    notificationsRef.child("isUnread").setValue(false)
                }
                // Start the new activity
                context.startActivity(intent)

            }
            return view
        }

        private class ViewHolder(view: View) {
            val titleTextView: TextView = view.findViewById(R.id.titleTextView)
            val bodyTextView: TextView = view.findViewById(R.id.bodyTextView)
            val timeTextView: TextView = view.findViewById(R.id.timeTextView)
            val readStatus: CardView = view.findViewById(R.id.readStatus)
            val profileImageView: ImageView = view.findViewById(R.id.profileImageView)
        }

        private fun loadImageWithGlide(profilePicture: ImageView, imageUrl: String) {
            try {
                Glide.with(context)
                    .load(imageUrl)
                    .diskCacheStrategy(DiskCacheStrategy.ALL) // Enable caching
                    .into(profilePicture)
            } catch (e: Exception) {
                println(e.message)
            }
        }

        private fun formatTime(timestamp: Long): String {
            val currentTimeMillis = System.currentTimeMillis()
            val timeDiffMillis = currentTimeMillis - timestamp

            val seconds = timeDiffMillis / 1000
            val minutes = seconds / 60
            val hours = minutes / 60
            val days = hours / 24
            val weeks = days / 7
            val months = days / 30
            val years = months / 12

            return when {
                years > 0 -> "$years y"
                months > 0 -> "$months mn"
                weeks > 0 -> "$weeks w"
                days > 0 -> "$days d"
                hours > 0 -> "$hours h"
                minutes > 0 -> "$minutes m"
                else -> "${kotlin.math.abs(seconds)} s"
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        (application as ConnectAppApplication).stopAppUsageTracking()
    }
}
