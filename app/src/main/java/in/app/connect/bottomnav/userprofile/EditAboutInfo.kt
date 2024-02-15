package `in`.app.connect.bottomnav.userprofile

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.Button
import android.widget.EditText
import android.widget.Spinner
import androidx.fragment.app.DialogFragment
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import `in`.app.connect.R

class EditAboutInfo : BottomSheetDialogFragment() {
    // Define the interface to communicate with the activity
    interface UpdateListener {
        fun onUpdate(
            gender: String?,
            lookingFor: String?,
            location: String?,
            hometown: String?,
        )
    }

    private var updateListener: UpdateListener? = null

    // Set the listener when the fragment is attached to the activity
    fun setUpdateListener(listener: UpdateListener?) {
        updateListener = listener
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view: View =
            inflater.inflate(R.layout.fragment_edit_about_info, container, false)

        // Initialize UI components
        val genderDropdown = view.findViewById<Spinner>(R.id.genderDropdown)
        val lookingForDropdown = view.findViewById<Spinner>(R.id.lookingForDropdown)
        val locationEditText = view.findViewById<EditText>(R.id.locationEditText)
        val hometownEditText = view.findViewById<EditText>(R.id.hometownEditText)
        val updateButton = view.findViewById<Button>(R.id.updateButton)
        val cancelButton = view.findViewById<Button>(R.id.cancelButton)

        // Populate dropdowns
        val genders = resources.getStringArray(R.array.genders)
        val lookingForArray = resources.getStringArray(R.array.looking_for)
        val adapter =
            ArrayAdapter(requireContext(), android.R.layout.simple_spinner_dropdown_item, genders)
        genderDropdown.adapter = adapter

        lookingForDropdown.adapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_spinner_dropdown_item,
            lookingForArray
        )

        var selectedGender = ""
        var lookingFor = ""
        genderDropdown.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parent: AdapterView<*>?,
                view: View?,
                position: Int,
                id: Long
            ) {
                selectedGender = genders[position]
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {
                // Handle the case when nothing is selected (optional)
            }
        }
        lookingForDropdown.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parent: AdapterView<*>?,
                view: View?,
                position: Int,
                id: Long
            ) {
                lookingFor = lookingForArray[position]
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {
                // Handle the case when nothing is selected (optional)
            }
        }
        val prevGender = arguments?.getString("gender")
        val prevLookingFor = arguments?.getString("lookingFor")
        if (prevGender != null) {
            val genderPosition = genders.indexOf(prevGender)
            if (genderPosition != -1) {
                genderDropdown.setSelection(genderPosition)
            }
        }

        if (prevLookingFor != null) {
            val lookingForPosition = lookingForArray.indexOf(prevLookingFor)
            if (lookingForPosition != -1) {
                lookingForDropdown.setSelection(lookingForPosition)
            }
        }
        locationEditText.setText(arguments?.getString("location"))
        hometownEditText.setText(arguments?.getString("hometown"))

        // Set click listener for update button
        updateButton.setOnClickListener {
            val location = locationEditText.text.toString()
            val hometown = hometownEditText.text.toString()
            // Send data back to the activity
            if (updateListener != null) {
                updateListener!!.onUpdate(selectedGender, lookingFor, location, hometown)
            }
            // Close the bottom sheet fragment
            dismiss()
        }
        cancelButton.setOnClickListener { dismiss() }
        return view
    }

}
