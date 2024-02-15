package `in`.app.connect.bottomnav.userprofile

import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.facebook.shimmer.ShimmerFrameLayout
import com.google.firebase.dynamiclinks.DynamicLink
import com.google.firebase.dynamiclinks.FirebaseDynamicLinks
import com.google.firebase.storage.FirebaseStorage
import com.google.zxing.BarcodeFormat
import com.google.zxing.MultiFormatWriter
import com.google.zxing.WriterException
import com.journeyapps.barcodescanner.BarcodeEncoder
import com.squareup.picasso.Picasso
import `in`.app.connect.R
import `in`.app.connect.utils.SessionManager
import java.util.HashMap



class GenerateQRFragment : Fragment() {
    lateinit var sessionManager: SessionManager
    private var userDetails: HashMap<String, Any> = HashMap()

    private lateinit var profileName: TextView

    private lateinit var profilePicture: ImageView
    private lateinit var profileShimmerLayout: ShimmerFrameLayout
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_genrate_q_r, container, false)
        sessionManager = SessionManager(requireContext())
        userDetails = sessionManager.getUserDetailFromSession()
        // Generate QR code when the fragment is created
        profileName = view.findViewById(R.id.profileName)
        profilePicture = view.findViewById(R.id.profileImage)
        profileShimmerLayout=view.findViewById(R.id.profileShimmerLayout)

        profileName.text = userDetails[sessionManager.KEY_FULLNAME].toString()
        val phoneNumber = userDetails[sessionManager.KEY_PHONENUMBER].toString() // Replace with the actual phone number
        fetchImageUrlsFromStorage(userDetails[sessionManager.KEY_PHONENUMBER].toString())
        generateDynamicLink(phoneNumber)
        return view
    }
    var qrCodeBitMap: Bitmap? = null
    var shortLink: Uri?=null
    private fun generateDynamicLink(phoneNumber: String) {
        FirebaseDynamicLinks.getInstance().createDynamicLink()
            .setLink(Uri.parse("https://connect-app.in/app/profile/${phoneNumber}"))
            .setDomainUriPrefix("https://connectblog.page.link") // Replace with your domain prefix
            .setAndroidParameters(
                DynamicLink.AndroidParameters.Builder("in.app.connect")
                    .build()
            ).setSocialMetaTagParameters(
                DynamicLink.SocialMetaTagParameters.Builder()
                    .build()
            )
            .buildShortDynamicLink()
            .addOnSuccessListener { result ->
                shortLink = result.shortLink
                qrCodeBitMap = generateQRCode(shortLink.toString())!!
                val qrCodeImageView = view?.findViewById<ImageView>(R.id.qrCodeImageView)
                qrCodeImageView?.setImageBitmap(qrCodeBitMap)            }
    }

    private fun generateQRCode(content: String): Bitmap? {
        val multiFormatWriter = MultiFormatWriter()
        return try {
            val bitMatrix = multiFormatWriter.encode(content, BarcodeFormat.QR_CODE, 300, 300)
            val barcodeEncoder = BarcodeEncoder()
            barcodeEncoder.createBitmap(bitMatrix)
        } catch (e: WriterException) {
            e.printStackTrace()
            null
        }
    }

    private fun fetchImageUrlsFromStorage(userNumber: String) {
        val storage = FirebaseStorage.getInstance()
        val storageRef = storage.getReference("RecentPicture/$userNumber")
        profileShimmerLayout.startShimmer()
        profilePicture.visibility=View.GONE
        profileShimmerLayout.visibility=View.VISIBLE

        storageRef.listAll()
            .addOnSuccessListener { listResult ->
                if (listResult.items.isNotEmpty()) {
                    val firstImageRef = listResult.items[0]
                    firstImageRef.downloadUrl.addOnSuccessListener { uri ->
                        loadImageWithGlide(uri.toString())

                    }.addOnFailureListener { exception ->
                        println("Error fetching image URL: ${exception.message}")
                        profileShimmerLayout.stopShimmer()
                    }
                } else {
                    Picasso.get()
                        .load(R.drawable.ic_user)
                        .into(profilePicture)
                    profileShimmerLayout.stopShimmer()
                }
            }
            .addOnFailureListener { exception ->
                println("Error fetching image URL: ${exception.message}")
            }
    }

    fun getQRCodeBitmap(phoneNumber: String, callback: (Bitmap?,String) -> Unit) {
        FirebaseDynamicLinks.getInstance().createDynamicLink()
            .setLink(Uri.parse("https://connect-app.in/app/profile/${phoneNumber}"))
            .setDomainUriPrefix("https://connectblog.page.link") // Replace with your domain prefix
            .setAndroidParameters(
                DynamicLink.AndroidParameters.Builder("in.app.connect")
                    .build()
            ).setSocialMetaTagParameters(
                DynamicLink.SocialMetaTagParameters.Builder()
                    .build()
            )
            .buildShortDynamicLink()
            .addOnSuccessListener { result ->
                val shortLink = result.shortLink
                val qrCodeBitmap = generateQRCode(shortLink.toString())
                callback(qrCodeBitmap,shortLink.toString())
            }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.menu_share -> {
                Toast.makeText(context, "Share", Toast.LENGTH_SHORT).show()
                return true
            }
        }
        return super.onOptionsItemSelected(item)
    }

    private fun loadImageWithGlide(imageUrl: String) {
        
        try{
            Glide.with(this)
                .load(imageUrl)
                .diskCacheStrategy(DiskCacheStrategy.ALL) // Enable caching
                .into(profilePicture)
            profileShimmerLayout.stopShimmer()
            profilePicture.visibility=View.VISIBLE
            profileShimmerLayout.visibility=View.GONE
        }catch(e:Exception){
            profileShimmerLayout.stopShimmer()
            println(e.message)
        }

    }
}
