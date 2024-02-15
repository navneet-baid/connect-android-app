package `in`.app.connect.usermanagment

import android.annotation.SuppressLint
import android.content.res.Resources
import android.content.res.TypedArray
import android.graphics.Color
import android.hardware.biometrics.BiometricManager.Strings
import android.os.Bundle
import android.util.TypedValue
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.core.content.ContextCompat
import com.google.android.material.card.MaterialCardView
import `in`.app.connect.R
import `in`.app.connect.usermanagment.models.GenderData
import `in`.app.connect.usermanagment.models.NameData


class GenderSelectionFragment : Fragment() {


    private lateinit var cardViews: Array<MaterialCardView>
    private var selectedGenderIndex: Int = -1
    private lateinit var error: TextView
    private lateinit var nextButton: Button
    private val genders= arrayOf("Male","Female","Other")
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        val view= inflater.inflate(R.layout.fragment_gender_selection, container, false)
        error=view.findViewById(R.id.error)
        cardViews = arrayOf(
            view.findViewById(R.id.genderMale),
            view.findViewById(R.id.genderFemale),
            view.findViewById(R.id.genderOther),
        )
        for (i in cardViews.indices) {
            cardViews[i].setOnClickListener {
                selectCard(i)
            }
        }
        // Access the activity reference (the fragment is attached to RegisterUser activity)
        val activity = requireActivity() as? RegisterUser
        // Check if the activity is not null and the nextButton is accessible (public)
        activity?.let {
            nextButton = it.findViewById(R.id.btnNext)
        }
        nextButton.setOnClickListener {
            if(selectedGenderIndex==-1){
                error.visibility=View.VISIBLE
                error.text="Please Select Gender."
            }else{
                error.visibility=View.GONE
                val userDataListener = requireActivity() as? UserDataListener
                userDataListener?.onGenderDataReceived(GenderData(genders[selectedGenderIndex]))
                nextButton.performClick()
            }
        }
        return view
    }


    private fun selectCard(index: Int) {
        val selectedColor = ContextCompat.getColor(requireContext(), R.color.red) // Replace R.color.colorPrimary with your actual resource ID

        for (i in cardViews.indices) {
            if (i == index) {
                // Update the stroke color of the newly selected card to the selected color
                cardViews[i].strokeColor = selectedColor
                cardViews[i].strokeWidth = 1
                println(i)
            } else {
                val defaultTextColorAttr = android.R.attr.textColorPrimary
                val typedArray: TypedArray = requireContext().obtainStyledAttributes(intArrayOf(defaultTextColorAttr))
                val defaultColor = typedArray.getColor(0, 0)
                typedArray.recycle()
                println(i)
                cardViews[i].strokeColor = defaultColor
                cardViews[i].strokeWidth = 0 // Set to default stroke width (no stroke)
            }
        }
        // Update the selected card index
        selectedGenderIndex = index
    }

}