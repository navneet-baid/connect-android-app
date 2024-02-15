package `in`.app.connect.usermanagment

import android.Manifest
import android.content.pm.PackageManager
import android.location.Location
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.material.slider.RangeSlider
import `in`.app.connect.R
import `in`.app.connect.usermanagment.models.DistanceData

class DistancePreferenceFragment : Fragment() {
    private lateinit var rangeSlider: RangeSlider
    private lateinit var distanceSelectedTextView: TextView
    private lateinit var nextButton: Button
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private val LOCATION_PERMISSION_REQUEST_CODE = 121

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_distance_prefrence, container, false)

        rangeSlider = view.findViewById(R.id.rangeSlider)
        distanceSelectedTextView = view.findViewById(R.id.distanceSelected)

        distanceSelectedTextView.text = "${rangeSlider.values[0].toInt()}KM"

        rangeSlider.addOnChangeListener { slider, _, _ ->
            val value = slider.values[0].toInt()
            distanceSelectedTextView.text = "$value KM"
        }

        val activity = requireActivity() as? RegisterUser
        activity?.let {
            nextButton = it.findViewById(R.id.btnNext)
        }

        nextButton.setOnClickListener {
            val userDataListener = requireActivity() as? UserDataListener
            userDataListener?.onDistanceDataReceived(DistanceData(rangeSlider.values[0].toInt()),latitude,longitude)
            nextButton.performClick()
        }

        showLocationPermissionDialog()

        return view
    }

    private fun showLocationPermissionDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_location_permission, null)
        val dialogButton = dialogView.findViewById<Button>(R.id.dialogButton)

        val dialog = AlertDialog.Builder(requireContext())
            .setView(dialogView)
            .setCancelable(false)
            .create()

        dialogButton.setOnClickListener {
            dialog.dismiss()
            requestLocationPermission()
        }

        dialog.show()
    }

    private fun requestLocationPermission() {
        val locationPermission = Manifest.permission.ACCESS_FINE_LOCATION
        val isLocationPermissionGranted = PackageManager.PERMISSION_GRANTED ==
                ContextCompat.checkSelfPermission(requireContext(), locationPermission)

        if (!isLocationPermissionGranted) {
            requestPermissions(arrayOf(locationPermission), LOCATION_PERMISSION_REQUEST_CODE)
        } else {
            fetchAndStoreLocation()
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                fetchAndStoreLocation()
            } else {
                // Location permission denied, handle accordingly (e.g., show a message)
            }
        }
    }
    private var latitude=0.0
    private var longitude=0.0

    private fun fetchAndStoreLocation() {
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireContext())
        if (ActivityCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            requestLocationPermission()
            return
        }
        fusedLocationClient.lastLocation.addOnSuccessListener { location: Location? ->
            if (location != null) {
                latitude = location.latitude
                longitude = location.longitude
            }
        }
    }
}
