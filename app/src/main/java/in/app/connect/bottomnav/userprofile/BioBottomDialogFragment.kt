package `in`.app.connect.bottomnav.userprofile

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import `in`.app.connect.R

class BioBottomDialogFragment : BottomSheetDialogFragment() {

    interface BioUpdateListener {
        fun onUpdateBio(newBio: String)
    }

    private var bioUpdateListener: BioUpdateListener? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_bio_bottom_dialog, container, false)

        val buttonCorrect = view.findViewById<Button>(R.id.buttonCorrect)
        val buttonIncorrect = view.findViewById<Button>(R.id.buttonIncorrect)
        val editTextNewBio = view.findViewById<EditText>(R.id.editTextNewBio)


        // Retrieve bio data from arguments
        val currentBio = arguments?.getString("bio")

        // Set the current bio data to the EditText in the fragment
        editTextNewBio.setText(currentBio)
        // Handle "Correct" button click
        buttonCorrect.setOnClickListener {
            val newBio = editTextNewBio.text.toString()
            bioUpdateListener?.onUpdateBio(newBio)
            dismiss()
        }
        // Handle "Incorrect" button click
        buttonIncorrect.setOnClickListener {
            dismiss()
        }

        return view
    }

    fun setBioUpdateListener(listener: BioUpdateListener) {
        bioUpdateListener = listener
    }
}
