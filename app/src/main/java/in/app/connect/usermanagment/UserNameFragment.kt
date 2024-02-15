package `in`.app.connect.usermanagment

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import `in`.app.connect.R
import `in`.app.connect.usermanagment.models.NameData
import `in`.app.connect.usermanagment.models.PhoneNumberData


class UserNameFragment : Fragment() {
    private lateinit var userName:EditText
    private lateinit var nextButton: Button
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        val view= inflater.inflate(R.layout.fragment_user_name, container, false)
        userName=view.findViewById(R.id.userName)
        // Access the activity reference (the fragment is attached to RegisterUser activity)
        val activity = requireActivity() as? RegisterUser
        // Check if the activity is not null and the nextButton is accessible (public)
        activity?.let {
            nextButton = it.findViewById(R.id.btnNext)
        }

        nextButton.setOnClickListener {
            val name=userName.text.toString().trim()
            if(name==""){
                userName.error="Name is required."
            }else if(name.length<2){
                userName.error="Invalid Name."
            }else{
                userName.error=null
                val userDataListener = requireActivity() as? UserDataListener
                userDataListener?.onNameDataReceived(NameData(name))
                nextButton.performClick()
            }
        }

        return view
    }


}