package `in`.app.connect.usermanagment

import android.app.DatePickerDialog
import android.os.Bundle
import android.text.Editable
import android.text.InputFilter
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import androidx.fragment.app.Fragment
import `in`.app.connect.R
import `in`.app.connect.usermanagment.models.DOBData
import `in`.app.connect.usermanagment.models.NameData
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale


class BirthDaySelectionFragment : Fragment() {
    private lateinit var dobEditText:EditText
    private lateinit var nextButton: Button
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        val view=  inflater.inflate(R.layout.fragment_birth_day_selection, container, false)
        // Get a reference to the EditText for DOB selection
        dobEditText = view.findViewById(R.id.dobEditText)

        // Set a click listener on the EditText to open the DOB selector calendar
        dobEditText.setOnClickListener {
            showDatePickerDialog()
        }
        // Access the activity reference (the fragment is attached to RegisterUser activity)
        val activity = requireActivity() as? RegisterUser
        // Check if the activity is not null and the nextButton is accessible (public)
        activity?.let {
            nextButton = it.findViewById(R.id.btnNext)
        }

        nextButton.setOnClickListener {
            val dob=dobEditText.text.toString().trim()

            if(dob==""){
                dobEditText.error="DOB is required."
            }else{
                val userDataListener = requireActivity() as? UserDataListener
                userDataListener?.onDOBDataReceived(DOBData(dob))
                nextButton.performClick()
            }

        }
        return view
    }

    private fun showDatePickerDialog() {
        val calendar = Calendar.getInstance()

        val datePickerDialog = DatePickerDialog(
            requireContext(),
            { _, year, month, dayOfMonth ->
                // Handle the selected date here
                val selectedDate = formatDate(dayOfMonth, month + 1, year)
                dobEditText.setText(selectedDate)
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        )

        // Set the maximum allowed date to today
        datePickerDialog.datePicker.maxDate = System.currentTimeMillis()
        // Set the minimum allowed date to -100 years from today
        val minCal = Calendar.getInstance()
        minCal.add(Calendar.YEAR, -100)
        datePickerDialog.datePicker.minDate = minCal.timeInMillis

        datePickerDialog.show()
    }

    // Helper function to format date values to DD/MM/YYYY format
    private fun formatDate(day: Int, month: Int, year: Int): String {
        val cal = Calendar.getInstance()
        cal.set(Calendar.DAY_OF_MONTH, day)
        cal.set(Calendar.MONTH, month - 1) // Month is zero-based
        cal.set(Calendar.YEAR, year)
        val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
        return dateFormat.format(cal.time)
    }
}