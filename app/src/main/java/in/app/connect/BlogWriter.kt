package `in`.app.connect

import android.app.Activity
import android.app.AlertDialog
import android.content.DialogInterface
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.text.Editable
import android.text.TextWatcher
import androidx.appcompat.app.AppCompatActivity
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.Toast
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.storage.FirebaseStorage
import `in`.app.connect.utils.ConnectAppApplication
import `in`.app.connect.utils.SessionManager
import java.text.SimpleDateFormat
import java.util.Date
import java.util.HashMap

class BlogWriter : AppCompatActivity(), PostOptionsDialog.PostOptionListener {
    private lateinit var auth: FirebaseAuth
    private lateinit var storage: FirebaseStorage
    private lateinit var backArrow: ImageView
    private lateinit var database: FirebaseDatabase
    private var selectedImageUri: Uri? = null
    private lateinit var addImageButton: ImageView
    private lateinit var deleteImageIcon: ImageView
    private lateinit var titleEditText: TextInputEditText
    private lateinit var contentEditText: TextInputEditText
    private lateinit var imagePreview: ImageView
    private lateinit var publishButton: MaterialButton
    private lateinit var progressBar: ProgressBar

    lateinit var sessionManager: SessionManager
    private var userDetails: HashMap<String, Any> = HashMap()
    private var isDirty = false // Flag to track unsaved changes
    private var isImageDirty = false // Flag to track image changes

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        (application as ConnectAppApplication).startAppUsageTracking()

        setContentView(R.layout.activity_blog_writer)

        auth = FirebaseAuth.getInstance()
        storage = FirebaseStorage.getInstance()
        database = FirebaseDatabase.getInstance()

        addImageButton = findViewById(R.id.addImageButton)
        backArrow = findViewById(R.id.backArrow)
        deleteImageIcon = findViewById(R.id.deleteImageIcon)

        titleEditText = findViewById(R.id.titleEditText)
        contentEditText = findViewById(R.id.contentEditText)
        imagePreview = findViewById(R.id.imagePreview)
        publishButton = findViewById(R.id.publishButton)
        progressBar = findViewById(R.id.progressBar)
        sessionManager = SessionManager(this@BlogWriter)
        userDetails = sessionManager.getUserDetailFromSession()

        addImageButton.setOnClickListener {
            val imagePickerIntent =
                Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
            startActivityForResult(imagePickerIntent, IMAGE_PICK_REQUEST_CODE)
        }
        deleteImageIcon.setOnClickListener {
            selectedImageUri = null
            imagePreview.visibility = ImageView.GONE
            deleteImageIcon.visibility = ImageView.GONE
            isImageDirty = false
        }
        publishButton.setOnClickListener {
            showPostOptionsDialog()
        }
        backArrow.setOnClickListener {
            onBackPressed()
        }
        // Add text change listeners to your input fields to track changes
        titleEditText.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                isDirty = true
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
                // Do nothing
            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                // Do nothing
            }
        })

        contentEditText.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                isDirty = true
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
                // Do nothing
            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                // Do nothing
            }
        })
    }

    companion object {
        private const val IMAGE_PICK_REQUEST_CODE = 1
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == IMAGE_PICK_REQUEST_CODE && resultCode == Activity.RESULT_OK) {
            selectedImageUri = data?.data

            val imagePreview: ImageView = findViewById(R.id.imagePreview)
            imagePreview.setImageURI(selectedImageUri)
            imagePreview.visibility = ImageView.VISIBLE
            deleteImageIcon.visibility = ImageView.VISIBLE
            isImageDirty = true
        }
    }

    val dialog = PostOptionsDialog()
    private fun showPostOptionsDialog() {
        dialog.show(supportFragmentManager, "post_options_dialog")
    }

    var postAs = ""
    override fun onOptionSelected(option: String) {
        dialog.dismiss()
        postAs = option
        val title = titleEditText.text.toString()
        val content = contentEditText.text.toString()

        if (title.isNotEmpty() && content.isNotEmpty() && postAs.isNotEmpty()) {
            val phoneNumber = userDetails[sessionManager.KEY_PHONENUMBER].toString()
            val fullName = userDetails[sessionManager.KEY_FULLNAME].toString()

            // Disable UI elements during upload
            publishButton.isEnabled = false
            progressBar.visibility = ProgressBar.VISIBLE

            // Get the current date and time
            val currentDate = SimpleDateFormat("dd-MM-yy HH:mm").format(Date())

            val blogData = BlogEntry(title, content, postAs, fullName, currentDate)
            val databaseRef = database.reference.child("blogs").child(phoneNumber).push()
            databaseRef.setValue(blogData)
                .addOnSuccessListener {
                    val blogId = databaseRef.key

                    selectedImageUri?.let { uri ->
                        val storageRef =
                            storage.reference.child("images/${auth.currentUser?.uid}/$blogId.jpg")

                        storageRef.putFile(uri)
                            .addOnProgressListener { taskSnapshot ->
                                // Calculate the upload progress percentage
                                val progress =
                                    (100.0 * taskSnapshot.bytesTransferred / taskSnapshot.totalByteCount).toInt()

                                // Update the progress bar
                                progressBar.progress = progress
                            }
                            .addOnSuccessListener {
                                storageRef.downloadUrl.addOnSuccessListener { imageUrl ->
                                    val imageUrlString = imageUrl.toString()
                                    databaseRef.child("imageUrl").setValue(imageUrlString)
                                        .addOnSuccessListener {
                                            Toast.makeText(
                                                this,
                                                "Blog posted successfully!",
                                                Toast.LENGTH_SHORT
                                            ).show()
                                            progressBar.visibility = ProgressBar.GONE
                                            finish()
                                        }
                                        .addOnFailureListener { e ->
                                            Toast.makeText(
                                                this,
                                                "Failed to upload image URL",
                                                Toast.LENGTH_SHORT
                                            ).show()
                                            // Re-enable UI elements in case of failure
                                            publishButton.isEnabled = true
                                            progressBar.visibility = ProgressBar.GONE
                                        }
                                }
                            }
                            .addOnFailureListener { e ->
                                Toast.makeText(this, "Failed to upload image", Toast.LENGTH_SHORT)
                                    .show()
                                // Re-enable UI elements in case of failure
                                publishButton.isEnabled = true
                                progressBar.visibility = ProgressBar.GONE
                            }
                    } ?: run {
                        finish()
                    }
                }
                .addOnFailureListener { e ->
                    Toast.makeText(this, "Failed to post blog content", Toast.LENGTH_SHORT).show()
                    // Re-enable UI elements in case of failure
                    publishButton.isEnabled = true
                    progressBar.visibility = ProgressBar.GONE
                }
        } else {
            Toast.makeText(this, "Title and content cannot be empty", Toast.LENGTH_SHORT).show()
        }
    }

    data class BlogEntry(
        val title: String,
        val content: String,
        val postAs: String,
        val fullName: String,
        val date: String,
        val likes: Int = 0,
        val dislikes: Int = 0
    )

    override fun onBackPressed() {
        if (isDirty || isImageDirty) {
            // Unsaved changes detected, show a confirmation dialog
            showDiscardChangesConfirmationDialog()
        } else {
            super.onBackPressed()
        }
    }

    private fun showDiscardChangesConfirmationDialog() {
        AlertDialog.Builder(this)
            .setTitle("Discard Changes")
            .setMessage("Are you sure you want to go back and discard the post?")
            .setPositiveButton("Yes", DialogInterface.OnClickListener { dialog, which ->
                // User confirmed, go back
                finish()
            })
            .setNegativeButton("No", DialogInterface.OnClickListener { dialog, which ->
                // User canceled, do nothing
            })
            .show()
    }

    override fun onDestroy() {
        super.onDestroy()
        (application as ConnectAppApplication).stopAppUsageTracking()
    }
}
