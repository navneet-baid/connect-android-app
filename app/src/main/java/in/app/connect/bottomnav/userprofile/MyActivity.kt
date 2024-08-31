package `in`.app.connect.bottomnav.userprofile

import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.content.ContextCompat
import com.github.mikephil.charting.charts.BarChart
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.components.Description
import com.github.mikephil.charting.components.LimitLine
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import com.github.mikephil.charting.formatter.ValueFormatter
import com.github.mikephil.charting.highlight.Highlight
import com.github.mikephil.charting.listener.OnChartValueSelectedListener
import `in`.app.connect.R
import com.google.firebase.database.*
import `in`.app.connect.utils.ConnectAppApplication
import `in`.app.connect.utils.SessionManager
import java.util.Calendar
import java.util.HashMap
import kotlin.math.ceil

class MyActivity : AppCompatActivity() {
    private lateinit var barChart: BarChart
    private lateinit var lineChart: LineChart
    private lateinit var noDataTextView: TextView
    private lateinit var database: FirebaseDatabase
    private lateinit var userPhoneNumber: String
    lateinit var sessionManager: SessionManager
    private var userDetails: HashMap<String, Any> = HashMap()
    private var selectedDay: String? = null // Variable to store the selected day
    private lateinit var dataMap: Map<String, Any>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        (application as ConnectAppApplication).startAppUsageTracking()

        setContentView(R.layout.activity_my)
        sessionManager = SessionManager(this@MyActivity)
        userDetails = sessionManager.getUserDetailFromSession()
        barChart = findViewById(R.id.barChart)
        lineChart = findViewById(R.id.hourlyChart)
        database = FirebaseDatabase.getInstance()
        noDataTextView = findViewById(R.id.noDataTextView)

        // Replace this with the user's phone number retrieval logic
        userPhoneNumber = userDetails[sessionManager.KEY_PHONENUMBER].toString()
        // Fetch usage data from Firebase
        val calendar = Calendar.getInstance()
        val currentWeekNumber = calendar.get(Calendar.WEEK_OF_YEAR)
        fetchUsageData(currentWeekNumber)
        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)

        // Enable the back arrow for navigation
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        // Handle the back button click (optional)
        toolbar.setNavigationOnClickListener {
            onBackPressed() // You can customize this behavior as needed
        }
    }

    private fun fetchUsageData(weekNumber: Int) {
        val database = FirebaseDatabase.getInstance()
        val currentWeekRef = database.getReference("AppUsage/$userPhoneNumber/$weekNumber")

        currentWeekRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.exists()) {
                    // Process the data for the entire week
                    barChart.visibility = View.VISIBLE
                    lineChart.visibility = View.VISIBLE
                    noDataTextView.visibility = View.GONE
                    findViewById<TextView>(R.id.helperText).visibility = View.VISIBLE
                    findViewById<TextView>(R.id.dailyAverageUsageText).visibility = View.VISIBLE
                    dataMap = snapshot.value as Map<String, Any>
                    displayWeekDataInBarGraph(dataMap)
                } else {

                    // No data to display, show the "No data available" message
                    barChart.visibility = View.GONE
                    lineChart.visibility = View.GONE
                    noDataTextView.visibility = View.VISIBLE
                    findViewById<TextView>(R.id.helperText).visibility = View.GONE
                    findViewById<TextView>(R.id.dailyAverageUsageText).visibility = View.GONE

                }
            }

            override fun onCancelled(error: DatabaseError) {
                barChart.visibility = View.GONE
                lineChart.visibility = View.GONE
                noDataTextView.visibility = View.VISIBLE
            }
        })
    }

    private fun displayWeekDataInBarGraph(dataMap: Map<String, Any>) {
        // Initialize a map to store the total usage minutes for each day of the week
        val daysOfWeek =
            listOf("Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday")

        // Calculate the total usage minutes and counts for each day
        val totalMinutesMap = mutableMapOf<String, Int>()
        val dayCounts = mutableMapOf<String, Int>()

        for (day in daysOfWeek) {
            if (dataMap.containsKey(day)) {
                selectedDay = day
                val dayData = dataMap[day] as Map<String, Any>
                var totalMinutes = 0
                var count = 0

                for (dateData in dayData.values) {
                    if (dateData is Map<*, *>) {
                        for (hourData in dateData.values) {
                            if (hourData is Long) {
                                totalMinutes += (hourData / 60).toInt()
                                count++
                            }
                        }
                    }
                }

                if (totalMinutes >= 1) {
                    totalMinutesMap[day] = totalMinutes
                    dayCounts[day] = count
                }
            }
        }

        // Create a BarChart
        val barChart = findViewById<BarChart>(R.id.barChart)
        // Create a list of BarEntries with both the day of the week and usage minutes (or 0 if no data)
        val barEntries = daysOfWeek.mapIndexed { index, day ->
            val usageMinutes = totalMinutesMap[day] ?: 0
            BarEntry(index.toFloat(), usageMinutes.toFloat())
        }

        // Create a BarDataSet from the BarEntries
        val barDataSet = BarDataSet(barEntries, "Total Usage Minutes")
        barDataSet.color = ContextCompat.getColor(this, R.color.red) // Customize the color

        // Create a BarData and set the BarDataSet
        val barData = BarData(barDataSet)
        barChart.data = barData
        // Customize the appearance of the BarChart
        customizeBarChart(barChart, daysOfWeek)

        // Set a custom X-axis value formatter to include both minutes and hours labels
        setCustomXAxisValueFormatter(barChart, totalMinutesMap)

        // Calculate the total minutes for the whole week
        val totalMinutesForWeek = totalMinutesMap.values.sum()

        // Calculate the weekly average in minutes or hours
        val weeklyAverage = calculateWeeklyAverage(totalMinutesForWeek)

        // Display weekly average
        val weeklyAverageText = findViewById<TextView>(R.id.dailyAverageUsageText)
        weeklyAverageText.text = weeklyAverage

        // Add the weekly average line
        addWeeklyAverageLine(barChart, totalMinutesForWeek)
        displayHourlyChartForSelectedDay(selectedDay)
        // Refresh the chart
        barChart.invalidate()
    }

    private fun customizeBarChart(barChart: BarChart, daysOfWeek: List<String>) {
        barChart.description.isEnabled = false
        barChart.xAxis.position = XAxis.XAxisPosition.BOTTOM
        barChart.xAxis.labelCount = daysOfWeek.size
        barChart.xAxis.setDrawGridLines(false)
        barChart.axisLeft.setDrawGridLines(false)
        barChart.setDrawValueAboveBar(true)
        barChart.setFitBars(true)
        barChart.setScaleEnabled(false)
        barChart.setPinchZoom(false)
        barChart.axisRight.isEnabled = false
        barChart.legend.isEnabled = true
        barChart.legend.textSize = 12f
        // Set a listener for when a day is selected in the bar chart
        barChart.setOnChartValueSelectedListener(object : OnChartValueSelectedListener {
            override fun onValueSelected(e: Entry?, h: Highlight?) {
                if (e != null) {
                    val dayIndex = e.x.toInt()
                    if (dayIndex >= 0 && dayIndex < daysOfWeek.size) {
                        selectedDay = daysOfWeek[dayIndex]
                        // Display the hourly chart for the selected day
                        displayHourlyChartForSelectedDay(selectedDay)
                    }
                }
            }

            override fun onNothingSelected() {
                // Handle when nothing is selected (optional)
            }
        })
    }

    private fun displayHourlyChartForSelectedDay(selectedDay: String?) {
        val weekData = dataMap
        if (weekData.containsKey(selectedDay)) {
            val dayData = weekData[selectedDay] as Map<String, Any>
            for (dateData in dayData.values) {
                if (dateData is Map<*, *>) {
                    val hourlyData = dateData as Map<String, Long>
                    setupHourlyLineChart(hourlyData)
                }
            }
        } else {
            // Handle when data for the selected day doesn't exist
        }
    }

    private fun setupHourlyLineChart(hourlyData: Map<String, Long>) {
        val entries = mutableListOf<Entry>()
        val hours = mutableListOf<String>()

        // Create a map with all hours from 00 to 23, initially with 0 minutes
        val allHours = (0..23).map { String.format("%02d", it) }

        val usageByHour = allHours.associateWith { hourlyData[it]?.toFloat() ?: 0f }
        println(usageByHour)
        for ((hour, usageHours) in usageByHour) {
            val roundedUsageHours = ceil(usageHours.toDouble() / 60).toFloat()
            entries.add(Entry(hour.toFloat(), roundedUsageHours))
            hours.add(hour)
        }

        val dataSet = LineDataSet(entries, "Usage Hours")
        dataSet.color = Color.RED // Customize the line color

        val lineData = LineData(dataSet)

        lineChart.data = lineData

        // Customize the appearance of the line chart (optional)
        val xAxis = lineChart.xAxis
        xAxis.position = XAxis.XAxisPosition.BOTTOM
        xAxis.valueFormatter = IndexAxisValueFormatter(hours)
        xAxis.labelCount = hours.size // To display all labels
        xAxis.setDrawGridLines(false) // Hide X-axis grid lines

        val yAxis = lineChart.axisLeft
        yAxis.setDrawGridLines(false) // Hide Y-axis grid lines

        val description = Description()
        description.text = "Hourly Usage Chart"
        lineChart.description = description

        // Refresh the chart
        lineChart.invalidate()
    }


    private fun setCustomXAxisValueFormatter(
        barChart: BarChart,
        totalMinutesMap: Map<String, Int>
    ) {
        val daysOfWeek = totalMinutesMap.keys.toTypedArray()
        val valueFormatter = object : ValueFormatter() {
            override fun getFormattedValue(value: Float): String {
                val day = daysOfWeek.getOrNull(value.toInt()) ?: ""
                val usageMinutes = totalMinutesMap[day] ?: 0
                return if (usageMinutes < 60) {
                    "$usageMinutes min"
                } else {
                    val hours = usageMinutes / 60
                    "$hours hr"
                }
            }
        }
        barChart.xAxis.valueFormatter = valueFormatter
    }

    private fun calculateWeeklyAverage(totalMinutesForWeek: Int): String {
        val totalMinutesForWeek = (totalMinutesForWeek / 7).toInt()
        return if (totalMinutesForWeek < 59) {
            "$totalMinutesForWeek min"
        } else {
            val totalHours = totalMinutesForWeek / 60
            "$totalHours h"
        }
    }

    private fun addWeeklyAverageLine(barChart: BarChart, totalMinutesForWeek: Int) {
        // Calculate the average value
        val averageValue = totalMinutesForWeek.toFloat() / 7 // Average per day

        val limitLine = LimitLine(averageValue)
        limitLine.label = "Weekly Avg"
        limitLine.lineColor = ContextCompat.getColor(this, R.color.red)
        limitLine.lineWidth = 2f

        val leftAxis = barChart.axisLeft
        leftAxis.addLimitLine(limitLine)
    }


    override fun onDestroy() {
        super.onDestroy()
        (application as ConnectAppApplication).stopAppUsageTracking()
    }


}
