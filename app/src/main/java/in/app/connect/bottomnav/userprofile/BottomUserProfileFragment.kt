package `in`.app.connect.bottomnav.userprofile


import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.PopupMenu
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.facebook.shimmer.ShimmerFrameLayout
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.api.Distribution.BucketOptions.Linear
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.dynamiclinks.DynamicLink
import com.google.firebase.dynamiclinks.FirebaseDynamicLinks
import com.google.firebase.storage.FirebaseStorage
import com.google.gson.Gson
import com.squareup.picasso.Picasso
import `in`.app.connect.MainActivity
import `in`.app.connect.R
import `in`.app.connect.authentication.LoginActivity
import `in`.app.connect.bottomnav.Blog.BlogDetails
import `in`.app.connect.utils.SessionManager
import java.text.SimpleDateFormat
import java.util.Date
import java.util.HashMap
import java.util.concurrent.TimeUnit

class BottomUserProfileFragment : Fragment(), MainActivity.RefreshableFragment {
    override fun refreshContent() {
        adapter.updateData(mutableListOf()) // Clear the list of blog posts
        progressBar.visibility = View.VISIBLE
        fetchPendingConnectionsCount(
            FirebaseDatabase.getInstance().reference.child("Connections"),
            userDetails[sessionManager.KEY_PHONENUMBER].toString(),
            object : PendingConnectionsCountCallback {
                override fun onPendingConnectionsCount(pendingCount: Int) {
                    pendingConnectionsCount.text = pendingCount.toString()
                    if (pendingCount > 0)
                        pendingConnectionsLayout.setOnClickListener {
                            // Handle when the "Pending Connections" view is clicked
                            val intent =
                                Intent(requireContext(), PendingConnectionsActivity::class.java)
                            val phoneNumber = userDetails[sessionManager.KEY_PHONENUMBER]!!
                            intent.putExtra("phoneNumber", phoneNumber.toString())
                            startActivity(intent)
                        }
                }
            }
        )
        fetchConnectedConnectionsCount(
            FirebaseDatabase.getInstance().reference.child("Connections"),
            userDetails[sessionManager.KEY_PHONENUMBER].toString(),
            object : ConnectedConnectionsCountCallback {
                override fun onConnectedConnectionsCount(connectedCount: Int) {
                    connectedConnectionsCount.text = connectedCount.toString()
                    if (connectedCount > 0)
                        connectedConnectionsLayout.setOnClickListener {
                            // Handle when the "Connected Connections" view is clicked
                            val intent =
                                Intent(requireContext(), ConnectedConnectionsActivity::class.java)
                            val phoneNumber = userDetails[sessionManager.KEY_PHONENUMBER]!!
                            intent.putExtra("phoneNumber", phoneNumber.toString())
                            startActivity(intent)
                        }
                }
            }
        )
        fetchImageUrlsFromStorage(userDetails[sessionManager.KEY_PHONENUMBER].toString())
        fetchBlogPosts()
    }

    private lateinit var profilePicture: ImageView
    private lateinit var qrCodeImageView: ImageView
    private lateinit var profileName: TextView
    private lateinit var location: TextView
    private lateinit var hometown: TextView
    private lateinit var userAge: TextView
    private lateinit var userGender: TextView
    private lateinit var userBio: TextView
    private lateinit var userBlogRecycler: RecyclerView
    lateinit var sessionManager: SessionManager
    private var userDetails: HashMap<String, Any> = HashMap()
    private lateinit var adapter: BlogPostAdapter
    private lateinit var database: DatabaseReference
    private lateinit var progressBar: ProgressBar
    private lateinit var noOfPosts: TextView
    private lateinit var pendingConnectionsCount: TextView
    private lateinit var pendingConnectionsLayout: LinearLayout
    private lateinit var connectedConnectionsCount: TextView
    private lateinit var connectedConnectionsLayout: LinearLayout
    private lateinit var noPostLayout: LinearLayout
    private lateinit var profileShimmerLayout: ShimmerFrameLayout

    @SuppressLint("MissingInflatedId")
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view =
            inflater.inflate(R.layout.fragment_bottom_user_profile_fragment, container, false)
        profilePicture = view.findViewById(R.id.profileImage)
        profileName = view.findViewById(R.id.profileName)
        userAge = view.findViewById(R.id.userAge)
        location = view.findViewById(R.id.location)
        hometown = view.findViewById(R.id.hometown)
        userGender = view.findViewById(R.id.userGender)
        userBio = view.findViewById(R.id.userBio)
        userBlogRecycler = view.findViewById(R.id.userBlogRecycler)
        noOfPosts = view.findViewById(R.id.noOfPosts)
        progressBar = view.findViewById(R.id.progressBar)
        profileShimmerLayout = view.findViewById(R.id.profileShimmerLayout)
        noPostLayout = view.findViewById(R.id.noPostLayout)
        qrCodeImageView = view.findViewById(R.id.qrCodeImageView)
        pendingConnectionsCount = view.findViewById(R.id.pendingConnectionsCount)
        pendingConnectionsLayout = view.findViewById(R.id.pendingConnectionsLayout)
        connectedConnectionsCount = view.findViewById(R.id.connectedConnectionsCount)
        connectedConnectionsLayout = view.findViewById(R.id.connectedConnectionsLayout)
        sessionManager = SessionManager(requireContext())
        userDetails = sessionManager.getUserDetailFromSession()
        profileName.text = userDetails[sessionManager.KEY_FULLNAME].toString()
        hometown.text = userDetails[sessionManager.KEY_HOMETOWN].toString()
        location.text = userDetails[sessionManager.KEY_LOCATION].toString()
        val sdf = SimpleDateFormat("dd/MM/yyyy")
        val birthDate: Date = sdf.parse(userDetails[sessionManager.KEY_DOB].toString()) as Date
        val currentDate = Date()
        val diffInMillis: Long = currentDate.time - birthDate.time
        val ageInDays: Long = TimeUnit.MILLISECONDS.toDays(diffInMillis)
        val ageInYears: Int = (ageInDays / 365.25).toInt()
        userAge.text = ageInYears.toString()
        userGender.text = userDetails[sessionManager.KEY_GENDER].toString()
        userBio.text = userDetails[sessionManager.KEY_BIO].toString()
        val layoutManager =
            LinearLayoutManager(requireContext(), LinearLayoutManager.VERTICAL, false)
        fetchPendingConnectionsCount(
            FirebaseDatabase.getInstance().reference.child("Connections"),
            userDetails[sessionManager.KEY_PHONENUMBER].toString(),
            object : PendingConnectionsCountCallback {
                override fun onPendingConnectionsCount(pendingCount: Int) {
                    pendingConnectionsCount.text = pendingCount.toString()
                    if (pendingCount > 0)
                        pendingConnectionsLayout.setOnClickListener {
                            // Handle when the "Pending Connections" view is clicked
                            val intent =
                                Intent(requireContext(), PendingConnectionsActivity::class.java)
                            val phoneNumber = userDetails[sessionManager.KEY_PHONENUMBER]!!
                            intent.putExtra("phoneNumber", phoneNumber.toString())
                            startActivity(intent)
                        }
                }
            }
        )
        fetchConnectedConnectionsCount(
            FirebaseDatabase.getInstance().reference.child("Connections"),
            userDetails[sessionManager.KEY_PHONENUMBER].toString(),
            object : ConnectedConnectionsCountCallback {
                override fun onConnectedConnectionsCount(connectedCount: Int) {
                    connectedConnectionsCount.text = connectedCount.toString()
                    if (connectedCount > 0)
                        connectedConnectionsLayout.setOnClickListener {
                            // Handle when the "Connected Connections" view is clicked
                            val intent =
                                Intent(requireContext(), ConnectedConnectionsActivity::class.java)
                            val phoneNumber = userDetails[sessionManager.KEY_PHONENUMBER]!!
                            intent.putExtra("phoneNumber", phoneNumber.toString())
                            startActivity(intent)
                        }
                }
            }
        )


        userBlogRecycler.layoutManager = layoutManager
        adapter = BlogPostAdapter(
            requireContext(),
            mutableListOf(),
            userDetails[sessionManager.KEY_PHONENUMBER].toString(),
            true
        )
        userBlogRecycler.adapter = adapter

        fetchImageUrlsFromStorage(userDetails[sessionManager.KEY_PHONENUMBER].toString())
        fetchBlogPosts()

        qrCodeImageView.setOnClickListener {
            val intent = Intent(requireContext(), QrCodeManager::class.java)
            startActivity(intent)
        }
        return view
    }

    interface PendingConnectionsCountCallback {
        fun onPendingConnectionsCount(count: Int)
    }

    private fun fetchPendingConnectionsCount(
        connectionsRef: DatabaseReference,
        userPhoneNumber: String,
        callback: PendingConnectionsCountCallback
    ) {
        connectionsRef.orderByChild("receiver").equalTo(userPhoneNumber)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(dataSnapshot: DataSnapshot) {
                    var count = 0
                    for (snapshot in dataSnapshot.children) {
                        val status = snapshot.child("status").value.toString()
                        if (status == "requested") {
                            count++
                        }
                    }
                    callback.onPendingConnectionsCount(count)
                }

                override fun onCancelled(databaseError: DatabaseError) {
                    println("Error fetching pending connection count: ${databaseError.message}")
                    callback.onPendingConnectionsCount(0) // Handle errors by indicating no pending connections
                }
            })
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


    private fun fetchBlogPosts() {
        // Adjust the database reference to include user information
        database = FirebaseDatabase.getInstance().reference
        database.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                val posts = mutableListOf<BlogPost>()
                for (userSnapshot in dataSnapshot.child("blogs").children) {
                    val userPhoneNumber = userSnapshot.key
                    if (userPhoneNumber == userDetails[sessionManager.KEY_PHONENUMBER].toString())
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

    private fun fetchImageUrlsFromStorage(userNumber: String) {
        val storage = FirebaseStorage.getInstance()
        val storageRef = storage.getReference("RecentPicture/$userNumber")
        profileShimmerLayout.startShimmer()
        profilePicture.visibility = View.GONE
        profileShimmerLayout.visibility = View.VISIBLE

        storageRef.listAll()
            .addOnSuccessListener { listResult ->
                if (listResult.items.isNotEmpty()) {
                    val firstImageRef = listResult.items[0]
                    firstImageRef.downloadUrl.addOnSuccessListener { uri ->
                        loadImageWithGlide(uri.toString())
                        profilePicture.setOnClickListener {
                            val intent = Intent(requireContext(), PicturePopUpActivity::class.java)
                            intent.putExtra("imageUrl", uri.toString())
                            startActivity(intent)
                        }
                    }.addOnFailureListener { exception ->
                        println("Error fetching image URL: ${exception.message}")
                        profileShimmerLayout.stopShimmer()
                    }
                } else {
                    Picasso.get()
                        .load(R.drawable.ic_user)
                        .into(profilePicture)
                    profileShimmerLayout.stopShimmer()
                }
            }
            .addOnFailureListener { exception ->
                println("Error fetching image URL: ${exception.message}")
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
        } catch (e: Exception) {
            profileShimmerLayout.stopShimmer()
            println(e.message)
        }

    }

    /*Bottom menu class*/
    class BottomSheetMenuFragment : BottomSheetDialogFragment() {
        lateinit var sessionManager: SessionManager
        private var userDetails: HashMap<String, Any> = HashMap()
        override fun onCreateView(
            inflater: LayoutInflater,
            container: ViewGroup?,
            savedInstanceState: Bundle?
        ): View? {
            val view = inflater.inflate(R.layout.bottom_menu, container, false)

            val textLogout = view.findViewById<LinearLayout>(R.id.logout)
            val textEditProfile = view.findViewById<LinearLayout>(R.id.editProfile)
            val MyContacts = view.findViewById<LinearLayout>(R.id.MyContacts)
            val myActivity = view.findViewById<LinearLayout>(R.id.myActivity)
            val qrCode = view.findViewById<LinearLayout>(R.id.qrCode)
            val accountCenter = view.findViewById<LinearLayout>(R.id.accountCenter)

            sessionManager = SessionManager(requireContext())
            userDetails = sessionManager.getUserDetailFromSession()
            textLogout.setOnClickListener {
                val builder = AlertDialog.Builder(requireContext())
                builder.setTitle("Logout")
                builder.setMessage("Are you sure you want to log out?")
                builder.setPositiveButton("Yes") { _, _ ->
                    // User clicked "Yes", log out
                    sessionManager.logoutUserFromSession()
                    FirebaseAuth.getInstance().signOut()
                    startActivity(Intent(requireContext(), LoginActivity::class.java))
                    requireActivity().finish()
                }
                builder.setNegativeButton("No") { dialog, _ ->
                    // User clicked "No", dismiss the dialog
                    dialog.dismiss()
                }
                val dialog = builder.create()
                dialog.show()
            }

            MyContacts.setOnClickListener {
                startActivity(Intent(requireContext(), ContactsActivity::class.java))
                dismiss()
            }
            accountCenter.setOnClickListener {
                startActivity(Intent(requireContext(), AccountCenter::class.java))
                dismiss()
            }

            textEditProfile.setOnClickListener {

                val intent = Intent(requireContext(), Setting_Menu_Activity::class.java)
                startActivity(intent)
                dismiss()
            }

            myActivity.setOnClickListener {

                val intent = Intent(requireContext(), MyActivity::class.java)
                startActivity(intent)
                dismiss()
            }
            qrCode.setOnClickListener {

                val intent = Intent(requireContext(), QrCodeManager::class.java)
                startActivity(intent)
                dismiss()
            }

            return view
        }
    }
}

data class BlogPost(
    // Adding default value to avoid constructor issues
    val title: String,
    val content: String,
    val imageUrl: String,
    val postAs: String,
    var phoneNumber: String,
    var fullName: String = "",
    var date: String,
    var likes: Int,
    var dislikes: Int,
    var blogId: String,
) {
    constructor() : this("", "", "", "", "", "", "", 0, 0, "")
}

class BlogPostAdapter(
    private val context: Context,
    private var blogPosts: List<BlogPost>,
    private val userPhoneNumber: String,
    private val isLocalProfile: Boolean = false
) :
    RecyclerView.Adapter<BlogPostAdapter.ViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(context).inflate(R.layout.user_blog_card, parent, false)
        return ViewHolder(view)
    }

    override fun getItemCount(): Int {
        return blogPosts.size
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val post = blogPosts[position]
        // Bind data to views in your ViewHolder
        if (post.imageUrl.isNotEmpty()) {
            Glide.with(context)
                .load(post.imageUrl)
                .placeholder(ColorDrawable(Color.GRAY))
                .error(ColorDrawable(Color.GRAY))
                .diskCacheStrategy(DiskCacheStrategy.ALL) // Enable caching
                .into(holder.imageView)
        } else {
            holder.imageView.visibility = View.GONE // Set a default image if imageUrl is empty
        }
        holder.itemView.setOnClickListener {
            val intent = Intent(context, BlogDetails::class.java)
            // Convert the `in`.app.connect.bottomnav.Blog.BlogPost object to a JSON string
            val gson = Gson()
            val blogPostJson = gson.toJson(post)

            // Pass the JSON string as an extra in the intent
            intent.putExtra("blogPostJson", blogPostJson)
            context.startActivity(intent)
        }
        holder.titleTextView.text = post.title

        if (post.postAs == "Public")
            holder.typeIcon.setImageResource(R.drawable.ic_public)
        else
            holder.typeIcon.setImageResource(R.drawable.ic_private)
        holder.optionsIcon.setOnClickListener { view ->
            val popupMenu = PopupMenu(view.context, holder.optionsIcon)
            val inflater = popupMenu.menuInflater
            inflater.inflate(R.menu.blog_option_menu, popupMenu.menu)
            // Fetch the user's like/dislike status
            if (isLocalProfile) {
                if (userPhoneNumber != post.phoneNumber) {
                    popupMenu.menu.removeItem(R.id.menu_delete)
                }
            } else {
                if (userPhoneNumber == post.phoneNumber) {
                    popupMenu.menu.removeItem(R.id.menu_delete)
                }
            }

            val databaseReference = FirebaseDatabase.getInstance().reference
            val postPhoneNumber = post.phoneNumber
            val blogRef = databaseReference.child("blogs").child(postPhoneNumber).child(post.blogId)

            popupMenu.setOnMenuItemClickListener { item ->
                when (item.itemId) {
                    R.id.menu_delete -> {
                        // Ask for user confirmation
                        val builder = AlertDialog.Builder(context)
                        builder.setMessage("Are you sure you want to delete this blog post?")
                            .setPositiveButton("Yes") { dialog, id ->
                                deleteBlogPostAndImage(blogRef, post.imageUrl)
                            }
                            .setNegativeButton("No") { dialog, id ->
                                // User canceled the delete operation
                            }
                        val alertDialog = builder.create()
                        alertDialog.show()
                        true
                    }

                    R.id.menu_share -> {
                        FirebaseDynamicLinks.getInstance().createDynamicLink()
                            .setLink(Uri.parse("https://connect-app.in/app/blog/${post.blogId}/${post.phoneNumber}"))
                            .setDomainUriPrefix("https://connectblog.page.link") // Replace with your domain prefix
                            .setAndroidParameters(
                                DynamicLink.AndroidParameters.Builder("in.app.connect")
                                    .build()
                            ).setSocialMetaTagParameters(
                                DynamicLink.SocialMetaTagParameters.Builder()
                                    .setTitle(post.title)
                                    .setImageUrl(Uri.parse(post.imageUrl))
                                    .build()
                            )
                            .buildShortDynamicLink()
                            .addOnSuccessListener { result ->
                                val shortLink = result.shortLink
                                val shareIntent = Intent(Intent.ACTION_SEND)
                                shareIntent.type = "text/plain"
                                shareIntent.putExtra(
                                    Intent.EXTRA_SUBJECT,
                                    "Check out this blog post on Connect-App"
                                )
                                shareIntent.putExtra(
                                    Intent.EXTRA_TEXT,
                                    "$shortLink"
                                )
                                context.startActivity(
                                    Intent.createChooser(
                                        shareIntent,
                                        "Share this article"
                                    )
                                )
                            }
                            .addOnFailureListener { _ ->
                                Toast.makeText(
                                    context,
                                    "Please try again.",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }

                        true
                    }

                    else -> false
                }
            }


            popupMenu.show()
        }
    }

    private fun deleteBlogPostAndImage(blogRef: DatabaseReference, imageUrl: String) {
        if (imageUrl != null && imageUrl.isNotEmpty()) {
            // Assuming the image URL is stored in blogPost.imageUrl
            val storageReference = FirebaseStorage.getInstance().reference
            val uri = Uri.parse(imageUrl)
            val url = Uri.decode(uri.path)
            // Extract the path to the image
            val pathToImage = url.substring(url.indexOf("images/"))

            // Create a reference to the image using the image URL
            val imageReference = storageReference.child(pathToImage)

            // Delete the image from Firebase Storage
            imageReference.delete().addOnSuccessListener {
                // Image deleted successfully
                // Now, delete the blog post from the Realtime Database
                blogRef.removeValue().addOnSuccessListener {
                    // Blog post deleted successfully
                    Toast.makeText(context, "Deleted Successfully", Toast.LENGTH_SHORT)
                        .show()
                }.addOnFailureListener { exception ->
                    Toast.makeText(
                        context,
                        "Some Error Occurred, Please try again",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }.addOnFailureListener { exception ->
                // Handle the image deletion failure
                Toast.makeText(context, "Image Deletion Failed", Toast.LENGTH_SHORT).show()
            }
        } else {
            // If there is no image URL, skip image deletion and proceed with blog post deletion
            blogRef.removeValue().addOnSuccessListener {
                // Blog post deleted successfully
                Toast.makeText(context, "Deleted Successfully", Toast.LENGTH_SHORT).show()
            }.addOnFailureListener { exception ->
                Toast.makeText(
                    context,
                    "Some Error Occurred, Please try again",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val imageView: ImageView = itemView.findViewById(R.id.imageView)
        val optionsIcon: ImageView = itemView.findViewById(R.id.optionsIcon)
        val typeIcon: ImageView = itemView.findViewById(R.id.typeIcon)
        val titleTextView: TextView = itemView.findViewById(R.id.titleTextView)
    }

    // Method to update the data and notify the adapter
    fun updateData(newData: MutableList<BlogPost>) {
        blogPosts = newData
        notifyDataSetChanged()
    }
}


