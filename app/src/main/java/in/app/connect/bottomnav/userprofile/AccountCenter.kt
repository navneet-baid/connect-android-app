package `in`.app.connect.bottomnav.userprofile

import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.RelativeLayout
import android.widget.Switch
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import com.google.android.material.switchmaterial.SwitchMaterial
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import `in`.app.connect.R
import `in`.app.connect.usermanagment.models.UserData
import `in`.app.connect.utils.ConnectAppApplication
import `in`.app.connect.utils.CryptoUtils
import `in`.app.connect.utils.SessionManager
import java.util.HashMap

class AccountCenter : AppCompatActivity() {
    private lateinit var oldPasswordEditText: EditText
    private lateinit var newPasswordEditText: EditText
    private lateinit var changePasswordButton: Button
    private lateinit var backButton: ImageView
    private lateinit var enablePostDisLikesSwitch: SwitchMaterial
    private lateinit var sessionManager: SessionManager
    private lateinit var usersRef: DatabaseReference
    private lateinit var userDetails: HashMap<String, Any>

    private lateinit var changePasswordSection: RelativeLayout
    private lateinit var changePasswordArrow: ImageView
    private lateinit var changePasswordCardView: CardView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        (application as ConnectAppApplication).startAppUsageTracking()
        setContentView(R.layout.activity_account_center)

        initializeViews()
        setOnClickListeners()
        setupFirebaseReferences()
        fetchIsDislikesEnable()
    }

    private fun initializeViews() {
        oldPasswordEditText = findViewById(R.id.oldPasswordEditText)
        newPasswordEditText = findViewById(R.id.newPasswordEditText)
        backButton = findViewById(R.id.backButton)
        changePasswordButton = findViewById(R.id.changePasswordButton)
        enablePostDisLikesSwitch = findViewById(R.id.enablePostDisLikesSwitch)
        sessionManager = SessionManager(this)
        changePasswordSection = findViewById(R.id.changePasswordSection)
        changePasswordArrow = findViewById(R.id.changePasswordArrow)
        changePasswordCardView = findViewById(R.id.changePasswordCardView)
    }

    private fun setOnClickListeners() {
        backButton.setOnClickListener { onBackPressed() }

        changePasswordSection.setOnClickListener {
            toggleChangePasswordSection()
        }

        changePasswordButton.setOnClickListener {
            changePassword()
        }

        enablePostDisLikesSwitch.setOnCheckedChangeListener { _, isChecked ->
            updateIsDislikesEnable(isChecked)
        }
    }

    private fun setupFirebaseReferences() {
        val database: FirebaseDatabase = FirebaseDatabase.getInstance()
        usersRef = database.getReference("Users")
        userDetails = sessionManager.getUserDetailFromSession()
    }

    private fun fetchIsDislikesEnable() {
        val phoneNumber = userDetails[sessionManager.KEY_PHONENUMBER]
        usersRef.child(phoneNumber.toString()).child("isDislikesEnable")
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(dataSnapshot: DataSnapshot) {
                    if (dataSnapshot.exists()) {
                        val isDislikesEnable = dataSnapshot.getValue(Boolean::class.java) ?: false
                        enablePostDisLikesSwitch.isChecked = isDislikesEnable
                    } else {
                        // Handle the case where data doesn't exist
                    }
                }

                override fun onCancelled(databaseError: DatabaseError) {
                    // Handle database errors
                }
            })
    }

    private fun updateIsDislikesEnable(isChecked: Boolean) {
        val phoneNumber = userDetails[sessionManager.KEY_PHONENUMBER]
        usersRef.child(phoneNumber.toString()).child("isDislikesEnable")
            .setValue(isChecked)
    }

    private fun toggleChangePasswordSection() {
        if (changePasswordCardView.visibility == View.VISIBLE) {
            changePasswordCardView.visibility = View.GONE
            changePasswordArrow.setImageResource(R.drawable.ic_expand_arrow)
        } else {
            changePasswordCardView.visibility = View.VISIBLE
            changePasswordArrow.setImageResource(R.drawable.ic_collapse_arrow)
        }
    }

    private fun changePassword() {
        changePasswordButton.isEnabled = false
        val oldPassword = oldPasswordEditText.text.toString()
        val newPassword = newPasswordEditText.text.toString()

        if (oldPassword.isEmpty() || newPassword.isEmpty()) {
            Toast.makeText(this, "Both fields are required.", Toast.LENGTH_SHORT).show()
            return
        }

        val phoneNumberToFetch = userDetails[sessionManager.KEY_PHONENUMBER]
        val passwordListener = object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                if (dataSnapshot.exists()) {
                    val user = dataSnapshot.getValue(UserData::class.java)
                    val hashOldPassword = user?.password
                    if (oldPassword != null) {
                        if (CryptoUtils.verifyPassword(oldPassword, hashOldPassword!!)) {
                            // Passwords match - userEnteredPassword is the correct old password
                            // Update the password with the new one
                            usersRef.child(phoneNumberToFetch.toString()).child("password")
                                .setValue(CryptoUtils.hashPassword(newPassword))
                            Toast.makeText(
                                this@AccountCenter,
                                "Password changed successfully.",
                                Toast.LENGTH_SHORT
                            ).show()
                            oldPasswordEditText.setText("")
                            newPasswordEditText.setText("")
                            changePasswordButton.isEnabled = true
                        } else {
                            // Passwords do not match
                            Toast.makeText(
                                this@AccountCenter,
                                "Old password is incorrect.",
                                Toast.LENGTH_SHORT
                            ).show()
                            changePasswordButton.isEnabled = true
                        }
                    } else {
                        Toast.makeText(
                            this@AccountCenter,
                            "Password not found for $phoneNumberToFetch",
                            Toast.LENGTH_SHORT
                        ).show()
                        changePasswordButton.isEnabled = true
                    }
                } else {
                    Toast.makeText(
                        this@AccountCenter,
                        "User with phone number $phoneNumberToFetch not found",
                        Toast.LENGTH_SHORT
                    ).show()
                    changePasswordButton.isEnabled = true
                }
            }

            override fun onCancelled(databaseError: DatabaseError) {
                // Handle any database error
                Toast.makeText(
                    this@AccountCenter,
                    "Error fetching data: ${databaseError.message}",
                    Toast.LENGTH_SHORT
                ).show()
                changePasswordButton.isEnabled = true
            }
        }

        usersRef.child(phoneNumberToFetch.toString())
            .addListenerForSingleValueEvent(passwordListener)
    }


    override fun onDestroy() {
        super.onDestroy()
        (application as ConnectAppApplication).stopAppUsageTracking()
    }
}
