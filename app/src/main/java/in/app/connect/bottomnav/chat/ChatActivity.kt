package `in`.app.connect.bottomnav.chat

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.PopupMenu
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.android.volley.Response
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.Volley
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.database.ChildEventListener
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import `in`.app.connect.PopupViewProfile
import `in`.app.connect.R
import `in`.app.connect.utils.SessionManager
import org.json.JSONException
import org.json.JSONObject

class ChatActivity : AppCompatActivity() {
    private lateinit var messagesAdapter: MessagesAdapter
    private lateinit var messagesList: MutableList<Message>
    private lateinit var databaseReference: DatabaseReference
    private lateinit var currentUserPhoneNumber: String
    private lateinit var chatUserPhoneNumber: String
    private lateinit var sessionManager: SessionManager
    private lateinit var profileImageView: ImageView
    private lateinit var nameTextView: TextView
    private lateinit var typingStatusTextView: TextView
    private lateinit var menuButton: ImageButton
    private lateinit var messagesRecyclerView: RecyclerView
    private lateinit var sendButton: ImageButton
    private lateinit var messageEditText: EditText
    private lateinit var currentUser: FirebaseUser
    private lateinit var typingReference: DatabaseReference
    private lateinit var usersReference: DatabaseReference
    private var isBlocked = false
    private lateinit var receiverToken :String
    private lateinit var chatUserProfileImage :String
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_chat)
        profileImageView = findViewById(R.id.profileImageView)
        nameTextView = findViewById(R.id.nameTextView)
        typingStatusTextView = findViewById(R.id.typingStatusTextView)
        messagesRecyclerView = findViewById(R.id.messagesRecyclerView)
        sendButton = findViewById(R.id.sendButton)
        menuButton = findViewById(R.id.menuButton)
        messageEditText = findViewById(R.id.messageEditText)

        // Get user data from the intent
        chatUserPhoneNumber = intent.getStringExtra("CHAT_USER_PHONE_NUMBER") ?: ""
        receiverToken = intent.getStringExtra("CHAT_USER_FCM_TOKEN") ?: ""
        nameTextView.text = intent.getStringExtra("CHAT_USER_NAME") ?: ""
        chatUserProfileImage = intent.getStringExtra("CHAT_USER_PROFILE_IMAGE") ?: ""
        if (chatUserProfileImage.isNotEmpty()) {
            Glide.with(this)
                .load(chatUserProfileImage)
                .apply(RequestOptions().fitCenter())
                .into(profileImageView)
        } else {
            profileImageView.setImageResource(R.drawable.ic_user)
        }
        nameTextView.setOnClickListener{
            val intent = Intent(this, PopupViewProfile::class.java)
            intent.putExtra(
                "phoneNumber",
                chatUserPhoneNumber
            )
            startActivity(intent)
        }
        profileImageView.setOnClickListener{
            val intent = Intent(this, PopupViewProfile::class.java)
            intent.putExtra(
                "phoneNumber",
                chatUserPhoneNumber
            )
            startActivity(intent)
        }
        sessionManager = SessionManager(this)
        currentUserPhoneNumber =
            sessionManager.getUserDetailFromSession()[sessionManager.KEY_PHONENUMBER].toString()

        messagesList = mutableListOf()
        messagesAdapter = MessagesAdapter(messagesList, this)

        messagesRecyclerView.layoutManager = LinearLayoutManager(this)
        messagesRecyclerView.adapter = messagesAdapter

        databaseReference = FirebaseDatabase.getInstance().getReference("Messages")
        currentUser = FirebaseAuth.getInstance().currentUser!!
        typingReference = FirebaseDatabase.getInstance().getReference("TypingStatus")
        usersReference = FirebaseDatabase.getInstance().getReference("Users")

        sendButton.setOnClickListener {
            if (!isBlocked) {
                sendMessage()
            } else {
                showBlockedAlert()
            }
        }
        menuButton.setOnClickListener {
            showOptionsMenu()
        }
        messageEditText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
                // Not used
            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                updateTypingStatus(s?.isNotEmpty() ?: false)
            }

            override fun afterTextChanged(s: Editable?) {
                // Not used
            }
        })
        fetchMessages()
        listenForTypingStatus()
        listenForUserStatus()
        checkIfBlocked()
    }

    private fun fetchMessages() {
        databaseReference.child(currentUserPhoneNumber).child(chatUserPhoneNumber)
            .addChildEventListener(object : ChildEventListener {
                override fun onChildAdded(snapshot: DataSnapshot, previousChildName: String?) {
                    val message = snapshot.getValue(Message::class.java)
                    if (message != null) {
                        messagesList.add(message)
                        messagesAdapter.notifyItemInserted(messagesList.size - 1)
                        messagesRecyclerView.scrollToPosition(messagesList.size - 1)
                    }
                }

                override fun onChildChanged(snapshot: DataSnapshot, previousChildName: String?) {
                    // Handle child changed if necessary
                }

                override fun onChildRemoved(snapshot: DataSnapshot) {
                    // Handle child removed if necessary
                }

                override fun onChildMoved(snapshot: DataSnapshot, previousChildName: String?) {
                    // Handle child moved if necessary
                }

                override fun onCancelled(error: DatabaseError) {
                    // Handle possible errors
                }
            })
    }

    private fun sendPushNotification(receiverToken: String, message: String) {
        try {
            val json = JSONObject()
            json.put("to", receiverToken)
            val notification = JSONObject()
notification.put("title", "New chat from ${sessionManager.getUserDetailFromSession()[sessionManager.KEY_FULLNAME].toString()}")
notification.put("body", message)
json.put("notification", notification)
val data = JSONObject()
data.put("message", message)
data.put("phoneNumber", currentUserPhoneNumber)
        data.put("chat", true)
        json.put("data", data)

        val url = "https://fcm.googleapis.com/fcm/send"
        val jsonObjectRequest = object : JsonObjectRequest(
            Method.POST, url, json,
            Response.Listener { response ->
                // Handle success
            },
            Response.ErrorListener { error ->
                // Handle error
            }) {

            override fun getHeaders(): MutableMap<String, String> {
                val headers = HashMap<String, String>()
                headers["Content-Type"] = "application/json"
                headers["Authorization"] = "key=AAAAaZhi2z4:APA91bE5d7PFZB5LyDtXp9CFgkAe7Afnc0il0ETL9zujB_6fwv_mGZu0r_ETCTp-v3nNWh8yDM7DflvrTmycl4sGCFZUg-fWglLIsA3CLNRH2JWv476RtNzhrwfP6sVTzTRxkYCD26GX" // Replace with your server key
                return headers
                }
            }

            // Add the request to the RequestQueue
            Volley.newRequestQueue(this).add(jsonObjectRequest)

        } catch (e: JSONException) {
            e.printStackTrace()
        }
    }
    private fun sendMessage() {
        val messageText = messageEditText.text.toString().trim()
        if (messageText.isNotEmpty()) {
            if (!isBlocked) {
                val messageId = databaseReference.push().key
                val message = Message(currentUserPhoneNumber, chatUserPhoneNumber, messageText)

                if (messageId != null) {
                    databaseReference.child(currentUserPhoneNumber).child(chatUserPhoneNumber)
                        .child(messageId).setValue(message)
                    databaseReference.child(chatUserPhoneNumber).child(currentUserPhoneNumber)
                        .child(messageId).setValue(message)
                    // Send push notification
                    sendPushNotification(receiverToken, messageText)
                }

                messageEditText.text.clear()
            } else {
                showBlockedAlert()
            }
        }
    }


    private fun showOptionsMenu() {
        val popupMenu = PopupMenu(this, menuButton)
        popupMenu.menuInflater.inflate(R.menu.menu_message_options, popupMenu.menu)
        popupMenu.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                R.id.menu_block -> {
                    val blockMenuItem = popupMenu.menu.findItem(R.id.menu_block)
                    if (!isBlocked) {
                        showBlockConfirmationDialog(blockMenuItem)
                    } else {
                        showUnblockConfirmationDialog(blockMenuItem)
                    }
                    true
                }

                R.id.menu_clear_chat -> {
                    clearChat()
                    true
                }

                else -> false
            }
        }
        popupMenu.show()
    }

    private fun showBlockConfirmationDialog(blockMenuItem: MenuItem) {
        AlertDialog.Builder(this)
            .setTitle("Block User")
            .setMessage("Are you sure you want to block this user?")
            .setPositiveButton("Block") { _, _ ->
                blockUser()
                updateMenuTitle(blockMenuItem,true)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showUnblockConfirmationDialog(blockMenuItem: MenuItem) {
        AlertDialog.Builder(this)
            .setTitle("Unblock User")
            .setMessage("Are you sure you want to unblock this user?")
            .setPositiveButton("Unblock") { _, _ ->
                unblockUser()
                updateMenuTitle(blockMenuItem,false)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }


    private fun blockUser() {
        val blockMap = HashMap<String, Any>()
        blockMap["blocked"] = true

        usersReference.child(currentUserPhoneNumber).child("BlockedUsers")
            .child(chatUserPhoneNumber)
            .setValue(blockMap)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    isBlocked = true
                    showBlockedUI()
                    invalidateOptionsMenu()
                } else {
                    // Handle error
                }
            }
    }

    private fun unblockUser() {
        usersReference.child(currentUserPhoneNumber).child("BlockedUsers")
            .child(chatUserPhoneNumber)
            .removeValue()
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    isBlocked = false
                    showUnblockedUI()
                    invalidateOptionsMenu()
                } else {
                    // Handle error
                }
            }
    }

    private fun showBlockedUI() {
        messageEditText.isEnabled = false
        sendButton.isEnabled = false
        messageEditText.hint = "You can't send messages to this user."
        typingStatusTextView.text = "You're Blocked"
    }

    private fun showUnblockedUI() {
        messageEditText.isEnabled = true
        sendButton.isEnabled = true
        messageEditText.hint = "Type a message..."
        typingStatusTextView.text = ""
    }

    private fun showBlockedAlert() {
        AlertDialog.Builder(this)
            .setTitle("You're Blocked")
            .setMessage("You can't send messages to this user.")
            .setPositiveButton("OK", null)
            .show()
    }

    private fun clearChat() {
        AlertDialog.Builder(this)
            .setTitle("Clear Chat")
            .setMessage("Are you sure you want to clear this chat?")
            .setPositiveButton("Clear") { _, _ ->
                databaseReference.child(currentUserPhoneNumber).child(chatUserPhoneNumber)
                    .removeValue()
                messagesList.clear()
                messagesAdapter.notifyDataSetChanged()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }


    private fun updateTypingStatus(isTyping: Boolean) {
        if (isTyping) {
            typingReference.child(chatUserPhoneNumber).child(currentUserPhoneNumber)
                .setValue("typing")
        } else {
            typingReference.child(chatUserPhoneNumber).child(currentUserPhoneNumber).removeValue()
        }
    }

    private fun listenForTypingStatus() {
        typingReference.child(currentUserPhoneNumber).child(chatUserPhoneNumber)
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val typingStatus = snapshot.getValue(String::class.java)
                    if (typingStatus != null && typingStatus == "typing") {
                        typingStatusTextView.text = "Typing..."
                    } else {
                        listenForUserStatus()  // Clear the typing status
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    // Handle possible errors
                }
            })
    }

    private fun listenForUserStatus() {
        usersReference.child(chatUserPhoneNumber).child("status")
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val status = snapshot.value?.toString()
                    if (status != null && status == "online") {
                        typingStatusTextView.text = "Online"
                    } else {
                        typingStatusTextView.text = "Offline"
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    // Handle possible errors
                }
            })
    }

    private fun checkIfBlocked() {
        usersReference.child(currentUserPhoneNumber).child("BlockedUsers")
            .child(chatUserPhoneNumber)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    if (snapshot.exists() && snapshot.child("blocked").value == true) {
                        isBlocked = true
                        showBlockedUI()
                    } else {
                        isBlocked = false
                        showUnblockedUI()
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    // Handle possible errors
                }
            })
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_message_options, menu)
        return true
    }

    override fun onPrepareOptionsMenu(menu: Menu): Boolean {
        val menuItem = menu.findItem(R.id.menu_block)
        updateMenuTitle(menuItem, isBlocked)
        return true
    }

    private fun updateMenuTitle(menuItem: MenuItem?, isBlocked: Boolean) {
        menuItem?.apply {
            println(isBlocked)
            setTitle(if (isBlocked) "Unblock" else "Block")
            invalidateOptionsMenu() // Add this line
        }
    }


//    override fun onOptionsItemSelected(item: MenuItem): Boolean {
//        return when (item.itemId) {
//            R.id.menu_block -> {
//                showBlockConfirmationDialog()
//                true
//            }
//
//            R.id.menu_clear_chat -> {
//                clearChat()
//                true
//            }
//
//            R.id.menu_search_chat -> {
//                // Implement search chat functionality here
//                true
//            }
//
//            else -> super.onOptionsItemSelected(item)
//        }
//    }

    override fun onStart() {
        super.onStart()
        updateOnlineStatus(true)
    }

    override fun onStop() {
        super.onStop()
        updateOnlineStatus(false)
    }

    private fun updateOnlineStatus(isOnline: Boolean) {
        val databaseReference = FirebaseDatabase.getInstance().getReference("Users")
        val status = if (isOnline) "online" else "offline"
        val userStatusMap = HashMap<String, Any>()
        userStatusMap["status"] = status
        typingStatusTextView.text = status
        databaseReference.child(currentUserPhoneNumber).updateChildren(userStatusMap)
    }
}

class MessagesAdapter(private val messages: List<Message>, private val context: Context) :
    RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    companion object {
        private const val VIEW_TYPE_SENT = 1
        private const val VIEW_TYPE_RECEIVED = 2
    }

    private val currentUserPhoneNumber: String

    init {
        val sessionManager = SessionManager(context)
        currentUserPhoneNumber =
            sessionManager.getUserDetailFromSession()[sessionManager.KEY_PHONENUMBER].toString()
    }

    override fun getItemViewType(position: Int): Int {
        return if (messages[position].sender == currentUserPhoneNumber) {
            VIEW_TYPE_SENT
        } else {
            VIEW_TYPE_RECEIVED
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return if (viewType == VIEW_TYPE_SENT) {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_message_sent, parent, false)
            SentMessageViewHolder(view)
        } else {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_message_received, parent, false)
            ReceivedMessageViewHolder(view)
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val message = messages[position]
        if (holder.itemViewType == VIEW_TYPE_SENT) {
            (holder as SentMessageViewHolder).bind(message)
        } else {
            (holder as ReceivedMessageViewHolder).bind(message)
        }
    }

    override fun getItemCount() = messages.size

    class SentMessageViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val messageTextView: TextView = itemView.findViewById(R.id.messageTextView)

        fun bind(message: Message) {
            messageTextView.text = message.message
        }
    }

    class ReceivedMessageViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val messageTextView: TextView = itemView.findViewById(R.id.messageTextView)

        fun bind(message: Message) {
            messageTextView.text = message.message
        }
    }
}

data class Message(
    val sender: String = "",
    val receiver: String = "",
    val message: String = "",
    val timestamp: Long = System.currentTimeMillis()
)