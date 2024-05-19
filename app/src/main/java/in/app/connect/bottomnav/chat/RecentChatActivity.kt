package `in`.app.connect.bottomnav.chat

import android.os.Bundle
import android.view.MenuItem
import android.view.MotionEvent
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SearchView
import androidx.appcompat.widget.Toolbar
import `in`.app.connect.R
import `in`.app.connect.utils.ConnectAppApplication
import android.view.GestureDetector
import android.view.Menu
import android.view.View
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator

class RecentChatActivity : AppCompatActivity() {
    private var toolbar: Toolbar? = null
//    private lateinit var searchView: SearchView
    private lateinit var gestureDetector: GestureDetector
    private lateinit var viewPager: ViewPager2
    private lateinit var tabLayout: TabLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        (application as ConnectAppApplication).startAppUsageTracking()

        setContentView(R.layout.activity_recent_chat)

        toolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true) // Show back arrow

        toolbar?.setNavigationOnClickListener {
            onBackPressed() // Handle back arrow click
        }

        gestureDetector = GestureDetector(this, GestureListener())

        // Initialize ViewPager2 and TabLayout
        viewPager = findViewById(R.id.viewPager)
        tabLayout = findViewById(R.id.tabLayout)

        val adapter = TabPagerAdapter(this)
        viewPager.adapter = adapter

        TabLayoutMediator(tabLayout, viewPager) { tab, position ->
            tab.text = when (position) {
                0 -> "All Chats"
                else -> ""
            }
        }.attach()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_recent_chat, menu)
//        val searchItem = menu.findItem(R.id.action_search)
//        searchView = searchItem.actionView as SearchView
//
//        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
//            override fun onQueryTextSubmit(query: String?): Boolean {
//                // Handle query submission here
//                return true
//            }
//
//            override fun onQueryTextChange(newText: String?): Boolean {
//                // Handle query text change here
//                return true
//            }
//        })

        return true
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
            if (diffX > SWIPE_THRESHOLD && Math.abs(velocityX) > SWIPE_VELOCITY_THRESHOLD) {
                onBackPressed() // Close the activity and return to the previous one
            }
            return true
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        (application as ConnectAppApplication).stopAppUsageTracking()
    }

    inner class TabPagerAdapter(activity: AppCompatActivity) : FragmentStateAdapter(activity) {
        override fun getItemCount(): Int = 1

        override fun createFragment(position: Int): Fragment {
            return when (position) {
                0 ->AllChats()
                else -> Fragment()
            }
        }
    }
}
