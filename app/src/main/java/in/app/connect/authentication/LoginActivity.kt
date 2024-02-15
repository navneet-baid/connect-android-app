package `in`.app.connect.authentication

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.LinearLayout
import android.widget.Toast
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.messaging.FirebaseMessaging
import `in`.app.connect.MainActivity
import `in`.app.connect.R
import `in`.app.connect.usermanagment.RegisterUser
import `in`.app.connect.utils.SessionManager

class LoginActivity : AppCompatActivity() {
    private val RC_SIGN_IN = 123
    private lateinit var googleSignInClient: GoogleSignInClient
    private lateinit var firebaseAuth: FirebaseAuth

    private lateinit var signWithPhoneNumber: LinearLayout
    private lateinit var singInWithGoogle: LinearLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)
        signWithPhoneNumber = findViewById(R.id.signWithPhoneNumber)
        singInWithGoogle = findViewById(R.id.singInWithGoogle)

        signWithPhoneNumber.setOnClickListener {
            val intent = Intent(this@LoginActivity, LoginWithNumber::class.java)
            startActivity(intent)
        }

        // Configure Google Sign-In
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id)) // Replace with your Web client ID
            .requestEmail()
            .build()

        googleSignInClient = GoogleSignIn.getClient(this, gso)

        // Initialize Firebase Auth
        firebaseAuth = FirebaseAuth.getInstance()

        singInWithGoogle.setOnClickListener {
            // Firebase sign out
            FirebaseAuth.getInstance().signOut()
            // Google sign out (if applicable)
            googleSignInClient.signOut()
            // Start the sign-in process again
            signInWithGoogle()
        }
    }

    private fun signInWithGoogle() {
        val signInIntent = googleSignInClient.signInIntent
        startActivityForResult(signInIntent, RC_SIGN_IN)
    }

    // Handle the Google Sign-In result
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == RC_SIGN_IN) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(data)
            try {
                val account = task.getResult(ApiException::class.java)
                if (account != null) {
                    firebaseAuthWithGoogle(account)
                }
            } catch (e: ApiException) {
                Toast.makeText(this, "Google sign-in failed", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun firebaseAuthWithGoogle(account: GoogleSignInAccount) {
        val credential = GoogleAuthProvider.getCredential(account.idToken, null)
        firebaseAuth.signInWithCredential(credential)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    val email = account.email
                    val googleId = account.id

                    FirebaseMessaging.getInstance().token.addOnSuccessListener { fcmToken ->
                        // Update the user's data in the database
                        updateUserInFirebase(email, googleId!!, fcmToken)
                    }
                } else {
                    Toast.makeText(this, "Google sign-in failed", Toast.LENGTH_SHORT).show()
                }
            }
    }

    private fun updateUserInFirebase(email: String?, googleId: String, fcmToken: String) {
        // Update the user's data in Firebase
        val userReference = FirebaseDatabase.getInstance().getReference("Users")
        val userQuery = userReference.orderByChild("emailId").equalTo(email)

        userQuery.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.exists()) {
                    // User exists in the database, update their FCM token
                    for (userSnapshot in snapshot.children) {
                        val userKey = userSnapshot.key
                        userReference.child(userKey!!).child("fcmToken").setValue(fcmToken)
                    }
                    isUserEmailRegistered(email!!)
                } else {
                    // Handle the case where the user doesn't exist in the database
                    Toast.makeText(
                        this@LoginActivity,
                        "Email not registered. Register Your Account.",
                        Toast.LENGTH_SHORT
                    ).show()
                    startActivity(Intent(this@LoginActivity, RegisterUser::class.java))
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(this@LoginActivity, error.message, Toast.LENGTH_LONG).show()
            }
        })
    }

    private fun isUserEmailRegistered(email: String) {
        val checkUser =
            FirebaseDatabase.getInstance().getReference("Users").orderByChild("emailId")
                .equalTo(email)
        checkUser.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.exists()) {
                    var completedPhoneNumber = ""
                    for (userSnapshot in snapshot.children) {
                        completedPhoneNumber =
                            userSnapshot.child("phoneNumber").getValue(String::class.java)!!
                    }
                    val password =
                        snapshot.child(completedPhoneNumber).child("password")
                            .getValue(String::class.java)
                    val fullName = snapshot.child(completedPhoneNumber).child("userName")
                        .getValue(String::class.java)
                    val emailId = snapshot.child(completedPhoneNumber).child("emailId")
                        .getValue(String::class.java)
                    val registeredPhoneNumber =
                        snapshot.child(completedPhoneNumber).child("phoneNumber")
                            .getValue(String::class.java)

                    val sessionManager = SessionManager(this@LoginActivity)
                    sessionManager.createLoginSession(
                        fullName!!,
                        registeredPhoneNumber!!,
                        emailId!!,
                        password!!
                    )
                    val location = snapshot.child(completedPhoneNumber).child("location")
                        .getValue(String::class.java)
                    val hometown = snapshot.child(completedPhoneNumber).child("hometown")
                        .getValue(String::class.java)

                    val defaultEmptyString = ""

                    val locationValue = location?.takeIf { it.isNotBlank() } ?: defaultEmptyString
                    val hometownValue = hometown?.takeIf { it.isNotBlank() } ?: defaultEmptyString
                    sessionManager.updateUserDetails(
                        fullName,
                        registeredPhoneNumber,
                        emailId,
                        password,
                        snapshot.child(completedPhoneNumber).child("dob")
                            .getValue(String::class.java)!!,
                        snapshot.child(completedPhoneNumber).child("gender")
                            .getValue(String::class.java)!!,
                        snapshot.child(completedPhoneNumber).child("bio")
                            .getValue(String::class.java)!!,
                        snapshot.child(completedPhoneNumber).child("lookingFor")
                            .getValue(String::class.java)!!,
                        locationValue,
                        hometownValue,
                        snapshot.child(completedPhoneNumber).child("distance")
                            .getValue(Int::class.java)!!,
                    )

                    startActivity(Intent(this@LoginActivity, MainActivity::class.java))
                    finishAffinity()

                }
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(this@LoginActivity, error.message, Toast.LENGTH_LONG).show()
            }
        })
    }
}
