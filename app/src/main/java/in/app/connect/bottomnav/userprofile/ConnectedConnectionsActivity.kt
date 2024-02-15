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
import android.widget.Toolbar
import androidx.appcompat.app.ActionBar
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

class ConnectedConnectionsActivity : AppCompatActivity() {
    private lateinit var listView: ListView
    private lateinit var searchView: SearchView
    private val connectedConnectionsList = mutableListOf<ConnectedConnection>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_connected_connections)
        listView = findViewById(R.id.connectionListView)
        searchView = findViewById(R.id.searchView)

        // Fetch and display pending connections
        val phoneNumber = intent.getStringExtra("phoneNumber")
        fetchConnectedConnections(phoneNumber!!)
        val adapter = ConnectedConnectionAdapter(
            this@ConnectedConnectionsActivity,
            connectedConnectionsList
        )
        listView.adapter = adapter
        // Set up the SearchView
        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                return false
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                filterConnectedConnections(newText)
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

    private fun filterConnectedConnections(query: String?) {
        val filteredList = mutableListOf<ConnectedConnection>()
        if (!query.isNullOrBlank()) {
            for (connectedConnection in connectedConnectionsList) {
                val userData = connectedConnection.userData
                if (userData != null && userData.userName.contains(query, ignoreCase = true)) {
                    filteredList.add(connectedConnection)
                }
            }
        } else {
            filteredList.addAll(connectedConnectionsList)
        }
        val adapter = listView.adapter as ConnectedConnectionAdapter
        adapter.updateList(filteredList)
    }

    private fun fetchConnectedConnections(userPhoneNumber: String) {
        val connectionsRef: DatabaseReference =
            FirebaseDatabase.getInstance().reference.child("Connections")
        connectionsRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                for (snapshot in dataSnapshot.children) {
                    val sender = snapshot.child("sender").value.toString()
                    val receiver = snapshot.child("receiver").value.toString()
                    val status = snapshot.child("status").value.toString()
                    if ((sender == userPhoneNumber || receiver == userPhoneNumber) && status == "connected") {
                        if (sender == userPhoneNumber) {
                            val phoneNumber = snapshot.child("receiver").value.toString()
                            val pendingConnection =
                                ConnectedConnection(phoneNumber, status, null)
                            connectedConnectionsList.add(pendingConnection)
                            fetchUserDetails(phoneNumber, pendingConnection)
                        } else {
                            val phoneNumber = snapshot.child("sender").value.toString()
                            val pendingConnection =
                                ConnectedConnection(phoneNumber, status, null)
                            connectedConnectionsList.add(pendingConnection)
                            fetchUserDetails(phoneNumber, pendingConnection)
                        }
                    }
                }
            }

            override fun onCancelled(databaseError: DatabaseError) {
                println("Error fetching connected connection count: ${databaseError.message}")
            }
        })
    }

    private fun fetchUserDetails(phoneNumber: String, pendingConnection: ConnectedConnection) {
        val userRef: DatabaseReference =
            FirebaseDatabase.getInstance().reference.child("Users").child(phoneNumber)

        userRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                if (dataSnapshot.exists()) {
                    val userData = dataSnapshot.getValue(UserData::class.java)
                    userData?.let {
                        // Update the pendingConnection with user data
                        pendingConnection.userData = userData
                    }
                }

                // Notify the adapter after all user details are fetched
                val adapter = listView.adapter as ConnectedConnectionAdapter
                adapter.notifyDataSetChanged()
            }

            override fun onCancelled(databaseError: DatabaseError) {
                // Handle database error
            }
        })
    }


    data class ConnectedConnection(
        val phoneNumber: String,  // The phone number of the user requesting the connection
        val status: String,       // The status of the connection (e.g., "requested", "connected")
        var userData: UserData?   // User data for the pending connection (optional)
    )

    class ConnectedConnectionAdapter(
        private val context: Context,
        private var connectedConnections: List<ConnectedConnection>
    ) : BaseAdapter(), ListAdapter {

        override fun getCount(): Int {
            return connectedConnections.size
        }

        override fun getItem(position: Int): ConnectedConnection {
            return connectedConnections[position]
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
                rowView = inflater.inflate(R.layout.connected_connection_layout, parent, false)

                holder = ViewHolder()
                holder.profileImageView = rowView.findViewById(R.id.profileImageView)
                holder.nameTextView = rowView.findViewById(R.id.nameTextView)

                rowView.tag = holder
            } else {
                holder = rowView.tag as ViewHolder
            }

            val pendingConnection = getItem(position)
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

        fun updateList(newList: List<ConnectedConnection>) {
            connectedConnections = newList
            notifyDataSetChanged()
        }
    }

}
