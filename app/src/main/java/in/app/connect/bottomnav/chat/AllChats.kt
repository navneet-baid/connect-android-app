package `in`.app.connect.bottomnav.chat

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import de.hdodenhof.circleimageview.CircleImageView
import `in`.app.connect.R
import `in`.app.connect.utils.SessionManager
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.HashMap
import java.util.Locale

class AllChats : Fragment() {
    private lateinit var recentChatRecycler: RecyclerView
    private lateinit var connectionList: MutableList<UserData>
    private lateinit var emptyTextView:TextView
    private lateinit var connectionAdapter: ConnectionAdapter
    private lateinit var databaseReference: DatabaseReference
    private lateinit var currentUserPhoneNumber: String
    lateinit var sessionManager: SessionManager
    private var userDetails: HashMap<String, Any> = HashMap()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        val view = inflater.inflate(R.layout.fragment_all_chats, container, false)
        recentChatRecycler = view.findViewById(R.id.recentChatsRecyclerView)
        emptyTextView = view.findViewById(R.id.emptyTextView)
        connectionList = mutableListOf()
        connectionAdapter = ConnectionAdapter(connectionList, requireContext())
        recentChatRecycler.adapter = connectionAdapter
        recentChatRecycler.layoutManager = LinearLayoutManager(requireContext())
        sessionManager = SessionManager(requireContext())
        userDetails = sessionManager.getUserDetailFromSession()
        currentUserPhoneNumber = userDetails[sessionManager.KEY_PHONENUMBER].toString()
        fetchConnectedUsers()
        return view
    }
    private fun fetchConnectedUsers() {
        databaseReference = FirebaseDatabase.getInstance().getReference("Connections")
        databaseReference.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val connections = mutableListOf<Connection>()
                for (dataSnapshot in snapshot.children) {
                    val connection = dataSnapshot.getValue(Connection::class.java)
                    if (connection != null) {
                        if (connection.status == "connected" &&
                            (connection.sender == currentUserPhoneNumber || connection.receiver == currentUserPhoneNumber)
                        ) {
                            connections.add(connection)
                        }
                    }
                }
                fetchUserDetails(connections)
            }

            override fun onCancelled(error: DatabaseError) {
                // Handle possible errors.
            }
        })
    }

    private fun fetchUserDetails(connections: List<Connection>) {
        val userPhoneNumbers = connections.map {
            if (it.sender == currentUserPhoneNumber) it.receiver else it.sender
        }

        val usersReference = FirebaseDatabase.getInstance().getReference("Users")
        val totalUsers = userPhoneNumbers.size
        var fetchedUsers = 0

        for (phoneNumber in userPhoneNumbers) {
            usersReference.child(phoneNumber).addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val user = snapshot.getValue(UserData::class.java)

                    if (user != null) {
                        getLastMessageAndTimestamp(user, connections)
                        // Check if user is already in the connectionList
                        if (!connectionList.contains(user)) {
                            connectionList.add(user)
                            connectionAdapter.notifyDataSetChanged()
                        }
                    }

                    fetchedUsers++
                    if (fetchedUsers == totalUsers) {
                        // After all users are fetched, update UI
                        if (connectionList.isEmpty()) {
                            emptyTextView.visibility = View.VISIBLE
                            recentChatRecycler.visibility = View.GONE
                        } else {
                            emptyTextView.visibility = View.GONE
                            recentChatRecycler.visibility = View.VISIBLE
                        }
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    // Handle possible errors.
                    fetchedUsers++
                    if (fetchedUsers == totalUsers) {
                        // After all attempts, update UI
                        if (connectionList.isEmpty()) {
                            emptyTextView.visibility = View.VISIBLE
                            recentChatRecycler.visibility = View.GONE
                        } else {
                            emptyTextView.visibility = View.GONE
                            recentChatRecycler.visibility = View.VISIBLE
                        }
                    }
                }
            })
        }
    }

    private fun getLastMessageAndTimestamp(user: UserData, connections: List<Connection>) {
        val chatRef = FirebaseDatabase.getInstance().getReference("Messages")
        val currentUserId = currentUserPhoneNumber
        val otherUserId = user.phoneNumber

        chatRef.child(currentUserId).child(otherUserId).limitToLast(1)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    if (snapshot.exists()) {
                        for (messageSnapshot in snapshot.children) {
                            val message = messageSnapshot.getValue(Message::class.java)
                            if (message != null) {
                                user.lastMessage = message.message
                                user.lastMessageTimestamp = message.timestamp
                            }
                        }
                    } else {
                        // If no messages found, handle accordingly
                        user.lastMessage = "No messages yet"
                        user.lastMessageTimestamp=0L
                    }
                    connectionAdapter.notifyDataSetChanged()
                }

                override fun onCancelled(error: DatabaseError) {
                    // Handle possible errors
                }
            })
    }
}

class ConnectionAdapter(private val users: List<UserData>, private val context: Context) : RecyclerView.Adapter<ConnectionAdapter.ConnectionViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ConnectionViewHolder {
        val itemView = LayoutInflater.from(parent.context).inflate(R.layout.chat_card, parent, false)
        return ConnectionViewHolder(itemView, context)
    }

    override fun onBindViewHolder(holder: ConnectionViewHolder, position: Int) {
        val user = users[position]
        holder.bind(user)
    }

    override fun getItemCount() = users.size

    class ConnectionViewHolder(itemView: View, private val context: Context) : RecyclerView.ViewHolder(itemView) {
        private val userNameTextView: TextView = itemView.findViewById(R.id.nameTextView)
        private val profileImageView: CircleImageView = itemView.findViewById(R.id.profileImageView)
        private val lastMessageTextView: TextView =
            itemView.findViewById(R.id.lastMsgTextView)
        private val lastMessageTimeTextView: TextView =
            itemView.findViewById(R.id.timeTextView)

        fun bind(user: UserData) {
            userNameTextView.text = user.userName
            lastMessageTextView.text = user.lastMessage
            lastMessageTimeTextView.text = if(user.lastMessageTimestamp!=0L){formatTimestamp(user.lastMessageTimestamp)}else{""}

            if (user.images.isNotEmpty()) {
                Glide.with(context)
                    .load(user.images[0])
                    .apply(RequestOptions().fitCenter())
                    .into(profileImageView)
            } else {
                profileImageView.setImageResource(R.drawable.ic_user)
            }
            itemView.setOnClickListener {
                val intent = Intent(context, ChatActivity::class.java).apply {
                    putExtra("CHAT_USER_PHONE_NUMBER", user.phoneNumber)
                    putExtra("CHAT_USER_NAME", user.userName)
                    putExtra("CHAT_USER_PROFILE_IMAGE", user.images[0])
                }
                context.startActivity(intent)
            }
        }

        private fun formatTimestamp(timestamp: Long): String {
            val now = Calendar.getInstance()
            val messageTime = Calendar.getInstance()
            messageTime.timeInMillis = timestamp

            val dayOfYear = 1000 * 60 * 60 * 24

            return when {
                now.get(Calendar.DAY_OF_YEAR) == messageTime.get(Calendar.DAY_OF_YEAR) -> {
                    SimpleDateFormat("hh:mm a", Locale.getDefault()).format(messageTime.time)
                }
                now.get(Calendar.WEEK_OF_YEAR) == messageTime.get(Calendar.WEEK_OF_YEAR) -> {
                    val daysAgo = (now.timeInMillis - timestamp) / dayOfYear
                    "${daysAgo}d"
                }
                else -> {
                    SimpleDateFormat("dd/MM hh:mm", Locale.getDefault()).format(messageTime.time)
                }
            }
        }
    }

}

data class Connection(
    val sender: String = "",
    val receiver: String = "",
    val status: String = ""
)
data class UserData(
    val phoneNumber: String = "",
    val userName: String = "",
    val images: List<String> = emptyList(),
    var lastMessage: String = "",
    var lastMessageTimestamp: Long = 0L
)
