package `in`.app.connect.bottomnav.userprofile

import android.content.Context
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.ImageView
import android.widget.ListAdapter
import android.widget.ListView
import android.widget.SearchView
import android.widget.TextView
import androidx.appcompat.app.ActionBar
import androidx.core.content.ContextCompat.startActivity
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import `in`.app.connect.PopupViewProfile
import `in`.app.connect.R
import `in`.app.connect.usermanagment.models.UserData

class PendingConnectionsActivity : AppCompatActivity() {
    private lateinit var listView: ListView
    private lateinit var searchView: SearchView
    private val pendingConnectionsList = mutableListOf<PendingConnection>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_pending_connections)

        listView = findViewById(R.id.connectionListView)
        searchView = findViewById(R.id.searchView)

        // Fetch and display pending connections
        val phoneNumber = intent.getStringExtra("phoneNumber")
        fetchPendingConnections(phoneNumber!!)
        val adapter = PendingConnectionAdapter(
            this@PendingConnectionsActivity,
            pendingConnectionsList
        )
        listView.adapter = adapter
        // Set up the SearchView
        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                return false
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                filterPendingConnections(newText)
                return true
            }
        })
        val toolbar: androidx.appcompat.widget.Toolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)

        val actionBar: ActionBar? = supportActionBar
        actionBar?.setDisplayHomeAsUpEnabled(true)
        actionBar?.setDisplayShowHomeEnabled(true)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> {
                // Handle the home/up arrow click here, for example, emulate a back press
                onBackPressed()
                return true
            }
        }
        return super.onOptionsItemSelected(item)
    }

    private fun filterPendingConnections(query: String?) {
        val filteredList = mutableListOf<PendingConnection>()
        if (!query.isNullOrBlank()) {
            for (pendingConnection in pendingConnectionsList) {
                val userData = pendingConnection.userData
                if (userData != null && userData.userName.contains(query, ignoreCase = true)) {
                    filteredList.add(pendingConnection)
                }
            }
        } else {
            filteredList.addAll(pendingConnectionsList)
        }
        val adapter = listView.adapter as PendingConnectionAdapter
        adapter.updateList(filteredList)
    }

    private fun fetchPendingConnections(userPhoneNumber: String) {
        val connectionsRef: DatabaseReference =
            FirebaseDatabase.getInstance().reference.child("Connections")

        connectionsRef.orderByChild("receiver").equalTo(userPhoneNumber)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(dataSnapshot: DataSnapshot) {
                    for (snapshot in dataSnapshot.children) {
                        val status = snapshot.child("status").value.toString()
                        if (status == "requested") {
                            val senderPhoneNumber = snapshot.child("sender").value.toString()
                            val pendingConnection =
                                PendingConnection(senderPhoneNumber, status, null)
                            pendingConnectionsList.add(pendingConnection)
                            fetchUserDetails(senderPhoneNumber, pendingConnection)
                        }
                    }
                }

                override fun onCancelled(databaseError: DatabaseError) {
                    // Handle database error
                }
            })
    }

    private fun fetchUserDetails(phoneNumber: String, pendingConnection: PendingConnection) {
        val userRef: DatabaseReference =
            FirebaseDatabase.getInstance().reference.child("Users").child(phoneNumber)

        userRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                if (dataSnapshot.exists()) {
                    val userData = dataSnapshot.getValue(UserData::class.java)
                    println(userData)
                    userData?.let {
                        // Update the pendingConnection with user data
                        pendingConnection.userData = userData
                    }
                }

                // Notify the adapter after all user details are fetched
                val adapter = listView.adapter as PendingConnectionAdapter
                adapter.notifyDataSetChanged()
            }

            override fun onCancelled(databaseError: DatabaseError) {
                // Handle database error
            }
        })
    }


    data class PendingConnection(
        val phoneNumber: String,  // The phone number of the user requesting the connection
        val status: String,       // The status of the connection (e.g., "requested", "connected")
        var userData: UserData?   // User data for the pending connection (optional)
    )

    class PendingConnectionAdapter(
        private val context: Context,
        private var pendingConnections: List<PendingConnection>
    ) : BaseAdapter(), ListAdapter {

        override fun getCount(): Int {
            return pendingConnections.size
        }

        override fun getItem(position: Int): PendingConnection {
            return pendingConnections[position]
        }

        override fun getItemId(position: Int): Long {
            return position.toLong()
        }

        override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
            val holder: ViewHolder
            var rowView = convertView

            if (rowView == null) {
                val inflater =
                    context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
                rowView = inflater.inflate(R.layout.pending_connection_item, parent, false)

                holder = ViewHolder()
                holder.profileImageView = rowView.findViewById(R.id.profileImageView)
                holder.nameTextView = rowView.findViewById(R.id.nameTextView)

                rowView.tag = holder
            } else {
                holder = rowView.tag as ViewHolder
            }

            val pendingConnection = getItem(position)
            println(pendingConnection)
            val userData = pendingConnection.userData
            if (userData != null) {
                holder.nameTextView.text = userData.userName
                loadImageWithGlide(holder.profileImageView, userData!!.images[0])

            } else {
                holder.nameTextView.text = "Loading..."
            }
            rowView!!.setOnClickListener {
                val intent = Intent(context, PopupViewProfile::class.java)
                intent.putExtra(
                    "phoneNumber",
                    pendingConnection.phoneNumber
                )
                context.startActivity(intent)
            }
            return rowView!!
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

        private class ViewHolder {
            lateinit var profileImageView: ImageView
            lateinit var nameTextView: TextView
        }

        fun updateList(newList: List<PendingConnection>) {
            pendingConnections = newList
            notifyDataSetChanged()
        }
    }

}

