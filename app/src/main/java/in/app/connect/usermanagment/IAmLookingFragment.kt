package `in`.app.connect.usermanagment

import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.util.TypedValue
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.cardview.widget.CardView
import com.google.android.material.card.MaterialCardView
import `in`.app.connect.R
import `in`.app.connect.usermanagment.models.GenderData
import `in`.app.connect.usermanagment.models.LookingForData

class IAmLookingFragment : Fragment() {

    private lateinit var cardViews: Array<MaterialCardView>
    private var selectedCardIndex: Int = -1
    private lateinit var nextButton: Button
    private val lookingFor= arrayOf("Introvert","Extrovert","Embivert")
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        val view = inflater.inflate(R.layout.fragment_i_am_looking_fragment, container, false)

        cardViews = arrayOf(
            view.findViewById(R.id.choice1),
            view.findViewById(R.id.choice2),
            view.findViewById(R.id.choice3)
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
            val userDataListener = requireActivity() as? UserDataListener
            userDataListener?.onIAmLookingForDataReceived(LookingForData(lookingFor[selectedCardIndex]))
            nextButton.performClick()
        }
        return view
    }

    private fun selectCard(index: Int) {
        for (i in cardViews.indices) {
            if(i==index){
                // Update the stroke color of the newly selected card to the selected color
                cardViews[i].strokeColor = Color.RED
            }else{
                val typedValue = TypedValue()
                context?.theme?.resolveAttribute(android.R.attr.colorForeground, typedValue, true)
                // If there was a previously selected card, reset its stroke color to default
                cardViews[i].strokeColor =  typedValue.data
            }
        }
        // Update the selected card index0
        selectedCardIndex = index
    }
}