package `in`.app.connect.bottomnav.userprofile


import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.ImageButton
import android.widget.ImageView
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import `in`.app.connect.R
import `in`.app.connect.utils.ConnectAppApplication

class PicturePopUpActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        (application as ConnectAppApplication).startAppUsageTracking()

        setContentView(R.layout.activity_picture_pop_up)
        val close = findViewById<ImageView>(R.id.ic_close)
        val imageUrl = intent.getStringExtra("imageUrl")
        close.setOnClickListener { finish() }
        val photoView: ImageView = findViewById(R.id.photo_view)
        Glide.with(this)
            .load(imageUrl)
            .apply(RequestOptions().fitCenter())
            .into(photoView)
    }

    override fun onBackPressed() {
        super.onBackPressed()
        finish()
    }

    override fun onDestroy() {
        super.onDestroy()
        (application as ConnectAppApplication).stopAppUsageTracking()
    }
}