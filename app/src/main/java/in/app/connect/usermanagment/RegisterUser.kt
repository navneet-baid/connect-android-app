package `in`.app.connect.usermanagment

import android.app.ProgressDialog
import android.content.Intent
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.SeekBar
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import `in`.app.connect.MainActivity
import `in`.app.connect.R
import `in`.app.connect.usermanagment.models.BioData
import `in`.app.connect.usermanagment.models.DOBData
import `in`.app.connect.usermanagment.models.DistanceData
import `in`.app.connect.usermanagment.models.EmailPasswordData
import `in`.app.connect.usermanagment.models.GenderData
import `in`.app.connect.usermanagment.models.ImagesData
import `in`.app.connect.usermanagment.models.LookingForData
import `in`.app.connect.usermanagment.models.NameData
import `in`.app.connect.usermanagment.models.PhoneNumberData
import `in`.app.connect.usermanagment.models.UserData
import `in`.app.connect.utils.CryptoUtils.hashPassword
import `in`.app.connect.utils.SessionManager

interface UserDataListener {
    fun onPhoneNumberDataReceived(phoneData: PhoneNumberData)
    fun onEmailPasswordDataReceived(emailPasswordData: EmailPasswordData)
    fun onNameDataReceived(nameData: NameData)
    fun onGenderDataReceived(genderData: GenderData)
    fun onDOBDataReceived(dobData: DOBData)
    fun onBioDataReceived(bioData: BioData)
    fun onIAmLookingForDataReceived(lookingForData: LookingForData)
    fun onDistanceDataReceived(distanceData: DistanceData, latitude: Double, longitude: Double)
    fun onImagesDataReceived(imagesData: ImagesData)
}

class RegisterUser : AppCompatActivity(), UserDataListener {
    lateinit var seekBar: SeekBar
    lateinit var backArrow: ImageView
    private lateinit var btnNext: Button
    private var currentFragmentIndex = 0
    private val fragmentStack = mutableListOf<Fragment>()
    private lateinit var fragments: Array<Fragment>
    lateinit var userData: UserData
    private var completePhoneNumber = ""
    private var emailId = ""
    private var password = ""
    private var fullName = ""
    private var dob = ""
    private var gender = ""
    private var bio = ""
    private var lookingFor = ""
    private var distance = 0
    private var latitude = 0.0
    private var longitude = 0.0
    private var images = arrayListOf<String>()
    lateinit var imagesUri: Array<Uri?>

    override fun onPhoneNumberDataReceived(phoneData: PhoneNumberData) {
        // Handle the received phone data here
        btnNext.visibility = View.VISIBLE
        completePhoneNumber = "+${phoneData.countryCode}${phoneData.phoneNumber}"
    }

    override fun onEmailPasswordDataReceived(emailPasswordData: EmailPasswordData) {
        nextButtonListener()
        emailId = emailPasswordData.emailId
        password = emailPasswordData.password
    }

    override fun onNameDataReceived(nameData: NameData) {
        // Handle the received name data here
        nextButtonListener()
        fullName = nameData.userName

    }

    override fun onGenderDataReceived(genderData: GenderData) {
        // Handle the received gender data here
        nextButtonListener()
        gender = genderData.gender

    }

    override fun onDOBDataReceived(dobData: DOBData) {
        // Handle the received date of birth data here
        nextButtonListener()
        dob = dobData.dob

    }

    override fun onBioDataReceived(bioData: BioData) {
        // Handle the received bio data here
        nextButtonListener()
        bio = bioData.bio

    }

    override fun onIAmLookingForDataReceived(lookingForData: LookingForData) {
        // Handle the received bio data here
        nextButtonListener()
        lookingFor = lookingForData.lookingFor

    }

    override fun onDistanceDataReceived(
        distanceData: DistanceData,
        lat: Double,
        long: Double
    ) {
        // Handle the received distance preference data here
        nextButtonListener()
        distance = distanceData.distance
        latitude=lat
        longitude=long

    }

    override fun onImagesDataReceived(imagesData: ImagesData) {
        // Handle the received images data here
        nextButtonListener()
        userData = UserData(
            completePhoneNumber,
            emailId,
            hashPassword(password),
            fullName,
            dob,
            gender,
            bio,
            lookingFor,
            distance,
            latitude,
            longitude,
            images
        )
        imagesUri = imagesData.images
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register_user)

        seekBar = findViewById(R.id.seekBar)
        backArrow = findViewById(R.id.backArrow)
        btnNext = findViewById(R.id.btnNext)

        fragments = arrayOf(
            PhoneNumberVerificationFragment(),
            EmailPasswordFragment(),
            UserNameFragment(),
            BirthDaySelectionFragment(),
            GenderSelectionFragment(),
            BioFragment(),
            IAmLookingFragment(),
            DistancePreferenceFragment(),
            RecentPicturesFragment(),
            UserProfileFragment()
        )

        backArrow.setOnClickListener { onBackPressed() }
        updateProgressBar()
        loadFragment(fragments[currentFragmentIndex])
        nextButtonListener()

    }

    private fun nextButtonListener() {
        btnNext.setOnClickListener {
            currentFragmentIndex++

            if (currentFragmentIndex < fragments.size) {
                updateProgressBar()
                loadFragment(fragments[currentFragmentIndex])
            } else {
                uploadPicturesToFirebase()
            }
        }
    }

    override fun onBackPressed() {
        // Handle the back arrow click
        if (currentFragmentIndex > 0) {
            if (currentFragmentIndex == fragments.size - 1) {
                seekBar.visibility = View.VISIBLE
                backArrow.visibility = View.VISIBLE
            }
            currentFragmentIndex--
            updateProgressBar()
            loadFragment(fragmentStack[currentFragmentIndex])
        } else {
            super.onBackPressed()// If on the first fragment, finish the activity
        }
    }

    private fun updateProgressBar() {
        val totalFragments = fragments.size // Total number of fragments
        val progress = (currentFragmentIndex + 1) * 100 / totalFragments
        seekBar.progress = progress
    }

    private fun loadFragment(fragment: Fragment) {
        fragmentStack.add(fragment)
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragmentContainer, fragment)
            .commit()
    }

    private fun uploadPicturesToFirebase() {
        val storage = FirebaseStorage.getInstance()
        val storageRef = storage.reference
        val uploadedImageUrls = mutableListOf<String>()
        val imageCount=imagesUri.count { it != null }
        // Upload each selected picture to Firebase Storage
        for (i in imagesUri.indices) {
            imagesUri[i]?.let { uri ->
                val progressDialog = ProgressDialog(this@RegisterUser)
                progressDialog.setTitle("Uploading...")
                progressDialog.setCancelable(false)
                progressDialog.show()

                val fileExtension = uri.lastPathSegment?.substringAfterLast(".")
                val imageRef: StorageReference =
                    storageRef.child("RecentPicture/$completePhoneNumber/image$i.$fileExtension")
                val uploadTask = imageRef.putFile(uri)

                uploadTask.addOnSuccessListener {
                    val uriTask = it.storage.downloadUrl
                    while (!uriTask.isComplete);
                    val url = uriTask.result
                    uploadedImageUrls.add(url.toString())
                    // Check if all images have been uploaded
                    println(uploadedImageUrls.size == imageCount)
                    if (uploadedImageUrls.size == imageCount) {
                        println("saving...")
                        images.addAll(uploadedImageUrls)
                        // All images have been uploaded, now call the function to save user data
                        saveUserDataToDatabase(userData)
                    }
                    progressDialog.dismiss()
                }.addOnFailureListener {
                    Toast.makeText(
                        this@RegisterUser,
                        it.message,
                        Toast.LENGTH_SHORT
                    ).show()
                }.addOnProgressListener {
                    val progress = (100.0 * it.bytesTransferred) / it.totalByteCount
                    progressDialog.setMessage("Uploaded: " + progress.toInt() + "%")
                }
            }
        }

    }
    private val usersDatabaseRef: DatabaseReference = FirebaseDatabase.getInstance().reference.child("Users")

    private fun saveUserDataToDatabase(userData: UserData) {
        usersDatabaseRef.child(completePhoneNumber).setValue(userData)
            .addOnSuccessListener {
                // User data saved successfully
                Toast.makeText(this@RegisterUser, "User Registered Successfully", Toast.LENGTH_SHORT).show()
                logInUser(userData)
            }
            .addOnFailureListener { exception ->
                // Handle Realtime Database data upload failure
                println(exception)
                Toast.makeText(this@RegisterUser, "Some Error Occurred While registering new users", Toast.LENGTH_SHORT).show()
                finish()
            }
    }
    private fun logInUser(userData: UserData) {
        val fullName = userData.userName
        val emailId = userData.emailId
        val registeredPhoneNumber =userData.phoneNumber
        val password=userData.password
        val sessionManager = SessionManager(this@RegisterUser)
        sessionManager.createLoginSession(
            fullName,
            registeredPhoneNumber,
            emailId,
            password
        )
        sessionManager.updateUserDetails(
            fullName,
            registeredPhoneNumber,
            emailId,
            password,
            userData.dob,
            userData.gender,
            userData.bio,
            userData.lookingFor,
            "","",
            userData.distance,
        )
        FirebaseMessaging.getInstance().token.addOnSuccessListener { fcmToken ->
            // Update the user's data in the database with the FCM token
            updateUserInFirebase( registeredPhoneNumber, fcmToken)
        }
        startActivity(Intent(this@RegisterUser,MainActivity::class.java));
        finish()
    }
    private fun updateUserInFirebase(phoneNumber: String, fcmToken: String) {
        // Update the user's data in Firebase with the FCM token
        val userReference = FirebaseDatabase.getInstance().getReference("Users")
        val userQuery = userReference.orderByChild("phoneNumber").equalTo(phoneNumber)

        userQuery.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.exists()) {
                    // User exists in the database, update their FCM token
                    for (userSnapshot in snapshot.children) {
                        val userKey = userSnapshot.key
                        userReference.child(userKey!!).child("fcmToken").setValue(fcmToken)
                    }
                    startActivity(Intent(this@RegisterUser, MainActivity::class.java))
                    finish()
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(this@RegisterUser, error.message, Toast.LENGTH_LONG).show()
            }
        })
    }

}