package `in`.app.connect

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.StrictMode
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.android.volley.Response
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.Volley
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.facebook.shimmer.ShimmerFrameLayout
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ServerValue
import com.google.firebase.database.ValueEventListener
import `in`.app.connect.bottomnav.userprofile.BlogPost
import `in`.app.connect.bottomnav.userprofile.BlogPostAdapter
import `in`.app.connect.bottomnav.userprofile.BottomUserProfileFragment
import `in`.app.connect.bottomnav.userprofile.ConnectedConnectionsActivity
import `in`.app.connect.bottomnav.userprofile.PicturePopUpActivity
import `in`.app.connect.usermanagment.models.UserData
import `in`.app.connect.utils.ConnectAppApplication
import `in`.app.connect.utils.SessionManager
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.HashMap
import java.util.concurrent.TimeUnit

class PopupViewProfile : AppCompatActivity() {
    private lateinit var profilePicture: ImageView
    private lateinit var profileName: TextView
    private lateinit var userAge: TextView
    private lateinit var userGender: TextView
    private lateinit var location: TextView
    private lateinit var hometown: TextView
    private lateinit var userBio: TextView
    private lateinit var userBlogRecycler: RecyclerView
    private lateinit var progressBar: ProgressBar
    private lateinit var noOfPosts: TextView
    private lateinit var connectButton: Button
    private lateinit var noPostLayout: LinearLayout
    private lateinit var connectedConnectionsLayout: LinearLayout
    private lateinit var connectedConnectionsCount: TextView
    private lateinit var profileShimmerLayout: ShimmerFrameLayout
    private lateinit var adapter: BlogPostAdapter
    private lateinit var backButton: ImageView
    private lateinit var database: DatabaseReference
    var phoneNumber = ""

    var userData = UserData()
    @SuppressLint("MissingInflatedId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        (application as ConnectAppApplication).startAppUsageTracking()

        setContentView(R.layout.activity_popup_view_profile)
        sessionManager = SessionManager(this@PopupViewProfile)
        userDetails = sessionManager.getUserDetailFromSession()
        profilePicture = findViewById(R.id.profileImage)
        profileName = findViewById(R.id.profileName)
        userAge = findViewById(R.id.userAge)
        userGender = findViewById(R.id.userGender)
        location = findViewById(R.id.location)
        hometown = findViewById(R.id.hometown)
        userBio = findViewById(R.id.userBio)
        userBlogRecycler = findViewById(R.id.userBlogRecycler)
        progressBar = findViewById(R.id.progressBar)
        noOfPosts = findViewById(R.id.noOfPosts)
        connectButton = findViewById(R.id.connect_button)
        noPostLayout = findViewById(R.id.noPostLayout)
        connectedConnectionsLayout = findViewById(R.id.connectedConnectionsLayout)
        connectedConnectionsCount = findViewById(R.id.connectedConnectionsCount)
        profileShimmerLayout = findViewById(R.id.profileShimmerLayout)
        backButton = findViewById(R.id.backButton)
        val data = intent.data

        if (data != null) {
            val pathSegments = data.pathSegments
            if (pathSegments.size >= 3) { // Check for index out of bounds
                phoneNumber = pathSegments[2] ?: "" // Safe access
            }
        } else {
            phoneNumber = intent.getStringExtra("phoneNumber")!!
        }
        if (phoneNumber == userDetails[sessionManager.KEY_PHONENUMBER]) {
            connectButton.visibility = View.GONE
        }
        // Create a database reference to the Users->PhoneNumber path
        val database = FirebaseDatabase.getInstance()
        val usersRef: DatabaseReference = database.reference.child("Users").child(phoneNumber)

        // Add a ValueEventListener to fetch the user data
        usersRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                // Check if the data exists at the specified path
                if (dataSnapshot.exists()) {
                    // Data for the specified phone number exists
                    // You can access the user data from dataSnapshot
                    userData = dataSnapshot.getValue(UserData::class.java)!!
                    // Replace 'User' with the actual data class that matches your database structure
                    if (userData != null) {
                        profileName.text = userData.userName
                        userGender.text = userData.gender
                        location.text = userData.location
                        hometown.text = userData.hometown
                        userBio.text = userData.bio
                        loadImageWithGlide(userData.images[0])
                        val sdf = SimpleDateFormat("dd/MM/yyyy")
                        val birthDate: Date = sdf.parse(userData.dob) as Date
                        val currentDate = Date()
                        val diffInMillis: Long = currentDate.time - birthDate.time
                        val ageInDays: Long = TimeUnit.MILLISECONDS.toDays(diffInMillis)
                        val ageInYears: Int = (ageInDays / 365.25).toInt()
                        userAge.text = ageInYears.toString()
                        val layoutManager = LinearLayoutManager(
                            this@PopupViewProfile,
                            LinearLayoutManager.VERTICAL,
                            false
                        )

                        userBlogRecycler.layoutManager = layoutManager
                        adapter =
                            BlogPostAdapter(this@PopupViewProfile, mutableListOf(), phoneNumber)
                        userBlogRecycler.adapter = adapter
                        fetchBlogPosts()
                    } else {
                        // Handle the case where the data couldn't be deserialized
                    }
                } else {
                    // Data for the specified phone number does not exist
                    // Handle this case
                }
            }

            override fun onCancelled(databaseError: DatabaseError) {
                // Handle any errors, such as database read errors
                println("Error fetching user data: ${databaseError.message}")
            }
        })


        val senderPhoneNumber = userDetails[sessionManager.KEY_PHONENUMBER].toString()
        val connectionsRef = FirebaseDatabase.getInstance().reference.child("Connections")

        // Check the status of the connection request when the activity is opened
        checkConnectionStatus(connectionsRef, senderPhoneNumber)
        backButton.setOnClickListener {
            onBackPressed()
        }
        connectButton.setOnClickListener {
            if (userData != null) {
                if (connectButton.text == "Connect") {
                    // Send a new connection request
                    // Ensure that you have the recipient's FCM token
                    val recipientFCMToken =
                        userData.fcmToken // Replace with the recipient's FCM token
                    val fullName = userDetails[sessionManager.KEY_FULLNAME].toString()

                    // Send the connection request notification
                    pushNotification(
                        this@PopupViewProfile,
                        recipientFCMToken,
                        "New Connection Request",
                        "You have received a new connection request from $fullName.",
                        fullName
                    )

                    // Create a unique key for the connection request
                    val connectionId = connectionsRef.push().key

                    // Create a map with the connection request details
                    val connectionRequest = HashMap<String, Any>()
                    connectionRequest["sender"] = senderPhoneNumber
                    connectionRequest["receiver"] = phoneNumber
                    connectionRequest["status"] =
                        "requested" // You can use "requested" to indicate a pending request

                    // Use the connectionId as the key to store the request
                    connectionsRef.child(connectionId!!).setValue(connectionRequest)

                    // Update the button label and behavior
                    connectButton.text = "Requested"
                } else if (connectButton.text == "Requested") {
                    // Cancel the connection request

                    connectionsRef.orderByChild("sender").equalTo(senderPhoneNumber)
                        .addListenerForSingleValueEvent(object : ValueEventListener {
                            override fun onDataChange(dataSnapshot: DataSnapshot) {
                                for (snapshot in dataSnapshot.children) {
                                    val receiver = snapshot.child("receiver").value.toString()
                                    if (receiver == phoneNumber) {
                                        connectionsRef.child(snapshot.key!!).removeValue()
                                        // Update the button label and behavior
                                        connectButton.text = "Connect"
                                        break
                                    }
                                }
                            }

                            override fun onCancelled(databaseError: DatabaseError) {
                                println("Error checking existing connection requests: ${databaseError.message}")
                            }
                        })
                } else if (connectButton.text == "Accept") {
                    // Accept the connection request and establish the connection
                    // Ensure you have the connectionId, which uniquely identifies the request
                    val recipientFCMToken =
                        userData.fcmToken // Replace with the recipient's FCM token
                    val fullName = userDetails[sessionManager.KEY_FULLNAME].toString()

                    // Send the connection request notification
                    pushNotification(
                        this@PopupViewProfile,
                        recipientFCMToken,
                        "Connection Request Accepted",
                        "$fullName accepted your request.",
                        fullName
                    )
                    connectionsRef.orderByChild("receiver").equalTo(senderPhoneNumber)
                        .addListenerForSingleValueEvent(object : ValueEventListener {
                            override fun onDataChange(dataSnapshot: DataSnapshot) {
                                for (snapshot in dataSnapshot.children) {
                                    val receiver = snapshot.child("sender").value.toString()
                                    if (receiver == phoneNumber) {
                                        // Update the connection status to "connected" or your desired status
                                        connectionsRef.child(snapshot.key!!).child("status")
                                            .setValue("connected")

                                        // Update the button label and behavior for both users
                                        connectButton.text = "Disconnect"

                                        break
                                    }
                                }
                            }

                            override fun onCancelled(databaseError: DatabaseError) {
                                println("Error checking existing connection requests: ${databaseError.message}")
                            }
                        })
                } else if (connectButton.text == "Disconnect") {
                    // Disconnect logic
                    // Find and remove the connection node
                    connectionsRef.orderByChild("receiver").equalTo(senderPhoneNumber)
                        .addListenerForSingleValueEvent(object : ValueEventListener {
                            override fun onDataChange(dataSnapshot: DataSnapshot) {
                                for (snapshot in dataSnapshot.children) {
                                    val receiver = snapshot.child("sender").value.toString()
                                    if (receiver == phoneNumber) {
                                        // Remove the connection node
                                        connectionsRef.child(snapshot.key!!).removeValue()
                                        // Update the button label for both users
                                        connectButton.text = "Connect"
                                        break
                                    }
                                }
                            }

                            override fun onCancelled(databaseError: DatabaseError) {
                                println("Error checking existing connection requests: ${databaseError.message}")
                            }
                        })
                }

            }
        }
        fetchConnectedConnectionsCount(
            FirebaseDatabase.getInstance().reference.child("Connections"),
            phoneNumber,
            object : ConnectedConnectionsCountCallback {
                override fun onConnectedConnectionsCount(connectedCount: Int) {
                    connectedConnectionsCount.text = connectedCount.toString()
                    if (connectedCount > 0)
                        connectedConnectionsLayout.setOnClickListener {
                            // Handle when the "Connected Connections" view is clicked
                            val intent =
                                Intent(this@PopupViewProfile, ConnectedConnectionsActivity::class.java)
                            intent.putExtra("phoneNumber", phoneNumber)
                            startActivity(intent)
                        }
                }
            }
        )
    }

    interface ConnectedConnectionsCountCallback {
        fun onConnectedConnectionsCount(count: Int)
    }

    private fun fetchConnectedConnectionsCount(
        connectionsRef: DatabaseReference,
        userPhoneNumber: String,
        callback: ConnectedConnectionsCountCallback
    ) {
        connectionsRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                var count = 0
                for (snapshot in dataSnapshot.children) {
                    val sender = snapshot.child("sender").value.toString()
                    val receiver = snapshot.child("receiver").value.toString()
                    val status = snapshot.child("status").value.toString()
                    if ((sender == userPhoneNumber || receiver == userPhoneNumber) && status == "connected") {
                        count++
                    }
                }
                callback.onConnectedConnectionsCount(count)
            }

            override fun onCancelled(databaseError: DatabaseError) {
                println("Error fetching connected connection count: ${databaseError.message}")
                callback.onConnectedConnectionsCount(0) // Handle errors by indicating no connected connections
            }
        })
    }


    private fun checkConnectionStatus(
        connectionsRef: DatabaseReference,
        senderPhoneNumber: String
    ) {
        // Check if the logged-in user (sender) has sent a connection request to the receiver
        val senderQuery = connectionsRef.orderByChild("sender").equalTo(senderPhoneNumber)
        senderQuery.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                var senderConnectionId: String? = null
                var receiverConnectionId: String? = null

                for (snapshot in dataSnapshot.children) {
                    val receiver = snapshot.child("receiver").value.toString()
                    if (receiver == phoneNumber) {
                        senderConnectionId = snapshot.key
                        break
                    }
                }

                if (senderConnectionId != null) {
                    // Request sent by the sender to the receiver
                    isConnected(senderConnectionId, connectionsRef) { isConnected ->
                        if (isConnected) {
                            connectButton.text = "Disconnect"
                        } else {
                            connectButton.text = "Requested"
                        }
                    }

                } else {
                    // No request from sender to receiver
                    // Check if the receiver has sent a request to the sender
                    val receiverQuery = connectionsRef.orderByChild("sender").equalTo(phoneNumber)
                    receiverQuery.addListenerForSingleValueEvent(object : ValueEventListener {
                        override fun onDataChange(dataSnapshot: DataSnapshot) {
                            for (snapshot in dataSnapshot.children) {
                                val sender = snapshot.child("receiver").value.toString()
                                if (sender == senderPhoneNumber) {
                                    receiverConnectionId = snapshot.key
                                    break
                                }
                            }

                            if (receiverConnectionId != null) {
                                // Request sent by the receiver to the sender
                                isConnected(receiverConnectionId!!, connectionsRef) { isConnected ->
                                    if (isConnected) {
                                        connectButton.text = "Disconnect"
                                    } else {
                                        connectButton.text = "Accept"
                                    }
                                }
                            } else {
                                // No connection requests between sender and receiver
                                connectButton.text = "Connect"
                            }
                        }

                        override fun onCancelled(databaseError: DatabaseError) {
                            println("Error checking receiver's connection requests: ${databaseError.message}")
                        }
                    })
                }
            }

            override fun onCancelled(databaseError: DatabaseError) {
                println("Error checking sender's connection requests: ${databaseError.message}")
            }
        })
    }

    private fun isConnected(
        connectionId: String,
        connectionsRef: DatabaseReference,
        callback: (Boolean) -> Unit
    ) {
        // Check the status of the connection by its ID
        connectionsRef.child(connectionId).child("status")
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(dataSnapshot: DataSnapshot) {
                    val connectionStatus = dataSnapshot.value as String
                    // Compare the connection status to "connected"
                    callback(connectionStatus == "connected")
                }

                override fun onCancelled(databaseError: DatabaseError) {
                    println("Error checking connection status: ${databaseError.message}")
                    // Handle the error condition, e.g., by calling callback(false)
                    callback(false)
                }
            })
    }


    private val BASE_URL = "https://fcm.googleapis.com/fcm/send"
    private val SERVER_KEY =
        "key=AAAAaZhi2z4:APA91bE5d7PFZB5LyDtXp9CFgkAe7Afnc0il0ETL9zujB_6fwv_mGZu0r_ETCTp-v3nNWh8yDM7DflvrTmycl4sGCFZUg-fWglLIsA3CLNRH2JWv476RtNzhrwfP6sVTzTRxkYCD26GX"
    lateinit var sessionManager: SessionManager
    private var userDetails: HashMap<String, Any> = HashMap()
    fun pushNotification(
        context: Context,
        token: String,
        title: String,
        message: String,
        fullName: String
    ) {

        val policy = StrictMode.ThreadPolicy.Builder().permitAll().build()
        StrictMode.setThreadPolicy(policy)

        val queue = Volley.newRequestQueue(context)

        try {
            val json = JSONObject()
            json.put("to", token)
            val notification = JSONObject()
            notification.put("title", title)
            notification.put("body", message)
            json.put("notification", notification)
            val data = JSONObject()
            data.put("message", message)
            data.put("phoneNumber", userDetails[sessionManager.KEY_PHONENUMBER])
            data.put("userName", fullName)
            json.put("data", data)

            val jsonObjectRequest =
                object : JsonObjectRequest(
                    Method.POST, BASE_URL, json, Response.Listener {
                        println("FCM: $it")
                        // If the FCM message was sent successfully, store it in Firebase Realtime Database
                        val databaseReference = FirebaseDatabase.getInstance().reference
                        val notificationId = databaseReference.child("notifications").push().key
                        if (notificationId != null) {
                            val notificationData = HashMap<String, Any>()
                            notificationData["title"] = title
                            notificationData["body"] = message
                            notificationData["senderPhoneNumber"] =
                                userDetails[sessionManager.KEY_PHONENUMBER].toString()
                            notificationData["fullName"] = fullName
                            notificationData["date"] =
                                ServerValue.TIMESTAMP // Timestamp for the current date
                            notificationData["isUnread"] = true // Set as unread
                            val notificationPath = "notifications/$phoneNumber/$notificationId"
                            databaseReference.child(notificationPath)
                                .updateChildren(notificationData)
                        }
                    },
                    Response.ErrorListener {
                        println("FCM: $it")
                    }) {
                    override fun getHeaders(): MutableMap<String, String> {
                        val headers = HashMap<String, String>()
                        headers["content-type"] = "application/json"
                        headers["Authorization"] = SERVER_KEY
                        return headers
                    }
                }
            queue.add(jsonObjectRequest)
        } catch (e: Exception) {
            println(e.message)
        }
    }

    private fun loadImageWithGlide(imageUrl: String) {
        try {
            Glide.with(this)
                .load(imageUrl)
                .diskCacheStrategy(DiskCacheStrategy.ALL) // Enable caching
                .into(profilePicture)
            profileShimmerLayout.stopShimmer()
            profilePicture.visibility = View.VISIBLE
            profileShimmerLayout.visibility = View.GONE
            profilePicture.setOnClickListener {
                val intent = Intent(this@PopupViewProfile, PicturePopUpActivity::class.java)
                intent.putExtra("imageUrl", imageUrl)
                startActivity(intent)
            }
        } catch (e: Exception) {
            profileShimmerLayout.stopShimmer()
            println(e.message)
        }
    }

    private fun fetchBlogPosts() {
        // Adjust the database reference to include user information
        database = FirebaseDatabase.getInstance().reference
        database.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                val posts = mutableListOf<BlogPost>()
                for (userSnapshot in dataSnapshot.child("blogs").children) {
                    val userPhoneNumber = userSnapshot.key
                    if (userPhoneNumber == phoneNumber)
                        for (uniqueKeySnapshot in userSnapshot.children) {
                            val post = uniqueKeySnapshot.getValue(BlogPost::class.java)
                            post?.let {
                                // Add user's first name to the post
                                it.phoneNumber = userPhoneNumber ?: ""
                                it.blogId = uniqueKeySnapshot.key ?: ""
                                posts.add(it)
                            }
                        }
                }
                adapter.updateData(posts)
                progressBar.visibility = View.GONE
                if (posts.size == 0) {
                    noPostLayout.visibility = View.VISIBLE
                    userBlogRecycler.visibility = View.GONE
                    noOfPosts.text = posts.size.toString()
                } else {
                    noPostLayout.visibility = View.GONE
                    userBlogRecycler.visibility = View.VISIBLE
                    noOfPosts.text = posts.size.toString()
                }

            }

            override fun onCancelled(error: DatabaseError) {
                progressBar.visibility = View.GONE
            }
        })
    }

    override fun onDestroy() {
        super.onDestroy()
        (application as ConnectAppApplication).stopAppUsageTracking()
    }
}