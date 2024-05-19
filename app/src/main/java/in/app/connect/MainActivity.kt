package `in`.app.connect

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.view.GestureDetector
import android.view.Menu
import android.view.MenuItem
import android.view.MotionEvent
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.view.GestureDetectorCompat
import androidx.fragment.app.Fragment
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import `in`.app.connect.bottomnav.Blog.BottomBlogFragment
import `in`.app.connect.bottomnav.chat.RecentChatActivity
import `in`.app.connect.bottomnav.globalprofile.BottomGlobalProfileFragment
import `in`.app.connect.bottomnav.nearby.BottomNearbyFragment
import `in`.app.connect.bottomnav.userprofile.BottomUserProfileFragment
import `in`.app.connect.utils.ConnectAppApplication
import `in`.app.connect.utils.SessionManager
import kotlin.math.abs


class MainActivity : AppCompatActivity() {
    private lateinit var bottomNavigationView: BottomNavigationView
    private var activeFragment: Fragment? = null
    private lateinit var fab: FloatingActionButton
    private lateinit var gestureDetector: GestureDetectorCompat
    private lateinit var toolbar: Toolbar
    private lateinit var chatIcon: MenuItem
    private lateinit var notificationIcon: MenuItem
    private lateinit var optionsMenu: MenuItem
    private var hasNotificationPermissionGranted = false
    private lateinit var swipeRefreshLayout: SwipeRefreshLayout
    lateinit var sessionManager: SessionManager
    private var userDetails: HashMap<String, Any> = HashMap()
    private val notificationPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            hasNotificationPermissionGranted = isGranted
            if (!isGranted) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    if (Build.VERSION.SDK_INT >= 33) {
                        if (shouldShowRequestPermissionRationale(Manifest.permission.POST_NOTIFICATIONS)) {
                            showNotificationPermissionRationale()
                        } else {
                            showSettingDialog()
                        }
                    }
                }
            }
        }

    @SuppressLint("PrivateResource")
    private fun showNotificationPermissionRationale() {

        MaterialAlertDialogBuilder(
            this,
            com.google.android.material.R.style.MaterialAlertDialog_MaterialComponents
        )
            .setTitle("Alert")
            .setMessage("Notification permission is required, to show notification")
            .setPositiveButton("Ok") { _, _ ->
                if (Build.VERSION.SDK_INT >= 33) {
                    notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    @SuppressLint("PrivateResource")
    private fun showSettingDialog() {
        MaterialAlertDialogBuilder(
            this,
            com.google.android.material.R.style.MaterialAlertDialog_MaterialComponents
        )
            .setTitle("Notification Permission")
            .setMessage("Notification permission is required, Please allow notification permission from setting")
            .setPositiveButton("Ok") { _, _ ->
                val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                intent.data = Uri.parse("package:$packageName")
                startActivity(intent)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        (application as ConnectAppApplication).startAppUsageTracking()
        swipeRefreshLayout = findViewById(R.id.swipeRefreshLayout)

        bottomNavigationView = findViewById(R.id.bottomNavigationView)
        fab = findViewById(R.id.fab)
        toolbar = findViewById(R.id.toolbar)
        setUpActionBar()
        if (Build.VERSION.SDK_INT >= 33) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            hasNotificationPermissionGranted = true
        }
        bottomNavigationView.background = null
        fab.background = null

        gestureDetector = GestureDetectorCompat(this, GestureListener())

        bottomNavigationView.setOnNavigationItemSelectedListener { menuItem ->
            val selectedFragment: Fragment = when (menuItem.itemId) {
                R.id.menu_global_profiles -> {
                    BottomGlobalProfileFragment()
                }

                R.id.menu_nearby -> BottomNearbyFragment()
                R.id.menu_blog -> BottomBlogFragment()
                R.id.menu_user_profile -> BottomUserProfileFragment()
                else -> throw IllegalArgumentException("Invalid menu item ID")
            }
            replaceFragment(selectedFragment)
            true
        }

        // Set the initial fragment based on the first menu item
        bottomNavigationView.selectedItemId = R.id.menu_global_profiles

        fab.setOnClickListener {
            // Launch a new activity
            val intent = Intent(this, BlogWriter::class.java)
            startActivity(intent)
        }
        // Initialize the SwipeRefreshLayout
        toggleSwipeRefresh()

        swipeRefreshLayout.setOnRefreshListener {
            // Perform the refresh action here
            refreshCurrentFragment()
            invalidateMenu()
        }
        sessionManager = SessionManager(this@MainActivity)
        userDetails = sessionManager.getUserDetailFromSession()
    }

    // Function to enable or disable SwipeRefreshLayout
    private fun toggleSwipeRefresh() {
        swipeRefreshLayout.isEnabled = activeFragment !is BottomBlogFragment
    }

    interface RefreshableFragment {
        fun refreshContent()
    }

    private fun refreshCurrentFragment() {
        // Get the current active fragment and refresh its content
        val currentFragment = activeFragment
        if (currentFragment is RefreshableFragment) {
            currentFragment.refreshContent()
        }

        // Complete the swipe-to-refresh animation
        swipeRefreshLayout.isRefreshing = false
    }

    fun checkLastNotificationIsUnread(userPhoneNumber: String) {
        // Reference to the notifications node for the specific user
        val notificationsRef = FirebaseDatabase.getInstance().reference
            .child("notifications")
            .child(userPhoneNumber)

        // Query the last notification and check its 'isUnread' status
        notificationsRef.orderByChild("date").limitToLast(1)
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(dataSnapshot: DataSnapshot) {
                    if (dataSnapshot.exists()) {
                        // Get the last notification
                        val isUnread = dataSnapshot.children.first().child("isUnread")
                            .getValue(Boolean::class.java)
                        if (isUnread != null) {
                            // Check if the last notification is unread
                            if (isUnread) {
                                // Display the red dot
                                showRedDot()
                            } else {
                                // Hide the red dot
                                hideRedDot()
                            }
                        }
                    }
                }

                override fun onCancelled(databaseError: DatabaseError) {
                    // Handle any errors that might occur during the fetch
                }
            })
    }

    var isUnread = false
    fun showRedDot() {
        val redDot: View? = findViewById(R.id.red_dot)
        redDot?.visibility = View.VISIBLE
        isUnread = true
    }


    fun hideRedDot() {
        val redDot: View? = findViewById(R.id.red_dot)
        redDot?.visibility = View.GONE
        isUnread = false
    }

    override fun onPrepareOptionsMenu(menu: Menu?): Boolean {
        checkLastNotificationIsUnread(userDetails[sessionManager.KEY_PHONENUMBER].toString())

        val item = menu?.findItem(R.id.menu_notification)

        if (item != null) {
            val redDotView = item.actionView!!.findViewById<View>(R.id.red_dot)
            val notificationIcon = item.actionView!!.findViewById<View>(R.id.menu_icon)

            // Update the visibility of the red dot based on your app's logic
            if (isUnread) {
                redDotView.visibility = View.VISIBLE
            } else {
                redDotView.visibility = View.GONE
            }
            notificationIcon.setOnClickListener {
                startActivity(Intent(this@MainActivity, NotificationActivity::class.java))
            }

        }

        return super.onPrepareOptionsMenu(menu)
    }

    private fun setUpActionBar() {
        setSupportActionBar(toolbar)
        supportActionBar?.title = "Connect App"
        val menu = toolbar.menu
        chatIcon = menu.findItem(R.id.menu_chat)
        notificationIcon = menu.findItem(R.id.menu_notification)
        optionsMenu = menu.findItem(R.id.menu_options)
        // Set the initial visibility based on the first fragment
        chatIcon.isVisible = true
        notificationIcon.isVisible = true
        optionsMenu.isVisible = false
    }

    private fun replaceFragment(fragment: Fragment) {
        if (fragment::class.java == activeFragment?.javaClass) {
            return
        }

        supportFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, fragment)
            .commitNow()

        setToolbarTitle(fragment) // Set the toolbar title when replacing the fragment
        activeFragment = fragment

        // Call the setSelectedItem function to update the selected item and icons
        when (fragment) {
            is BottomGlobalProfileFragment -> setSelectedItem(R.id.menu_global_profiles)
            is BottomNearbyFragment -> setSelectedItem(R.id.menu_nearby)
            is BottomBlogFragment -> setSelectedItem(R.id.menu_blog)
            is BottomUserProfileFragment -> setSelectedItem(R.id.menu_user_profile)
        }
        toggleSwipeRefresh()
    }


    private fun setSelectedItem(itemId: Int) {
        // Set the selected item in the BottomNavigationView
        bottomNavigationView.selectedItemId = itemId
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        gestureDetector.onTouchEvent(event)
        return super.onTouchEvent(event)
    }

    private inner class GestureListener : GestureDetector.SimpleOnGestureListener() {
        private val SWIPE_THRESHOLD = 100
        private val SWIPE_VELOCITY_THRESHOLD = 100

        override fun onFling(
            e1: MotionEvent?,
            e2: MotionEvent,
            velocityX: Float,
            velocityY: Float
        ): Boolean {
            val diffX = e2.x - e1!!.x
            val isGlobalProfileFragment = activeFragment is BottomGlobalProfileFragment
            val isNearbyFragment = activeFragment is BottomNearbyFragment

            if ((isGlobalProfileFragment || isNearbyFragment) &&
                diffX < -SWIPE_THRESHOLD &&
                abs(velocityX) > SWIPE_VELOCITY_THRESHOLD
            ) {
                openChatActivity()
            }

            return true
        }
    }

    private fun openChatActivity() {
        val intent = Intent(this@MainActivity, RecentChatActivity::class.java)
        startActivity(intent)
    }

    private fun setToolbarTitle(fragment: Fragment) {
        val title: String

        when (fragment) {
            is BottomGlobalProfileFragment -> {
                title = "Global Profiles"
            }

            is BottomNearbyFragment -> {
                title = "Nearby"
            }

            is BottomBlogFragment -> {
                title = "Blogs"
            }

            is BottomUserProfileFragment -> {
                title = "My Profile"
            }

            else -> {
                title = "Connect App"
            }
        }

        toolbar.title = title
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.toolbar_options_menu, menu)

        // Find the menu items for chat, notification, and options
        chatIcon = menu.findItem(R.id.menu_chat)
        notificationIcon = menu.findItem(R.id.menu_notification)
        optionsMenu = menu.findItem(R.id.menu_options)
        notificationIcon.setOnMenuItemClickListener {
            println("menu_notification item clicked") // Add this line for debugging
            true
        }
        // Update the visibility based on the active fragment
        updateMenuItemsVisibility(activeFragment)

        return true
    }

    private fun updateMenuItemsVisibility(fragment: Fragment?) {
        when (fragment) {
            is BottomGlobalProfileFragment, is BottomNearbyFragment, is BottomBlogFragment -> {
                chatIcon.isVisible = true
                notificationIcon.isVisible = true
                optionsMenu.isVisible = false
            }

            is BottomUserProfileFragment -> {
                chatIcon.isVisible = false
                notificationIcon.isVisible = false
                optionsMenu.isVisible = true
            }

            else -> {
                chatIcon.isVisible = false
                notificationIcon.isVisible = false
                optionsMenu.isVisible = false
            }
        }
    }


    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.menu_chat -> {
                openChatActivity()
                return true
            }

            R.id.menu_notification -> {
                println("menu_notification item clicked") // Add this line for debugging
                startActivity(Intent(this@MainActivity, NotificationActivity::class.java))
                return true
            }

            R.id.menu_options -> {
                val bottomSheetFragment = BottomUserProfileFragment.BottomSheetMenuFragment()
                bottomSheetFragment.show(supportFragmentManager, bottomSheetFragment.tag)
                return true
            }

        }
        return super.onOptionsItemSelected(item)
    }

    private var lastBackPressTime: Long = 0

    override fun onBackPressed() {
        // Handle the logic based on the current active fragment
        when (activeFragment) {
            is BottomGlobalProfileFragment -> {
                // Get the current time
                val currentTime = System.currentTimeMillis()
                // Check if the time difference between the current and last back button press is less than 2000 milliseconds (2 seconds)
                if (currentTime - lastBackPressTime < 2000) {
                    super.onBackPressed() // If it's a double-press, perform the regular back action
                } else {
                    Toast.makeText(
                        this@MainActivity,
                        "Press back again to exit.",
                        Toast.LENGTH_SHORT
                    ).show()
                }
                lastBackPressTime = currentTime // Update the last back button press time

            }

            else -> replaceFragment(BottomGlobalProfileFragment()) // Perform regular back action for other fragments
        }

    }

    override fun onDestroy() {
        super.onDestroy()
        (application as ConnectAppApplication).stopAppUsageTracking()
    }
}
