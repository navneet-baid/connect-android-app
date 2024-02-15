package `in`.app.connect.authentication

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.messaging.FirebaseMessaging
import com.hbb20.CountryCodePicker
import `in`.app.connect.MainActivity
import `in`.app.connect.R
import `in`.app.connect.usermanagment.RegisterUser
import `in`.app.connect.utils.CryptoUtils.verifyPassword
import `in`.app.connect.utils.EmailSender.sendEmail
import `in`.app.connect.utils.NetworkUtils
import `in`.app.connect.utils.SessionManager

class LoginWithNumber : AppCompatActivity() {
    private lateinit var countryCodePicker : CountryCodePicker
    private lateinit var phoneNumber : EditText
    private lateinit var userPassword : EditText
    private lateinit var progressBar : ProgressBar
    private lateinit var btnLogin : Button
    private lateinit var resgisterUser : TextView
    private lateinit var rootView : View

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login_with_number)

        countryCodePicker= findViewById(R.id.countryCodePicker)
        phoneNumber= findViewById(R.id.phoneNumber)
        userPassword= findViewById(R.id.userPassword)
        progressBar= findViewById(R.id.progressBar)
        btnLogin= findViewById(R.id.btnLogin)
        resgisterUser= findViewById(R.id.resgisterUser)
        rootView= findViewById(android.R.id.content)

        btnLogin.setOnClickListener {
            if (NetworkUtils.isNetworkAvailable(this)) {
                logInUser()
            } else {
                NetworkUtils.showNoInternetSnackBar(rootView)
            }
        }

        resgisterUser.setOnClickListener{
            val intent = Intent(this@LoginWithNumber, RegisterUser::class.java)
            startActivity(intent)
        }

    }
    private fun logInUser() {
        if (!validatePhoneNumber() or !validatePassword()) {
            return
        }
        progressBar.visibility = View.VISIBLE
        //Fetch data
        var phone = phoneNumber.text.toString()

        if (phone[0] == '0') {
            phone = phone.substring(1)
        }
        val completedPhoneNumber =
            "+" + countryCodePicker.selectedCountryCode + phone
        val userEnteredPassword = userPassword.text.toString()

        val checkUser =
            FirebaseDatabase.getInstance().getReference("Users").orderByChild("phoneNumber")
                .equalTo(completedPhoneNumber)

        checkUser.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.exists()) {
                    phoneNumber.error = null
                    val serverPassword =
                        snapshot.child(completedPhoneNumber).child("password")
                            .getValue(String::class.java)

                    if (verifyPassword(userEnteredPassword, serverPassword!!)) {
                        userPassword.error = null
                        val fullName = snapshot.child(completedPhoneNumber).child("userName")
                            .getValue(String::class.java)
                        val emailId = snapshot.child(completedPhoneNumber).child("emailId")
                            .getValue(String::class.java)
                        val registeredPhoneNumber = snapshot.child(completedPhoneNumber).child("phoneNumber")
                            .getValue(String::class.java)
                        progressBar.visibility = View.INVISIBLE
                        val sessionManager = SessionManager(this@LoginWithNumber)
                        sessionManager.createLoginSession(
                            fullName!!,
                            registeredPhoneNumber!!,
                            emailId!!,
                            userEnteredPassword
                        )
                        val location = snapshot.child(completedPhoneNumber).child("location").getValue(String::class.java)
                        val hometown = snapshot.child(completedPhoneNumber).child("hometown").getValue(String::class.java)

                        val defaultEmptyString = ""

                        val locationValue = location?.takeIf { it.isNotBlank() } ?: defaultEmptyString
                        val hometownValue = hometown?.takeIf { it.isNotBlank() } ?: defaultEmptyString
                        sessionManager.updateUserDetails(
                            fullName,
                            registeredPhoneNumber,
                            emailId,
                            userEnteredPassword,
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
//                        try {
//                            sendEmail(
//                                emailId,
//                                "New login detected on your Connect-App account",
//                                "<!DOCTYPE html>\n" +
//                                        "<html>\n" +
//                                        "  <head>\n" +
//                                        "    <meta charset=\"UTF-8\">\n" +
//                                        "    <title>New login detected</title>\n" +
//                                        "    <style>\n" +
//                                        "      body {\n" +
//                                        "        font-family: Arial, sans-serif;\n" +
//                                        "        font-size: 16px;\n" +
//                                        "        line-height: 1.5;\n" +
//                                        "        color: #333;\n" +
//                                        "      }\n" +
//                                        "      .container {\n" +
//                                        "        max-width: 600px;\n" +
//                                        "        margin: 0 auto;\n" +
//                                        "        padding: 20px;\n" +
//                                        "        background-color: #f8f8f8;\n" +
//                                        "        border: 1px solid #ddd;\n" +
//                                        "        border-radius: 5px;\n" +
//                                        "      }\n" +
//                                        "      .logo {\n" +
//                                        "        display: block;\n" +
//                                        "        max-width: 100px;\n" +
//                                        "        margin: 0 auto;\n" +
//                                        "        padding: 10px;\n" +
//                                        "      }\n" +
//                                        "      h1 {\n" +
//                                        "        font-size: 24px;\n" +
//                                        "        font-weight: bold;\n" +
//                                        "        color: #333;\n" +
//                                        "        margin-top: 0;\n" +
//                                        "        margin-bottom: 10px;\n" +
//                                        "        text-align: center;\n" +
//                                        "      }\n" +
//                                        "      p {\n" +
//                                        "        margin-top: 0;\n" +
//                                        "        margin-bottom: 10px;\n" +
//                                        "      }\n" +
//                                        "      .highlight {\n" +
//                                        "        font-weight: bold;\n" +
//                                        "        color: #d9534f;\n" +
//                                        "      }\n" +
//                                        "      .footer {\n" +
//                                        "        margin-top: 20px;\n" +
//                                        "        font-size: 14px;\n" +
//                                        "        text-align: center;\n" +
//                                        "      }\n" +
//                                        "    </style>\n" +
//                                        "  </head>\n" +
//                                        "  <body>\n" +
//                                        "    <div class=\"container\">\n" +
//                                        "      <img src=\"https://firebasestorage.googleapis.com/v0/b/connect-app-c8ee6.appspot.com/o/assets%2Fapp_logo.png?alt=media&token=43da3968-7a0e-48d5-8d52-ab3b04387e1f\" alt=\"Connect App\" class=\"logo\">\n" +
//                                        "      <h1>New login detected</h1>\n" +
//                                        "      <p><b>Dear $fullName,</b></p>\n" +
//                                        "      <p>We hope this message finds you well. We wanted to inform you that a new login has been detected on your Connect-App account. If you did not initiate this login, please contact our support team immediately.</p>\n" +
//                                        "      <p>If this login was initiated by you, please disregard this message.</p>\n" +
//                                        "      <p>Thank you for using Connect-App.</p>\n" +
//                                        "      <p class=\"footer\">Best regards,<br>Connect-App Team</p>\n" +
//                                        "    </div>\n" +
//                                        "  </body>\n" +
//                                        "</html>\n"
//                            )
//                        } catch (e: Exception) {
//                            println(e.message)
//                        }
                        // Obtain the FCM token
                        FirebaseMessaging.getInstance().token.addOnSuccessListener { fcmToken ->
                            // Update the user's data in the database with the FCM token
                            updateUserInFirebase( completedPhoneNumber, fcmToken)
                        }


                    } else {
                        progressBar.visibility = View.INVISIBLE
                        userPassword.error = "Invalid Password!"
                    }
                } else {
                    progressBar.visibility = View.INVISIBLE
                    phoneNumber.error = "No such user exists!"
                }
            }

            override fun onCancelled(error: DatabaseError) {
                progressBar.visibility = View.INVISIBLE
                Toast.makeText(this@LoginWithNumber, error.message, Toast.LENGTH_LONG).show()
            }

        })
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
                    startActivity(Intent(this@LoginWithNumber, MainActivity::class.java))
                    finishAffinity()
                }
            }

            override fun onCancelled(error: DatabaseError) {
                progressBar.visibility = View.INVISIBLE
                Toast.makeText(this@LoginWithNumber, error.message, Toast.LENGTH_LONG).show()
            }
        })
    }
    private fun validatePhoneNumber(): Boolean {
        val value = phoneNumber.text.toString()
        return if (value.isEmpty()) {
            phoneNumber.error = "Field cannot be empty"
            false
        } else {
            phoneNumber.error = null
            true
        }
    }

    private fun validatePassword(): Boolean {
        val value = userPassword.text.toString().trim()
        val passwordPattern = "^(?=.*[a-zA-Z])(?=.*[@#$%^&+=]).{5,}$"
        return if (value.isEmpty()) {
            userPassword.error = "Field cannot be empty"
            false
        } else if (!value.matches(passwordPattern.toRegex())) {
            userPassword.error = "Invalid Password"
            false
        } else {
            userPassword.error = null
            true
        }
    }
}