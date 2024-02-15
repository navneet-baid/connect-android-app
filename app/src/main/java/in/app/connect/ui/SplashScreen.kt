package `in`.app.connect.ui

import android.app.ActivityOptions
import android.content.Intent
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.graphics.Color
import android.media.tv.TvContract
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.view.Window
import android.view.WindowManager
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.ktx.Firebase
import `in`.app.connect.MainActivity
import `in`.app.connect.R
import `in`.app.connect.authentication.LoginActivity
import `in`.app.connect.utils.SessionManager
import `in`.destinytours.app.utils.RootUtil



class SplashScreen : AppCompatActivity() {
    private lateinit var slogan: TextView
    private lateinit var appVersionText: TextView
    private lateinit var bottomAnim: Animation
    private val splashTimeOut: Long = 2000 // 2.0 seconds
    private lateinit var sessionManager: SessionManager
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash_screen)

        if (RootUtil.isDeviceRooted) {
            Toast.makeText(this, "This app does not run on rooted devices.", Toast.LENGTH_LONG)
                .show()
            finish()
            return
        }
        val window: Window = window
        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
        window.decorView.systemUiVisibility =
            (View.SYSTEM_UI_FLAG_LAYOUT_STABLE or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN)
        window.statusBarColor = Color.TRANSPARENT
        this.window.decorView.systemUiVisibility =
            (View.SYSTEM_UI_FLAG_LAYOUT_STABLE or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY)

        setContentView(R.layout.activity_splash_screen)

        bottomAnim = AnimationUtils.loadAnimation(this, R.anim.bottom_animation)
        slogan = findViewById(R.id.Slogan)
        appVersionText = findViewById(R.id.appVersionText)
        val packageManager: PackageManager = packageManager
        val packageName: String = packageName

        try {
            val packageInfo: PackageInfo = packageManager.getPackageInfo(packageName, 0)
            val versionName: String = packageInfo.versionName
            appVersionText.text="V.$versionName"

            // Now you can use versionName and versionCode as needed
        } catch (e: PackageManager.NameNotFoundException) {
            e.printStackTrace()
        }

        //slogan bottom animation
        //slogan.animation = bottomAnim
        val logoImageView = findViewById<ImageView>(R.id.Logo)

        // Apply the animation to the logoImageView
        val animation = AnimationUtils.loadAnimation(this, R.anim.bottom_animation)
        logoImageView.startAnimation(animation)
        sessionManager = SessionManager(this@SplashScreen)
        Handler(Looper.getMainLooper()).postDelayed({
            if (sessionManager.checkLogin()) {
                // Start the MainActivity after the delay
                startActivity(Intent(this, MainActivity::class.java))
                // Finish the Splash Screen activity so the user cannot return to it
                finish()
            } else {
                // Start the LoginActivity after the delay
                startActivity(Intent(this, LoginActivity::class.java))
                // Finish the Splash Screen activity so the user cannot return to it
                finish()
            }
        }, splashTimeOut)
    }

    override fun onBackPressed() {
        super.onBackPressed()
        finish()
    }
}

