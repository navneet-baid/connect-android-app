package `in`.app.connect.bottomnav.Blog

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.net.Uri
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.PopupMenu
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
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
import `in`.app.connect.R
import `in`.app.connect.bottomnav.userprofile.ConnectedConnectionsActivity
import `in`.app.connect.utils.SessionManager
import java.util.HashMap

class FollowingFragment : Fragment() {


    private lateinit var database: DatabaseReference
    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: BlogPostAdapter
    lateinit var sessionManager: SessionManager
    private var userDetails: HashMap<String, Any> = HashMap()
    private val connectedConnectionsList =
        mutableListOf<String>()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val rootView = inflater.inflate(R.layout.fragment_following, container, false)

        recyclerView = rootView.findViewById(R.id.recyclerViewItems)
        sessionManager = SessionManager(requireContext())
        userDetails = sessionManager.getUserDetailFromSession()
        val layoutManager =
            LinearLayoutManager(requireContext(), LinearLayoutManager.VERTICAL, false)

        recyclerView.layoutManager = layoutManager
        fetchConnectedConnections(userDetails[sessionManager.KEY_PHONENUMBER].toString())
        adapter = BlogPostAdapter(
            requireContext(),
            mutableListOf(),
            userDetails[sessionManager.KEY_PHONENUMBER].toString()
        )
        recyclerView.adapter = adapter
        // Adjust the database reference to include user information


        return rootView
    }

    private var blogsValueEventListener: ValueEventListener? = null

    private fun fetchBlogsForConnectedUsers() {
        FirebaseDatabase.getInstance().reference.child("blogs").addValueEventListener( object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                val posts = mutableListOf<BlogPost>()
                for (connectedPhoneNumber in connectedConnectionsList) {
                    println(connectedPhoneNumber)
                    val userSnapshot = dataSnapshot.child(connectedPhoneNumber)
                    println(userSnapshot)
                    for (uniqueKeySnapshot in userSnapshot.children) {
                        val post = uniqueKeySnapshot.getValue(BlogPost::class.java)
                        post?.let {
                            it.phoneNumber = connectedPhoneNumber
                            it.blogId = uniqueKeySnapshot.key ?: ""
                            posts.add(it)
                        }
                    }
                }
                adapter.updateData(posts)
            }

            override fun onCancelled(error: DatabaseError) {
                // Handle error
            }
        })

    }

    private fun fetchConnectedConnections(userPhoneNumber: String) {
        val connectionsRef: DatabaseReference =
            FirebaseDatabase.getInstance().reference.child("Connections")
        connectionsRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                var count=0
                for (snapshot in dataSnapshot.children) {
                    count++
                    val sender = snapshot.child("sender").value.toString()
                    val receiver = snapshot.child("receiver").value.toString()
                    val status = snapshot.child("status").value.toString()
                    if ((sender == userPhoneNumber || receiver == userPhoneNumber) && status == "connected") {
                        if (sender == userPhoneNumber) {
                            val phoneNumber = snapshot.child("receiver").value.toString()
                            connectedConnectionsList.add(phoneNumber)
                        } else {
                            val phoneNumber = snapshot.child("sender").value.toString()
                            connectedConnectionsList.add(phoneNumber)
                        }
                    }
                    println(count)
                    println(dataSnapshot.childrenCount)
                    if (dataSnapshot.childrenCount.toInt() == count) {
                        fetchBlogsForConnectedUsers()
                    }

                }

            }

            override fun onCancelled(databaseError: DatabaseError) {
                println("Error fetching connected connection count: ${databaseError.message}")
            }
        })
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
        private val userPhoneNumber: String
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
                if (userPhoneNumber != post.phoneNumber) {
                    popupMenu.menu.removeItem(R.id.menu_delete)
                }
                // Fetch the user's like/dislike status
                val databaseReference = FirebaseDatabase.getInstance().reference
                val postPhoneNumber = post.phoneNumber
                val blogRef =
                    databaseReference.child("blogs").child(postPhoneNumber).child(post.blogId)

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

}
