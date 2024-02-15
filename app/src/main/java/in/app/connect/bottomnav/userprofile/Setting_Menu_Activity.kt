package `in`.app.connect.bottomnav.userprofile

import android.app.ProgressDialog
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.view.View
import android.widget.EditText
import android.widget.ImageView
import android.widget.RelativeLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import `in`.app.connect.R
import `in`.app.connect.utils.SessionManager
import com.bumptech.glide.Glide // Import Glide library
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.google.android.material.slider.Slider
import com.google.android.material.textfield.TextInputLayout
import com.google.firebase.storage.FirebaseStorage
import `in`.app.connect.utils.ConnectAppApplication
import java.io.IOException
import java.util.HashMap

class Setting_Menu_Activity : AppCompatActivity(), EditAboutInfo.UpdateListener {


    private lateinit var editTextBio: EditText
    private lateinit var bio: TextInputLayout
    private lateinit var backButton: ImageView
    private lateinit var rangeSlider: Slider
    private lateinit var aboutEdit: ImageView
    private lateinit var checkTick: ImageView
    private lateinit var distanceSelected: TextView
    private lateinit var genderTextView: TextView
    private lateinit var lookingForTextView: TextView
    private lateinit var hometownTextView: TextView
    private lateinit var locationTextView: TextView
    private lateinit var sessionManager: SessionManager
    private var userDetails: HashMap<String, Any> = HashMap()
    private lateinit var database: FirebaseDatabase
    private lateinit var reference: DatabaseReference
    private lateinit var profileImage: ImageView // Add this line
    private lateinit var changeProfileLayout: RelativeLayout // Add this line
    val PICK_IMAGE = 1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        (application as ConnectAppApplication).startAppUsageTracking()

        setContentView(R.layout.activity_setting_menu)

        // Initialize UI elements
        editTextBio = findViewById(R.id.editTextBio)
        changeProfileLayout = findViewById(R.id.changeProfileLayout)
        bio = findViewById(R.id.bio)
        rangeSlider = findViewById(R.id.rangeSlider)
        distanceSelected = findViewById(R.id.distanceSelected)
        genderTextView = findViewById(R.id.genderTextView)
        hometownTextView = findViewById(R.id.hometownTextView)
        locationTextView = findViewById(R.id.locationTextView)
        lookingForTextView = findViewById(R.id.lookingForTextView)
        backButton = findViewById(R.id.backButton)
        aboutEdit = findViewById(R.id.aboutEdit)
        checkTick = findViewById(R.id.checkTick)
        profileImage = findViewById(R.id.profileImage) // Initialize the ImageView

        sessionManager = SessionManager(this@Setting_Menu_Activity)
        userDetails = sessionManager.getUserDetailFromSession()

        database = FirebaseDatabase.getInstance()
        reference = database.reference.child("Users").child(userDetails["phoneNumber"] as String)
        checkTick.visibility = View.GONE
        backButton.setOnClickListener {
            onBackPressed()
        }
        changeProfileLayout.setOnClickListener {
            // Open the gallery
            val galleryIntent = Intent(
                Intent.ACTION_PICK,
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI
            )
            startActivityForResult(galleryIntent, PICK_IMAGE)
        }
        //edit info
        aboutEdit.setOnClickListener {
            val bottomSheetFragment = EditAboutInfo()
            bottomSheetFragment.setUpdateListener(this@Setting_Menu_Activity)
            val bundle = Bundle()
            bundle.putString("gender", genderTextView.text.toString())
            bundle.putString("location", locationTextView.text.toString())
            bundle.putString("hometown", hometownTextView.text.toString())
            bundle.putString("lookingFor", lookingForTextView.text.toString())
            bottomSheetFragment.arguments = bundle
            bottomSheetFragment.show(supportFragmentManager, bottomSheetFragment.tag)
        }


        editTextBio.setOnClickListener {
            val currentBio = editTextBio.text.toString()
            val bioDialogFragment = BioBottomDialogFragment()
            val bundle = Bundle()
            bundle.putString("bio", currentBio)
            bioDialogFragment.arguments = bundle

            bioDialogFragment.setBioUpdateListener(object :
                BioBottomDialogFragment.BioUpdateListener {
                override fun onUpdateBio(newBio: String) {
                    val userUpdateData: HashMap<String, Any?> = HashMap()
                    userUpdateData["bio"] = newBio
                    reference.updateChildren(userUpdateData)
                        .addOnCompleteListener { task ->
                            if (task.isSuccessful) {
                                editTextBio.setText(newBio)
                            } else {
                                Toast.makeText(
                                    this@Setting_Menu_Activity,
                                    "Bio Update Fail",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        }
                }
            })

            bioDialogFragment.show(supportFragmentManager, bioDialogFragment.tag)

        }


        // Fetch and display user information
        reference.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val userData = snapshot.value as? Map<String, Any>
                val bio = userData?.get("bio") as? String
                val distance = userData?.get("distance")
                val gender = userData?.get("gender") as? String
                val lookingFor = userData?.get("lookingFor") as? String
                val homeTown = userData?.get("hometown") as? String
                val location = userData?.get("location") as? String
                // Fetch the images
                val images = userData?.get("images") as? List<String>
                // Assuming you have an ImageView with the ID 'imageView'
                if (!images.isNullOrEmpty()) {
                    val imageUrl = images[0] // Assuming you want to display the first image
                    loadImageWithGlide(imageUrl)
                }
                // Update frontend UI with the fetched information
                editTextBio.setText(bio)
                rangeSlider.value = distance!!.toString().toFloat()
                distanceSelected.text = "$distance KM"
                genderTextView.text = gender
                lookingForTextView.text = lookingFor
                hometownTextView.text = homeTown
                locationTextView.text = location
            }

            override fun onCancelled(error: DatabaseError) {
                // Handle error
            }
        })
        // Add a value change listener to the RangeSlider
        rangeSlider.addOnChangeListener { slider, value, fromUser ->
            // Convert the float value to a string
            val distance = value.toInt()
            distanceSelected.text = "$distance KM"
            // Update the "distance" field in the Firebase database
            reference.child("distance").setValue(distance)
                .addOnCompleteListener { task ->
                    if (!task.isSuccessful) {
                        println("Error")
                    }
                }
        }
    }


    private fun loadImageWithGlide(imageUrl: String?) {
        println(imageUrl)
        try {
            Glide.with(this)
                .load(imageUrl)
                .diskCacheStrategy(DiskCacheStrategy.ALL) // Enable caching
                .into(profileImage)

        } catch (e: Exception) {

            println(e.message)
        }

    }

    override fun onUpdate(
        gender: String?,
        lookingFor: String?,
        location: String?,
        hometown: String?,

        ) {
        val userUpdateData: HashMap<String, Any?> = HashMap()
        userUpdateData["gender"] = gender
        userUpdateData["lookingFor"] = lookingFor
        userUpdateData["location"] = location
        userUpdateData["hometown"] = hometown

        reference.updateChildren(userUpdateData)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    Toast.makeText(
                        this@Setting_Menu_Activity,
                        "Profile Updated successfully",
                        Toast.LENGTH_SHORT
                    ).show()
                    genderTextView.text = gender
                    lookingForTextView.text = lookingFor
                    hometownTextView.text = hometown
                    locationTextView.text = location
                    sessionManager.updateLocationAndHometown(location!!, hometown!!)
                } else {
                    Toast.makeText(
                        this@Setting_Menu_Activity,
                        "Profile Updated Fail",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == PICK_IMAGE && resultCode == RESULT_OK) {
            val imageUri = data?.data
            try {
                // Get the selected image and update the CircleImageView
                val bitmap: Bitmap =
                    MediaStore.Images.Media.getBitmap(this.contentResolver, imageUri)
                profileImage.setImageBitmap(bitmap)
                checkTick.visibility = View.VISIBLE
                // Set an OnClickListener for the checkTick button
                checkTick.setOnClickListener {
                    updateProfileImage(imageUri)
                }
                // You can also save the image URI or bitmap to Firebase or elsewhere for later use.
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }
    }

    private fun updateProfileImage(imageUri: Uri?) {
        checkTick.setOnClickListener { null }

        val progressDialog = ProgressDialog(this)
        progressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL)
        progressDialog.max = 100
        progressDialog.setCancelable(false)
        progressDialog.setTitle("Uploading Image")
        progressDialog.setMessage("Please wait...")
        progressDialog.show()

        checkTick.setOnClickListener { null }
        val phoneNumber =
            userDetails[sessionManager.KEY_PHONENUMBER] // Replace with the user's actual phone number
        val storageReference = FirebaseStorage.getInstance().reference

        // Create a reference to the user's profile images folder in Firebase Storage
        val profileImagesRef = storageReference.child("RecentPicture/$phoneNumber")
        // Delete all existing photos from the profileImagesRef
        profileImagesRef.listAll()
            .addOnSuccessListener { items ->
                // Delete each item (photo) from the folder
                println(items)
                for (item in items.items) {
                    item.delete()
                        .addOnSuccessListener {
                            // Upload the new image to Firebase Storage
                            if (imageUri != null) {
                                val uploadTask =
                                    profileImagesRef.child(phoneNumber.toString()).putFile(imageUri)
                                uploadTask.addOnProgressListener { taskSnapshot ->
                                    // Calculate the upload progress percentage
                                    val progress =
                                        (100.0 * taskSnapshot.bytesTransferred / taskSnapshot.totalByteCount).toInt()
                                    progressDialog.progress = progress
                                }
                                    .addOnSuccessListener { taskSnapshot ->
                                        progressDialog.dismiss()
                                        // Get the download URL of the new image
                                        taskSnapshot.storage.downloadUrl.addOnSuccessListener { uri ->
                                            val newImageUrl = uri.toString()
                                            reference.child("images")
                                                .setValue(arrayListOf(newImageUrl))
                                                .addOnCompleteListener { task ->
                                                    if (!task.isSuccessful) {
                                                        println("Error")
                                                    } else {
                                                        checkTick.visibility = View.GONE
                                                        Toast.makeText(
                                                            this@Setting_Menu_Activity,
                                                            "Profile Picture Updated",
                                                            Toast.LENGTH_SHORT
                                                        )
                                                            .show()
                                                    }
                                                }

                                        }
                                    }
                            }
                        }
                        .addOnFailureListener { exception ->
                            println(exception)
                        }
                }


            }
            .addOnFailureListener { exception ->

                println(exception)
            }
    }

    override fun onDestroy() {
        super.onDestroy()
        (application as ConnectAppApplication).stopAppUsageTracking()
    }

}
