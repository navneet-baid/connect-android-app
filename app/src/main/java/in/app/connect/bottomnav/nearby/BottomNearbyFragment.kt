package `in`.app.connect.bottomnav.nearby

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.firebase.database.ChildEventListener
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import `in`.app.connect.MainActivity
import `in`.app.connect.R
import `in`.app.connect.bottomnav.globalprofile.UserProfile
import `in`.app.connect.bottomnav.globalprofile.UserProfileAdapter
import `in`.app.connect.usermanagment.models.UserData
import `in`.app.connect.utils.SessionManager
import java.util.HashMap
import kotlin.math.abs

class BottomNearbyFragment : Fragment() , MainActivity.RefreshableFragment {
    override fun refreshContent() {
        fetchPreferredDistanceFromDatabase()
    }
    private var preferredDistance = 0
    private var userRegisteredNumber = ""
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var userProfileRecycler: RecyclerView // Reference your RecyclerView here
    private lateinit var database: DatabaseReference // Reference to your Firebase Database
    private var userProfiles = mutableListOf<UserProfile>()

    private val LOCATION_PERMISSION_REQUEST_CODE = 121
    private lateinit var sessionManager: SessionManager
    private lateinit var userDetails: HashMap<String, Any>
    private lateinit var noUsersTextView: TextView
    private lateinit var lastKnownLocation: Location

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_bottom_nearby, container, false)

        userProfileRecycler =
            view.findViewById(R.id.userProfileRecycler) // Reference your RecyclerView here
        database = FirebaseDatabase.getInstance().getReference("Users")
        sessionManager = SessionManager(requireContext())
        userDetails = sessionManager.getUserDetailFromSession()
        preferredDistance = userDetails[sessionManager.KEY_DISTANCE].toString().toInt()
        userRegisteredNumber = userDetails[sessionManager.KEY_PHONENUMBER].toString()
        noUsersTextView = view.findViewById(R.id.noUsersTextView)
        fetchPreferredDistanceFromDatabase()

        return view
    }
    private fun fetchPreferredDistanceFromDatabase() {
        val userDistanceRef = FirebaseDatabase.getInstance().getReference("Users")
            .child(userRegisteredNumber).child("distance")// Adjust this based on your database structure

        userDistanceRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                preferredDistance = snapshot.getValue(Int::class.java) ?: 0
                fetchUserLocationAndProfiles()
            }

            override fun onCancelled(error: DatabaseError) {
                // Handle the error if necessary
            }
        })
    }
    private fun fetchUserLocationAndProfiles() {
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireContext())

        // Check if location permission is granted
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

        // Retrieve the last known location
        fusedLocationClient.lastLocation.addOnSuccessListener { location: Location? ->
            if (location != null) {
                // Use the new location
                val userLatitude = location.latitude
                val userLongitude = location.longitude
                saveLocationToSharedPreferences(location)
                fetchUserProfiles(userLatitude, userLongitude)
            } else {
                // Use the last known location from SharedPreferences
                val lastKnownLocation = retrieveLocationFromSharedPreferences()
                val userLatitude = lastKnownLocation.latitude
                val userLongitude = lastKnownLocation.longitude
                fetchUserProfiles(userLatitude, userLongitude)
            }
        }

    }
    private fun saveLocationToSharedPreferences(location: Location) {
        val sharedPreferences = requireContext().getSharedPreferences("MyPrefs", Context.MODE_PRIVATE)
        val editor = sharedPreferences.edit()
        editor.putFloat("lastKnownLatitude", location.latitude.toFloat())
        editor.putFloat("lastKnownLongitude", location.longitude.toFloat())
        editor.apply()
        val databaseReference = FirebaseDatabase.getInstance().reference

        val phoneNumber = userDetails[sessionManager.KEY_PHONENUMBER].toString() // Replace with the actual phone number

        val userRef = databaseReference.child("Users").child(phoneNumber)
        userRef.child("lat").setValue(location.latitude.toFloat())
        userRef.child("long").setValue(location.longitude.toFloat())

    }

    private fun retrieveLocationFromSharedPreferences(): Location {
        val sharedPreferences = requireContext().getSharedPreferences("MyPrefs", Context.MODE_PRIVATE)
        val latitude = sharedPreferences.getFloat("lastKnownLatitude", 0.0f).toDouble()
        val longitude = sharedPreferences.getFloat("lastKnownLongitude", 0.0f).toDouble()

        val location = Location("LastKnownLocation")
        location.latitude = latitude
        location.longitude = longitude
        return location
    }

    private fun requestLocationPermission() {
        val locationPermission = Manifest.permission.ACCESS_FINE_LOCATION
        val isLocationPermissionGranted = PackageManager.PERMISSION_GRANTED ==
                ContextCompat.checkSelfPermission(requireContext(), locationPermission)

        if (!isLocationPermissionGranted) {
            requestPermissions(arrayOf(locationPermission), LOCATION_PERMISSION_REQUEST_CODE)
        } else {
            fetchUserLocationAndProfiles()
        }
    }

    private fun fetchUserProfiles(userLatitude: Double, userLongitude: Double) {
        userProfiles= mutableListOf()
        database.addChildEventListener(object : ChildEventListener {
            override fun onChildAdded(snapshot: DataSnapshot, previousChildName: String?) {
                val phoneNumber = snapshot.key
                if (phoneNumber != userRegisteredNumber) { // Check if the key matches the registered phone number
                    val userData = snapshot.getValue(UserData::class.java)
                    if (userData != null) {
                        val userLat = userData.lat // Replace 'lat' with your actual latitude field
                        val userLong =
                            userData.long // Replace 'long' with your actual longitude field
                        val distance = calculateDistance(
                            userLat, userLong,
                            userLatitude, userLongitude
                        )
                        if (abs(preferredDistance) >= distance) {
                            userProfiles.add(UserProfile(phoneNumber!!, userData))
                            userProfileRecycler.adapter?.notifyDataSetChanged()
                        }
                        if (userProfiles.isEmpty()) {
                            noUsersTextView.visibility = View.VISIBLE
                        } else {
                            noUsersTextView.visibility = View.GONE
                        }
                    }
                }
            }

            override fun onChildChanged(snapshot: DataSnapshot, previousChildName: String?) {
                // Handle changes if needed
                // For example, update the user profile in the list
                val phoneNumber = snapshot.key
                val userData = snapshot.getValue(UserData::class.java)
                if (phoneNumber != userRegisteredNumber && userData != null) {
                    val updatedIndex = userProfiles.indexOfFirst { it.phoneNumber == phoneNumber }
                    if (updatedIndex != -1) {
                        userProfiles[updatedIndex] = UserProfile(phoneNumber!!, userData)
                        userProfileRecycler.adapter?.notifyItemChanged(updatedIndex)
                    }
                }
            }

            override fun onChildRemoved(snapshot: DataSnapshot) {
                // Handle removals if needed
                val phoneNumber = snapshot.key
                val removedIndex = userProfiles.indexOfFirst { it.phoneNumber == phoneNumber }
                if (removedIndex != -1) {
                    userProfiles.removeAt(removedIndex)
                    userProfileRecycler.adapter?.notifyItemRemoved(removedIndex)
                }
            }

            override fun onChildMoved(snapshot: DataSnapshot, previousChildName: String?) {
                // Handle moves if needed
                // Not typically relevant in this context
            }

            override fun onCancelled(error: DatabaseError) {
                // Handle errors if needed
                // For example, show an error message
            }
        })

        // Set up your RecyclerView adapter using the userProfiles list
        val adapter = UserProfileAdapter(requireContext(), userProfiles) // Create this adapter
        val layoutManager = GridLayoutManager(requireContext(), 2)
        layoutManager.spanCount = 2 // This ensures 2 columns in the grid
        layoutManager.reverseLayout = false // Set this to true if you want to reverse the layout
        userProfileRecycler.layoutManager = layoutManager
        userProfileRecycler.adapter = adapter
    }

    private fun calculateDistance(
        lat1: Double, lon1: Double,
        lat2: Double, lon2: Double
    ): Double {
        val R = 6371.0 // Earth's radius in kilometers

        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)

        val a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
                Math.sin(dLon / 2) * Math.sin(dLon / 2)

        val c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a))
        return R * c
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permission granted, fetch user location and profiles
                fetchUserLocationAndProfiles()
            } else {
                // Location permission denied, handle accordingly (e.g., show a message)
            }
        }
    }

}
