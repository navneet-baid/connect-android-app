package `in`.app.connect.usermanagment

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import `in`.app.connect.R
import `in`.app.connect.usermanagment.models.EmailPasswordData
import `in`.app.connect.usermanagment.models.NameData
import `in`.app.connect.utils.Validator

class EmailPasswordFragment : Fragment() {
    private lateinit var userEmail: EditText
    private lateinit var userPassword: EditText
    private lateinit var nextButton: Button

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        val view = inflater.inflate(R.layout.fragment_email_password, container, false)
        userEmail = view.findViewById(R.id.userEmail)
        userPassword = view.findViewById(R.id.userPassword)
        // Access the activity reference (the fragment is attached to RegisterUser activity)
        val activity = requireActivity() as? RegisterUser
        // Check if the activity is not null and the nextButton is accessible (public)
        activity?.let {
            nextButton = it.findViewById(R.id.btnNext)
        }

        nextButton.setOnClickListener {
            val email = userEmail.text.toString().trim()
            if (email == "") {
                userEmail.error = "Email is required."
            } else if (!validateEmailId(email)) {
                userEmail.error = "Invalid Email Id."
            } else {
                userEmail.error = null
                val password = userPassword.text.toString().trim()
                if (password == "") {
                    userPassword.error = "Password is required."
                } else if (!validatePassword(password)) {
                    userPassword.error =
                        "Password must contain at least one alphabetical character, one special character, and be at least 5 characters long"
                } else {
                    userPassword.error = null
                    isUserEmailAlreadyRegistered()
                }
            }
        }
        return view
    }
    private fun isUserEmailAlreadyRegistered() {
        val email = userEmail.text.toString().trim()
        nextButton.text="Verifying..."
        val checkUser =
            FirebaseDatabase.getInstance().getReference("Users").orderByChild("emailId")
                .equalTo(email)
        checkUser.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.exists()) {
                    nextButton.text="NEXT"
                    userEmail.error = "Email Id already registered."
                } else {
                    nextButton.text="NEXT"
                    val userDataListener = requireActivity() as? UserDataListener
                    userDataListener?.onEmailPasswordDataReceived(
                        EmailPasswordData(
                            email,
                            userPassword.text.toString().trim()
                        )
                    )
                    nextButton.performClick()
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(requireContext(), error.message, Toast.LENGTH_LONG).show()
            }
        })
    }
    private fun validateEmailId(emailId: String): Boolean {
        val emailPattern = "[a-zA-Z0-9._-]+@[a-z]+\\.+[a-z]+"
        return if (emailId.isEmpty()) {
            false
        } else emailId.matches(emailPattern.toRegex())
    }

    private fun validatePassword(password: String): Boolean {
        val passwordPattern = "^(?=.*[a-zA-Z])(?=.*[@#$%^&+=]).{5,}$"
        return if (password.isEmpty()) {
            false
        } else password.matches(passwordPattern.toRegex())
    }
}
