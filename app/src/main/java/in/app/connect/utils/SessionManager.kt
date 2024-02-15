package `in`.app.connect.utils


import android.content.Context
import android.content.SharedPreferences

class SessionManager {
    lateinit var userSession: SharedPreferences
    lateinit var editor: SharedPreferences.Editor
    lateinit var context: Context
    private var IS_LOGIN = "IsLoggedIn"
    var KEY_FULLNAME = "fullName"
    var KEY_PHONENUMBER = "phoneNumber"
    var KEY_EMAILID = "email"
    var KEY_PASSWORD = "password"
    var KEY_GENDER = "gender"
    var KEY_DOB = "dob"
    var KEY_BIO = "bio"
    var KEY_DISTANCE = "distance"
    var KEY_LOCATION = "location"
    var KEY_HOMETOWN = "hometown"
    var KEY_LOOKING_FOR = "lookingFor"
    var IS_FEEDBACK_RECEIVED = "IsFeedbackReceived"

    constructor() {}

    constructor(_context: Context) {
        context = _context
        userSession = context.getSharedPreferences("userLoginSession", Context.MODE_PRIVATE)
        editor = userSession.edit()
    }

    fun createLoginSession(
        fullName: String,
        phoneNumber: String,
        emailId: String,
        password: String
    ) {
        editor.putBoolean(IS_LOGIN, true)
        editor.putString(KEY_FULLNAME, fullName)
        editor.putString(KEY_PHONENUMBER, phoneNumber)
        editor.putString(KEY_EMAILID, emailId)
        editor.putString(KEY_PASSWORD, password)
        editor.commit()
    }

    fun updateUserDetails(
        fullName: String,
        phoneNumber: String,
        emailId: String,
        password: String,
        dob: String,
        gender: String,
        bio: String,
        lookingFor: String = "",
        location: String = "",
        hometown: String = "",
        distance: Int = 50,
    ) {
        editor.putString(KEY_FULLNAME, fullName)
        editor.putString(KEY_PHONENUMBER, phoneNumber)
        editor.putString(KEY_EMAILID, emailId)
        editor.putString(KEY_PASSWORD, password)
        editor.putString(KEY_DOB, dob)
        editor.putString(KEY_GENDER, gender)
        editor.putString(KEY_BIO, bio)
        editor.putString(KEY_LOOKING_FOR, lookingFor)
        editor.putInt(KEY_DISTANCE, distance)
        editor.putString(KEY_LOCATION, location)
        editor.putString(KEY_HOMETOWN, hometown)
        editor.commit()
    }

    fun updateLocationAndHometown(
        location: String = "",
        hometown: String = ""
    ) {
        editor.putString(KEY_LOCATION, location)
        editor.putString(KEY_HOMETOWN, hometown)
        editor.commit()

    }

    fun getUserDetailFromSession(): HashMap<String, Any> {
        val userData: HashMap<String, Any> = HashMap()
        userData[KEY_FULLNAME] = userSession.getString(KEY_FULLNAME, null)!!
        userData[KEY_PHONENUMBER] = userSession.getString(KEY_PHONENUMBER, null)!!
        userData[KEY_EMAILID] = userSession.getString(KEY_EMAILID, null)!!
        userData[KEY_PASSWORD] = userSession.getString(KEY_PASSWORD, null)!!
        userData[KEY_DOB] = userSession.getString(KEY_DOB, "")!!
        userData[KEY_GENDER] = userSession.getString(KEY_GENDER, "")!!
        userData[KEY_BIO] = userSession.getString(KEY_BIO, "")!!
        userData[KEY_LOOKING_FOR] = userSession.getString(KEY_LOOKING_FOR, "")!!
        userData[KEY_LOCATION] = userSession.getString(KEY_LOCATION, "")!!
        userData[KEY_HOMETOWN] = userSession.getString(KEY_HOMETOWN, "")!!
        userData[KEY_DISTANCE] = userSession.getInt(KEY_DISTANCE, 50)
        return userData
    }

    fun checkLogin(): Boolean {
        return userSession.getBoolean(IS_LOGIN, false)
    }

    fun setFeedBackReceived() {
        editor.putBoolean(IS_FEEDBACK_RECEIVED, true)
        editor.commit()
    }

    fun getFeedBackReceived(): Boolean {
        return userSession.getBoolean(IS_FEEDBACK_RECEIVED, false)
    }

    fun logoutUserFromSession() {
        editor.clear()
        editor.commit()
    }
}