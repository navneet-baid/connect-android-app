package `in`.app.connect.bottomnav.globalprofile

import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat.startActivity
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.request.RequestOptions
import `in`.app.connect.R
import com.google.firebase.database.*
import com.squareup.picasso.Picasso
import `in`.app.connect.MainActivity
import `in`.app.connect.PopupViewProfile
import `in`.app.connect.usermanagment.models.UserData
import `in`.app.connect.utils.SessionManager
import java.util.Calendar
import java.util.HashMap

class BottomGlobalProfileFragment : Fragment(), MainActivity.RefreshableFragment {
    override fun refreshContent() {
        fetchUserProfiles()
    }

    private lateinit var database: DatabaseReference
    private lateinit var userProfiles: MutableList<UserProfile> // Create a data structure to hold user profiles
    private lateinit var userProfileRecycler: RecyclerView // RecyclerView from XML
    lateinit var sessionManager: SessionManager
    private var userDetails: HashMap<String, Any> = HashMap()
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_bottom_global_profile, container, false)
        println(requireActivity().intent.data)
        userProfileRecycler =
            view.findViewById(R.id.userProfileRecycler) // Replace with your RecyclerView ID
        val layoutManager = GridLayoutManager(requireContext(), 2)
        layoutManager.spanCount = 2 // This ensures 2 columns in the grid
        layoutManager.reverseLayout = false // Set this to true if you want to reverse the layout
        userProfileRecycler.layoutManager = layoutManager
        // Initialize Firebase Realtime Database reference
        database = FirebaseDatabase.getInstance().reference.child("Users")
        // Reference your TextView here
        userProfiles = mutableListOf() // Initialize the list
        sessionManager = SessionManager(requireContext())
        userDetails = sessionManager.getUserDetailFromSession()
        fetchUserProfiles()

        return view
    }

    private fun fetchUserProfiles() {
        userProfiles= mutableListOf()
        // Attach a listener to the reference
        database.addChildEventListener(object : ChildEventListener {
            override fun onChildAdded(snapshot: DataSnapshot, previousChildName: String?) {
                val phoneNumber = snapshot.key
                val userData = snapshot.getValue(UserData::class.java)
                if (userData != null && userDetails[sessionManager.KEY_PHONENUMBER].toString() != phoneNumber) {
                    userProfiles.add(UserProfile(phoneNumber!!, userData))
                    userProfileRecycler.adapter?.notifyDataSetChanged()
                }
            }

            override fun onChildChanged(snapshot: DataSnapshot, previousChildName: String?) {
                // Handle changes if needed
            }

            override fun onChildRemoved(snapshot: DataSnapshot) {
                // Handle removals if needed
            }

            override fun onChildMoved(snapshot: DataSnapshot, previousChildName: String?) {
                // Handle moves if needed
            }

            override fun onCancelled(error: DatabaseError) {
                // Handle errors if needed
            }
        })

        // Set up your RecyclerView adapter using the userProfiles list
        val adapter =
            UserProfileAdapter(requireContext(), userProfiles) // You need to create this adapter
        userProfileRecycler.adapter = adapter
    }
}


// UserProfile.kt
data class UserProfile(
    val phoneNumber: String = "",
    val userData: UserData = UserData()
)

class UserProfileAdapter(
    private val context: Context,
    private val userProfiles: List<UserProfile>
) : RecyclerView.Adapter<UserProfileAdapter.ViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.userscard, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val userProfile = userProfiles[position]
        // Bind data to the views in the ViewHolder
        holder.usernameTextView.text = userProfile.userData.userName
        holder.genderTextView.text = userProfile.userData.gender
        //glide
        val userImageView: ImageView = holder.userImageView
        val imageUrl: String = userProfile.userData.images[0]

        val requestOptions = RequestOptions()
            .placeholder(ColorDrawable(Color.GRAY))
            .diskCacheStrategy(DiskCacheStrategy.ALL) // Enable caching

        Glide.with(context)
            .load(imageUrl)
            .apply(requestOptions)
            .into(userImageView)
        // Inside UserProfileAdapter's onBindViewHolder method
        holder.itemView.setOnClickListener {
            val userProfile = userProfiles[position]
            if (userProfile.userData.userName.isEmpty()) {
                // Show Shimmer effect if data is not yet loaded


                holder.contentLayout.visibility = View.GONE

            } else {
                // Hide Shimmer effect and show content when data is available
                holder.contentLayout.visibility = View.VISIBLE

                val dialog = Dialog(holder.itemView.context)
                dialog.setContentView(R.layout.popup_user_profile)


                // Initialize dialog views
                val dialogUsernameTextView =
                    dialog.findViewById<TextView>(R.id.dialogUsernameTextView)
                val dialogAgeTextView = dialog.findViewById<TextView>(R.id.dialogAgeTextView)
                val dialogGenderTextView = dialog.findViewById<TextView>(R.id.dialogGenderTextView)
                val dialogBioTextView = dialog.findViewById<TextView>(R.id.dialogBioTextView)
                val dialogUserImageView = dialog.findViewById<ImageView>(R.id.dialogUserImageView)
                val dialogShowProfileButton =
                    dialog.findViewById<Button>(R.id.dialogShowProfileButton)
                val dialogConnectButton = dialog.findViewById<Button>(R.id.dialogConnectButton)

                val dob = userProfile.userData.dob
                if (!dob.isNullOrBlank()) {
                    val dobParts = dob.split("-")
                    if (dobParts.size == 3) {
                        val year = dobParts[0].toInt()
                        val currentYear = Calendar.getInstance().get(Calendar.YEAR)
                        val age = currentYear - year
                        dialogAgeTextView.text = "Age: $age"
                    }
                }


                // Set data to dialog views
                dialogUsernameTextView.text = userProfile.userData.userName
                dialogGenderTextView.text = userProfile.userData.gender
                dialogBioTextView.text = userProfile.userData.bio
                dialogAgeTextView.text = userProfile.userData.dob
                Picasso.get().load(userProfile.userData.images[0])
                    .placeholder(ColorDrawable(Color.GRAY)).into(dialogUserImageView)

                // Set onClickListener for dialog buttons
                dialogShowProfileButton.setOnClickListener {
                    dialog.dismiss()
                    val intent = Intent(context, PopupViewProfile::class.java)
                    intent.putExtra("phoneNumber", userProfile.phoneNumber)
                    context.startActivity(intent)
                }


                dialogConnectButton.setOnClickListener {
                    // Handle connect button click
                }

                dialog.show()
            }

        }
    }

    override fun getItemCount(): Int {
        return userProfiles.size
    }

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

        val contentLayout: ViewGroup = itemView.findViewById(R.id.contentLayout)
        val usernameTextView = itemView.findViewById<TextView>(R.id.usernameTextView)
        val genderTextView = itemView.findViewById<TextView>(R.id.genderTextView)
        val userImageView = itemView.findViewById<ImageView>(R.id.userImageView)

    }
}

