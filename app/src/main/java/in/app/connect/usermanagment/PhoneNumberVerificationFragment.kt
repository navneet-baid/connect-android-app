package `in`.app.connect.usermanagment

import android.os.Bundle
import android.text.Editable
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import com.chaos.view.PinView
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseException
import com.google.firebase.FirebaseTooManyRequestsException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.PhoneAuthCredential
import com.google.firebase.auth.PhoneAuthOptions
import com.google.firebase.auth.PhoneAuthProvider
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.hbb20.CountryCodePicker
import `in`.app.connect.R
import `in`.app.connect.usermanagment.models.PhoneNumberData
import `in`.app.connect.utils.NetworkUtils
import java.util.concurrent.TimeUnit

class PhoneNumberVerificationFragment : Fragment() {

    private lateinit var countryCodePicker:CountryCodePicker
    private lateinit var phoneNumber:EditText
    private lateinit var otpBtn: Button
    private lateinit var otpLayout:LinearLayout
    private lateinit var helperText:TextView
    private lateinit var otpField:PinView
    private lateinit var auth: FirebaseAuth
    var codeByServer: String = "000000"
    private lateinit var nextButton:Button
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        val view= inflater.inflate(R.layout.fragment_phone_number_verification, container, false)
        countryCodePicker=view.findViewById(R.id.countryCodePicker)
        phoneNumber=view.findViewById(R.id.phoneNumber)
        otpBtn=view.findViewById(R.id.otpBtn)
        otpLayout=view.findViewById(R.id.otpLayout)
        helperText=view.findViewById(R.id.helperText)
        otpField=view.findViewById(R.id.otpField)

        FirebaseApp.initializeApp(requireContext())
        auth = FirebaseAuth.getInstance()

        otpBtn.setOnClickListener {
            val phone=phoneNumber.text
            if(phone.length<5){
                phoneNumber.error="Invalid Phone Number."
            }else{
                phoneNumber.error=null
                isUserPhoneNumberAlreadyRegistered()
            }
        }
        // Access the activity reference (the fragment is attached to RegisterUser activity)
        val activity = requireActivity() as? RegisterUser
        // Check if the activity is not null and the nextButton is accessible (public)
        activity?.let {
            nextButton = it.findViewById(R.id.btnNext)
            nextButton.visibility=View.GONE
        }
        return view
    }

    private fun isUserPhoneNumberAlreadyRegistered() {
        otpBtn.isEnabled = false
        otpBtn.text = "Verifying..."
        var phone = phoneNumber.text.toString()
        if (phone[0] == '0') {
            phone = phone.substring(1)
        }
        val completedPhoneNumber =
            "+" + countryCodePicker.selectedCountryCode + phone
        val checkUser =
            FirebaseDatabase.getInstance().getReference("Users").orderByChild("phoneNumber")
                .equalTo(completedPhoneNumber)

        checkUser.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.exists()) {
                    phoneNumber.error = "Phone number already registered."
                    otpBtn.isEnabled = true
                    otpBtn.text = "Continue"
                } else {
                    phoneNumber.error = null
                    val countryCode=countryCodePicker.selectedCountryCode
                    val completePhoneNumber= "+$countryCode$phone"
                    otpLayout.visibility=View.VISIBLE
                    phoneNumber.isEnabled=false
                    otpBtn.text="Send OTP"
                    otpBtn.isEnabled=true
                    sendVerificationCodeToUser(completePhoneNumber)
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(requireContext(), error.message, Toast.LENGTH_LONG).show()
            }
        })

    }

    private fun sendVerificationCodeToUser(phoneNumber: String) {
        if (NetworkUtils.isNetworkAvailable(requireContext())) {
            val options = PhoneAuthOptions.newBuilder(auth)
                .setPhoneNumber(phoneNumber)
                .setTimeout(60L, TimeUnit.SECONDS)
                .setActivity(requireActivity())
                .setCallbacks(callbacks)
                .build()
            PhoneAuthProvider.verifyPhoneNumber(options)
        } else {
            NetworkUtils.showNoInternetSnackBar(requireView())
        }
    }
    private var callbacks = object : PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
        override fun onVerificationCompleted(credential: PhoneAuthCredential) {
            val code = credential.smsCode
            val editableOtp = Editable.Factory.getInstance().newEditable(code)
            if (code != null) {
                otpField.text = editableOtp
                verifyCode(code)
            }
        }

        override fun onVerificationFailed(e: FirebaseException) {
            when (e) {
                is FirebaseAuthInvalidCredentialsException -> {
                    Toast.makeText(requireContext(), "Invalid phone number format", Toast.LENGTH_LONG)
                        .show()
                }
                is FirebaseTooManyRequestsException -> {
                    Toast.makeText(
                        requireContext(),
                        "SMS limit exceeded! Try again after 30 minutes.",
                        Toast.LENGTH_LONG
                    ).show()
                }
                else -> {
                    println(e)
                    println(e.message)
                    Toast.makeText(
                        requireContext(),
                        e.message,
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }


        override fun onCodeSent(
            verificationId: String,
            token: PhoneAuthProvider.ForceResendingToken
        ) {
            codeByServer = verificationId
            otpBtn.isEnabled=true
            otpBtn.text="Verify"
            otpBtn.setOnClickListener {
                val code = otpField.text.toString()
                if (code.isNotEmpty()) {
                    verifyCode(code)
                }
            }

        }
    }

    private fun verifyCode(code: String) {
        if (NetworkUtils.isNetworkAvailable(requireContext())) {
            if (codeByServer != "000000") {
               otpBtn.text = "Verifying..."
                val credential = PhoneAuthProvider.getCredential(codeByServer, code)
                signInWithPhoneAuthCredential(credential)
            }
        } else {
            NetworkUtils.showNoInternetSnackBar(requireView())
        }
    }

    private fun signInWithPhoneAuthCredential(credential: PhoneAuthCredential) {
        otpBtn.text = "Verifying..."
        auth.signInWithCredential(credential).addOnCompleteListener(requireActivity()) { task ->
            if (task.isSuccessful) {
                otpBtn.text = "Verified"
                otpBtn.isEnabled=false
                val phoneNumber = phoneNumber.text.toString()
                val activity = requireActivity() as? UserDataListener
                activity?.onPhoneNumberDataReceived(PhoneNumberData(countryCodePicker.selectedCountryCode,phoneNumber))
            } else {
                if (task.exception is FirebaseAuthInvalidCredentialsException) {
                    Toast.makeText(
                        requireContext(),
                        "Incorrect one time password.",
                        Toast.LENGTH_LONG
                    ).show()
                    otpBtn.text = "Try Again"
                }
            }
        }
    }
}