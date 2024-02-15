package     `in`.app.connect.utils

import android.app.Service
import android.content.Intent
import android.os.IBinder
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.HashMap
import java.util.Locale

class UsageTrackingService : Service() {
    private var startTime: Long = 0
    private var isTracking = false
    lateinit var sessionManager: SessionManager
    private var userDetails: HashMap<String, Any> = HashMap()
    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onCreate() {
        super.onCreate()
        sessionManager = SessionManager(this@UsageTrackingService)
        userDetails = sessionManager.getUserDetailFromSession()
        startTimer()
    }

    // Add logs in other methods as needed

    override fun onDestroy() {
        super.onDestroy()
        stopTimer()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return START_STICKY
    }

    private fun startTimer() {
        println(isTracking)
        if (!isTracking) {
            startTime = System.currentTimeMillis()
            isTracking = true
        }
    }

    private fun stopTimer() {
        println(isTracking)
        if (isTracking) {
            val usageTimeInSeconds = (System.currentTimeMillis() - startTime) / 1000
            val userPhoneNumber = userDetails[sessionManager.KEY_PHONENUMBER].toString()
            saveUsageDataToFirebase(userPhoneNumber, usageTimeInSeconds)
            isTracking = false
        }
    }

    private fun saveUsageDataToFirebase(userPhoneNumber: String, usageTimeInSeconds: Long) {
        val database = FirebaseDatabase.getInstance()
        val calendar = Calendar.getInstance()
        val dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK)
        val hourOfDay = calendar.get(Calendar.HOUR_OF_DAY)
        val currentWeekNumber = calendar.get(Calendar.WEEK_OF_YEAR)

        val currentWeekRef = database.getReference("AppUsage/$userPhoneNumber/$currentWeekNumber")

        // Calculate the week number for the previous week
        val previousWeekNumber = if (currentWeekNumber == 1) {
            // If the current week number is 1, it's the first week of the year, so set previousWeekNumber to the last week of the previous year
            calendar.add(Calendar.YEAR, -1)
            calendar.getActualMaximum(Calendar.WEEK_OF_YEAR)
        } else {
            currentWeekNumber - 1
        }
        val previousWeekRef = database.getReference("AppUsage/$userPhoneNumber/$previousWeekNumber")

        // Create a data structure for the current day
        val currentDay = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
        val currentHour = String.format("%02d", hourOfDay)
        val currentWeekDayRef = currentWeekRef.child(weekdayFromCalendar(dayOfWeek))
        val currentDayRef = currentWeekDayRef.child(currentDay)

        currentDayRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.exists()) {
                    // Data for the current day already exists, update the current hour's value
                    val existingData = snapshot.value as Map<*, *>
                    val existingHourValue = existingData[currentHour] as Long? ?: 0

                    // Add the new usage time to the existing hour's value
                    val updatedHourValue = existingHourValue + usageTimeInSeconds

                    // Update the value for the current hour in the database
                    currentDayRef.child(currentHour).setValue(updatedHourValue)
                } else {
                    // Data for the current day doesn't exist, create a new entry for the current hour
                    val hourData = mapOf(currentHour to usageTimeInSeconds)
                    currentDayRef.updateChildren(hourData)
                }

                // Delete data for the previous week
                previousWeekRef.removeValue()
            }

            override fun onCancelled(error: DatabaseError) {
                // Handle any errors here
            }
        })
    }

    private fun weekdayFromCalendar(dayOfWeek: Int): String {
        // Convert the Calendar.DAY_OF_WEEK value to the corresponding weekday name
        return when (dayOfWeek) {
            Calendar.MONDAY -> "Monday"
            Calendar.TUESDAY -> "Tuesday"
            Calendar.WEDNESDAY -> "Wednesday"
            Calendar.THURSDAY -> "Thursday"
            Calendar.FRIDAY -> "Friday"
            Calendar.SATURDAY -> "Saturday"
            Calendar.SUNDAY -> "Sunday"
            else -> "Unknown"
        }
    }
}
