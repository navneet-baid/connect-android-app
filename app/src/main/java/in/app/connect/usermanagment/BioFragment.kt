package `in`.app.connect.usermanagment

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import com.google.android.material.textfield.TextInputLayout
import `in`.app.connect.R
import `in`.app.connect.usermanagment.models.BioData
import `in`.app.connect.usermanagment.models.NameData

class BioFragment : Fragment() {

    private lateinit var bio: TextInputLayout
    private lateinit var nextButton: Button
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        val view=  inflater.inflate(R.layout.fragment_bio, container, false)
        bio=view.findViewById(R.id.bio)
        // Access the activity reference (the fragment is attached to RegisterUser activity)
        val activity = requireActivity() as? RegisterUser
        // Check if the activity is not null and the nextButton is accessible (public)
        activity?.let {
            nextButton = it.findViewById(R.id.btnNext)
        }

        nextButton.setOnClickListener {
            val bioText=bio.editText?.text.toString().trim()
            if(bioText.isEmpty()){
                bio.error="Bio is required."
            }else{
                bio.error=null
                val userDataListener = requireActivity() as? UserDataListener
                userDataListener?.onBioDataReceived(BioData(bioText))
                nextButton.performClick()
            }
        }
        return view
    }


}