package `in`.app.connect.bottomnav.userprofile

import ScanQRFragment
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.appcompat.widget.Toolbar
import androidx.core.content.FileProvider
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import `in`.app.connect.R
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.Lifecycle
import androidx.viewpager2.adapter.FragmentStateAdapter
import `in`.app.connect.utils.ConnectAppApplication
import `in`.app.connect.utils.SessionManager
import java.io.File
import java.io.FileOutputStream
import java.util.HashMap

class QrCodeManager : AppCompatActivity() {
    private lateinit var pagerAdapter: QRCodePagerAdapter
    private lateinit var viewPager: ViewPager2
    private lateinit var tabLayout: TabLayout
    private lateinit var shareIcon: MenuItem
    lateinit var sessionManager: SessionManager
    private var userDetails: HashMap<String, Any> = HashMap()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        (application as ConnectAppApplication).startAppUsageTracking()

        setContentView(R.layout.activity_qr_code_manager)
        viewPager = findViewById<ViewPager2>(R.id.viewPager)
        pagerAdapter = QRCodePagerAdapter(supportFragmentManager, lifecycle)
        viewPager.adapter = pagerAdapter
        tabLayout = findViewById<TabLayout>(R.id.tabLayout)
        sessionManager = SessionManager(this@QrCodeManager)
        userDetails = sessionManager.getUserDetailFromSession()
        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.title = "Profile QR Code"
        val tabs = arrayOf("Generate", "Scan")
        TabLayoutMediator(tabLayout, viewPager) { tab, position ->
            tab.text = tabs[position]
        }.attach()

    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.qr_option_menu, menu)
        shareIcon = menu.findItem(R.id.menu_share)
        viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                currentViewPagerPosition = position
                updateShareIconVisibility()
            }
        })
        return true
    }
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.menu_share -> {
                if (viewPager.currentItem == 0) {
                    val currentFragment = pagerAdapter.createFragment(viewPager.currentItem)
                    if (currentFragment is GenerateQRFragment) {
                        currentFragment.getQRCodeBitmap(userDetails[sessionManager.KEY_PHONENUMBER].toString()) { qrCodeBitmap, shortLink ->
                            if (qrCodeBitmap != null) {
                                shareQRCodeImage(qrCodeBitmap!!, shortLink)
                            }
                        }
                    }
                }
                return true
            }
        }
        return super.onOptionsItemSelected(item)
    }
    private var currentViewPagerPosition = 0
    
    private fun updateShareIconVisibility() {
        val currentFragment = pagerAdapter.createFragment(viewPager.currentItem)
        shareIcon.isVisible = currentFragment is GenerateQRFragment
    }


    // Method to share custom text along with a picture (QR code image)
    private fun shareQRCodeImage(qrCodeBitmap: Bitmap, shortLink: String) {
        // Create an intent to send the image
        val imageIntent = Intent(Intent.ACTION_SEND)
        imageIntent.type = "image/*"
        val uri = getImageUriFromBitmap(qrCodeBitmap)
        imageIntent.putExtra(Intent.EXTRA_STREAM, uri)
        // Set the custom text
        imageIntent.putExtra(Intent.EXTRA_TEXT, "Hey Connect me on Connect-App!, $shortLink")
        // Combine both the image and text intents using a Chooser
        val chooserIntent = Intent.createChooser(imageIntent, "Share Profile QR")
        startActivity(chooserIntent)
    }


    private fun getImageUriFromBitmap(bitmap: Bitmap): Uri {
        val imageFile = File(this.cacheDir, "qr_code_image.png")
        val imageUri = FileProvider.getUriForFile(this, "in.app.connect.provider", imageFile)

        try {
            val outputStream = FileOutputStream(imageFile)
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream)
            outputStream.flush()
            outputStream.close()
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return imageUri
    }

    override fun onDestroy() {
        super.onDestroy()
        (application as ConnectAppApplication).stopAppUsageTracking()
    }
}


class QRCodePagerAdapter(
    fragmentManager: FragmentManager,
    lifecycle: Lifecycle
) : FragmentStateAdapter(fragmentManager, lifecycle) {
    override fun getItemCount(): Int = 2

    override fun createFragment(position: Int): Fragment {
        return when (position) {
            0 -> GenerateQRFragment()
            1 -> ScanQRFragment()
            else -> GenerateQRFragment()
        }
    }
}
