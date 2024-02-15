package `in`.app.connect.bottomnav.Blog

import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.PopupMenu
import android.widget.ProgressBar
import android.widget.RelativeLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.dynamiclinks.DynamicLink
import com.google.firebase.dynamiclinks.FirebaseDynamicLinks
import com.google.firebase.storage.FirebaseStorage
import com.google.gson.Gson
import `in`.app.connect.MainActivity
import `in`.app.connect.PopupViewProfile
import `in`.app.connect.R
import `in`.app.connect.bottomnav.userprofile.BlogPost
import `in`.app.connect.utils.ConnectAppApplication
import `in`.app.connect.utils.SessionManager
import org.w3c.dom.Text
import java.text.SimpleDateFormat
import java.util.Date
import java.util.HashMap
import java.util.concurrent.TimeUnit

class BlogDetails : AppCompatActivity() {
    lateinit var sessionManager: SessionManager
    lateinit var progressBar: ProgressBar
    private var userDetails: HashMap<String, Any> = HashMap()
    private var liked = false
    private var disliked = false
    private var openedFromDynamicLink = false // Flag to indicate dynamic link opening
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        (application as ConnectAppApplication).startAppUsageTracking()

        setContentView(R.layout.activity_blog_details)
        progressBar = findViewById(R.id.progressBar)
        progressBar.visibility = View.VISIBLE
        val backArrow = findViewById<ImageView>(R.id.back_arrow)
        backArrow.setOnClickListener {
            navigateBack()
        }
        hideOtherLayoutElements()
        // Initialize Firebase
        val database = FirebaseDatabase.getInstance()
        var blogPostJson = ""
        val data = intent.data
        if (data != null) {
            openedFromDynamicLink = true
            val pathSegments = data.pathSegments
            if (pathSegments.size >= 4) { // Check for index out of bounds
                val phoneNumber = pathSegments[3] ?: "" // Safe access
                val blogId = pathSegments[2] ?: "" // Safe access
                val dataRef: DatabaseReference = database.reference
                val blogsRef = dataRef.child("blogs")
                    .child(phoneNumber)
                    .child(blogId)

                // Add a ValueEventListener to fetch the data
                blogsRef.addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onDataChange(dataSnapshot: DataSnapshot) {
                        // Handle the data once it's fetched
                        val post = dataSnapshot.getValue(BlogPost::class.java)
                        post?.let {
                            // Update the UI with the fetched data
                            it.phoneNumber = phoneNumber
                            it.blogId = blogId
                            updateUIWithData(it)
                        }
                    }

                    override fun onCancelled(databaseError: DatabaseError) {
                        // Handle any errors
                        println("Error fetching data: ${databaseError.message}")
                    }
                })
            }
        } else {
            // Retrieve the JSON string from the intent
            blogPostJson = intent.getStringExtra("blogPostJson") ?: ""
            if (blogPostJson.isNotEmpty()) {
                // Convert the JSON string back to a `in`.app.connect.bottomnav.Blog.BlogPost object using Gson
                val gson = Gson()
                val blogPost = gson.fromJson(blogPostJson, BlogPost::class.java)
                updateUIWithData(blogPost)
            }
        }
    }

    private fun navigateBack() {
        if (openedFromDynamicLink) {
            // If opened through a dynamic link, navigate to AllBlogs screen
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
        } else {
            // If opened from the previous screen, navigate back
            super.onBackPressed()
        }
    }

    private lateinit var usersRef: DatabaseReference

    private fun fetchIsDislikesEnable(phoneNumber: String) {
        // Get the reference to the Firebase Realtime Database
        val database: FirebaseDatabase = FirebaseDatabase.getInstance()
        usersRef = database.getReference("Users")
        // Fetch the isDislikesEnable value from Firebase

        usersRef.child(phoneNumber).child("isDislikesEnable")
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(dataSnapshot: DataSnapshot) {
                    if (dataSnapshot.exists()) {
                        val isDislikesEnable = dataSnapshot.getValue(Boolean::class.java) ?: false
                        val dislikesCount = findViewById<TextView>(R.id.dislikesCount)
                        if (!isDislikesEnable) {
                            dislikesCount.visibility = View.VISIBLE
                        } else {
                            dislikesCount.visibility = View.GONE
                        }

                    } else {
                        // Handle the case where data doesn't exist
                    }
                }

                override fun onCancelled(databaseError: DatabaseError) {
                    // Handle database errors
                }
            })
    }

    private fun updateUIWithData(blogPost: BlogPost) {
        showOtherLayoutElements()
        progressBar.visibility = View.GONE
        // Now you have the entire `in`.app.connect.bottomnav.Blog.BlogPost object to display its details
        val blogTitle = findViewById<TextView>(R.id.blogTitle)
        val blogImage = findViewById<ImageView>(R.id.blogImage)
        val typeIcon = findViewById<ImageView>(R.id.typeIcon)
        val optionsIcon = findViewById<ImageView>(R.id.optionsIcon)
        val authorName = findViewById<TextView>(R.id.authorName)
        val blogDate = findViewById<TextView>(R.id.blogDate)
        val blogContent = findViewById<TextView>(R.id.blogContent)
        val likesCount = findViewById<TextView>(R.id.likesCount)
        val dislikesCount = findViewById<TextView>(R.id.dislikesCount)
        val likeIcon = findViewById<ImageView>(R.id.likeIcon)
        val dislikeIcon = findViewById<ImageView>(R.id.dislikeIcon)


        sessionManager = SessionManager(this@BlogDetails)
        userDetails = sessionManager.getUserDetailFromSession()
        fetchIsDislikesEnable(blogPost.phoneNumber)
        if (blogPost.postAs == "Public") {
            typeIcon.setImageResource(R.drawable.ic_public)
            authorName.setOnClickListener {
                val intent = Intent(this@BlogDetails, PopupViewProfile::class.java)
                intent.putExtra("phoneNumber", blogPost.phoneNumber)
                startActivity(intent)
            }
        } else {
            typeIcon.setImageResource(R.drawable.ic_private)
            authorName.visibility = View.GONE
        }

        if (blogPost.imageUrl.isNotEmpty()) {
            Glide.with(this@BlogDetails).load(blogPost.imageUrl)
                .placeholder(ColorDrawable(Color.GRAY)).error(ColorDrawable(Color.GRAY))
                .diskCacheStrategy(DiskCacheStrategy.ALL) // Enable caching
                .into(blogImage)
        } else {
            blogImage.visibility = View.GONE // Set a default image if imageUrl is empty
        }
        // Fetch the user's like/dislike status
        val databaseReference = FirebaseDatabase.getInstance().reference
        val userPhoneNumber = userDetails[sessionManager.KEY_PHONENUMBER].toString()
        val postPhoneNumber = blogPost.phoneNumber
        val blogRef = databaseReference.child("blogs").child(postPhoneNumber).child(blogPost.blogId)

        optionsIcon.setOnClickListener { view ->
            val popupMenu = PopupMenu(view.context, optionsIcon)
            val inflater = popupMenu.menuInflater
            inflater.inflate(R.menu.blog_option_menu, popupMenu.menu)

            if (userDetails[sessionManager.KEY_PHONENUMBER].toString() != blogPost.phoneNumber) {
                popupMenu.menu.removeItem(R.id.menu_delete)
            }

            popupMenu.setOnMenuItemClickListener { item ->
                when (item.itemId) {
                    R.id.menu_delete -> {
                        // Ask for user confirmation
                        val builder = AlertDialog.Builder(this)
                        builder.setMessage("Are you sure you want to delete this blog post?")
                            .setPositiveButton("Yes") { dialog, id ->
                                deleteBlogPostAndImage(blogRef, blogPost.imageUrl)
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
                            .setLink(Uri.parse("https://connect-app.in/app/blog/${blogPost.blogId}/${blogPost.phoneNumber}"))
                            .setDomainUriPrefix("https://connectblog.page.link") // Replace with your domain prefix
                            .setAndroidParameters(
                                DynamicLink.AndroidParameters.Builder("in.app.connect")
                                    .build()
                            ).setSocialMetaTagParameters(
                                DynamicLink.SocialMetaTagParameters.Builder()
                                    .setTitle(blogPost.title)
                                    .setImageUrl(Uri.parse(blogPost.imageUrl))
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
                                startActivity(
                                    Intent.createChooser(
                                        shareIntent,
                                        "Share this article"
                                    )
                                )
                            }
                            .addOnFailureListener { _ ->
                                Toast.makeText(
                                    this@BlogDetails,
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
        likesCount.text = blogPost.likes.toString()
        dislikesCount.text = blogPost.dislikes.toString()

        blogDate.text = formatElapsedTime(blogPost.date)
        blogTitle.text = blogPost.title
        authorName.text = blogPost.fullName
        blogContent.text = blogPost.content



        blogRef.child("userLikes").child(userPhoneNumber).get()
            .addOnSuccessListener { dataSnapshot ->
                if (dataSnapshot.exists()) {
                    // The user has already liked this post
                    liked = true
                    likeIcon.setImageResource(R.drawable.ic_like_fill)
                }
            }.addOnFailureListener { exception ->
                // Handle any errors or set liked to false by default
                liked = false
                likeIcon.setImageResource(R.drawable.ic_like)
            }

        blogRef.child("userDislikes").child(userPhoneNumber).get()
            .addOnSuccessListener { dataSnapshot ->
                if (dataSnapshot.exists()) {
                    // The user has already disliked this post
                    disliked = true
                    dislikeIcon.setImageResource(R.drawable.ic_dislike_fill)
                }
            }.addOnFailureListener { exception ->
                // Handle any errors or set disliked to false by default
                disliked = false
                dislikeIcon.setImageResource(R.drawable.ic_dislike)
            }
        // Set up a ValueEventListener to listen for changes in likes count
        val likesCountListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                // Update the likes count based on the snapshot data
                val newLikesCount = snapshot.getValue(Int::class.java) ?: 0
                blogPost.likes = newLikesCount
                likesCount.text = newLikesCount.toString()
            }

            override fun onCancelled(error: DatabaseError) {
                // Handle errors
            }
        }

        // Set up a ValueEventListener to listen for changes in dislikes count
        val dislikesCountListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                // Update the dislikes count based on the snapshot data
                val newDislikesCount = snapshot.getValue(Int::class.java) ?: 0
                blogPost.dislikes = newDislikesCount
                dislikesCount.text = newDislikesCount.toString()
            }

            override fun onCancelled(error: DatabaseError) {
                // Handle errors
            }
        }


        val likesRef = blogRef.child("likes")
        likesRef.addValueEventListener(likesCountListener)

        val dislikesRef = blogRef.child("dislikes")
        dislikesRef.addValueEventListener(dislikesCountListener)

        likeIcon.setOnClickListener {
            val userLikesRef = blogRef.child("userLikes").child(userPhoneNumber)
            val userDislikesRef = blogRef.child("userDislikes").child(userPhoneNumber)

            if (disliked) {
                // User is switching from dislike to like
                userDislikesRef.removeValue()
                userLikesRef.setValue(true)

                // Decrease dislike count and update image
                blogPost.dislikes--
                dislikesCount.text = blogPost.dislikes.toString()
                dislikeIcon.setImageResource(R.drawable.ic_dislike)

                // Increase like count and update image
                blogPost.likes++
                likesCount.text = blogPost.likes.toString()
                likeIcon.setImageResource(R.drawable.ic_like_fill)

                disliked = false
                liked = true
            } else if (!liked) {
                // User is liking for the first time
                userLikesRef.setValue(true)

                // Increase like count and update image
                blogPost.likes++
                likesCount.text = blogPost.likes.toString()
                likeIcon.setImageResource(R.drawable.ic_like_fill)

                liked = true
            } else {
                // User is unliking
                userLikesRef.removeValue()

                // Decrease like count and update image
                blogPost.likes--
                likesCount.text = blogPost.likes.toString()
                likeIcon.setImageResource(R.drawable.ic_like)
                liked = false
            }

            // Update the likes and dislikes in the database
            blogRef.child("likes").setValue(blogPost.likes)
            blogRef.child("dislikes").setValue(blogPost.dislikes)
        }

        dislikeIcon.setOnClickListener {
            val userLikesRef = blogRef.child("userLikes").child(userPhoneNumber)
            val userDislikesRef = blogRef.child("userDislikes").child(userPhoneNumber)

            if (liked) {
                // User is switching from like to dislike
                userLikesRef.removeValue()
                userDislikesRef.setValue(true)

                // Decrease like count and update image
                blogPost.likes--
                likesCount.text = blogPost.likes.toString()
                likeIcon.setImageResource(R.drawable.ic_like)

                // Increase dislike count and update image
                blogPost.dislikes++
                dislikesCount.text = blogPost.dislikes.toString()
                dislikeIcon.setImageResource(R.drawable.ic_dislike_fill)

                liked = false
                disliked = true
            } else if (!disliked) {
                // User is disliking for the first time
                userDislikesRef.setValue(true)

                // Increase dislike count and update image
                blogPost.dislikes++
                dislikesCount.text = blogPost.dislikes.toString()
                dislikeIcon.setImageResource(R.drawable.ic_dislike_fill)

                disliked = true
            } else {
                // User is undoing their dislike
                userDislikesRef.removeValue()

                // Decrease dislike count and update image
                blogPost.dislikes--
                dislikesCount.text = blogPost.dislikes.toString()
                dislikeIcon.setImageResource(R.drawable.ic_dislike)

                disliked = false
            }

            // Update the likes and dislikes in the database
            blogRef.child("likes").setValue(blogPost.likes)
            blogRef.child("dislikes").setValue(blogPost.dislikes)
        }

    }

    private fun deleteBlogPostAndImage(blogRef: DatabaseReference, imageUrl: String) {
        if (imageUrl != null && imageUrl.isNotEmpty()) {
            // Assuming the image URL is stored in blogPost.imageUrl
            val storageReference = FirebaseStorage.getInstance().reference
            val uri = Uri.parse(imageUrl)
            val url = Uri.decode(uri.path)
            println(url)
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
                    Toast.makeText(this@BlogDetails, "Deleted Successfully", Toast.LENGTH_SHORT)
                        .show()
                    finish()
                }.addOnFailureListener { exception ->
                    Toast.makeText(
                        this@BlogDetails,
                        "Some Error Occurred, Please try again",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }.addOnFailureListener { exception ->
                // Handle the image deletion failure
                Toast.makeText(this@BlogDetails, "Image Deletion Failed", Toast.LENGTH_SHORT).show()
            }
        } else {
            // If there is no image URL, skip image deletion and proceed with blog post deletion
            blogRef.removeValue().addOnSuccessListener {
                // Blog post deleted successfully
                Toast.makeText(this@BlogDetails, "Deleted Successfully", Toast.LENGTH_SHORT).show()
                finish()
            }.addOnFailureListener { exception ->
                Toast.makeText(
                    this@BlogDetails,
                    "Some Error Occurred, Please try again",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    fun formatElapsedTime(date: String): String {
        val dateFormat = SimpleDateFormat("dd-MM-yy HH:mm")
        val currentDate = Date()

        try {
            val parsedDate = dateFormat.parse(date)

            if (parsedDate != null) {
                val elapsedTimeMillis = currentDate.time - parsedDate.time

                val seconds = TimeUnit.MILLISECONDS.toSeconds(elapsedTimeMillis)
                val minutes = TimeUnit.MILLISECONDS.toMinutes(elapsedTimeMillis)
                val hours = TimeUnit.MILLISECONDS.toHours(elapsedTimeMillis)
                val days = TimeUnit.MILLISECONDS.toDays(elapsedTimeMillis)
                val weeks = days / 7
                val months = days / 30
                val years = days / 365

                return when {
                    seconds < 60 -> "Just now"
                    minutes < 2 -> "1 min ago"
                    minutes < 60 -> "$minutes mins ago"
                    hours < 2 -> "1 hour ago"
                    hours < 24 -> "$hours hours ago"
                    days < 2 -> "yesterday"
                    days < 7 -> "$days days ago"
                    weeks < 2 -> "1 week ago"
                    weeks < 4 -> "$weeks weeks ago"
                    months < 2 -> "1 month ago"
                    months < 12 -> "$months months ago"
                    years < 2 -> "1 year ago"
                    else -> "$years years ago"
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        // Return an empty string if there was an error
        return ""
    }

    private fun hideOtherLayoutElements() {
        // Hide other layout elements (e.g., TextViews, ImageView, etc.)
        val otherLayoutElements = findViewById<RelativeLayout>(R.id.mainLayout)
        otherLayoutElements.visibility = View.GONE
    }

    private fun showOtherLayoutElements() {
        // Show other layout elements
        val otherLayoutElements = findViewById<RelativeLayout>(R.id.mainLayout)
        otherLayoutElements.visibility = View.VISIBLE
    }

    override fun onDestroy() {
        super.onDestroy()
        (application as ConnectAppApplication).stopAppUsageTracking()
    }
}