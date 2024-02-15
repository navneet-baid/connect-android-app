package `in`.app.connect.usermanagment

import android.app.Activity.RESULT_OK
import android.content.Intent
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.Toast
import androidx.cardview.widget.CardView
import com.google.android.material.card.MaterialCardView
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import `in`.app.connect.R
import `in`.app.connect.usermanagment.models.ImagesData
import java.io.IOException

class RecentPicturesFragment : Fragment() {
    private lateinit var cardView: MaterialCardView
    private lateinit var imageView: ImageView
    private lateinit var icon: ImageView
    private lateinit var nextButton: Button
    private var selectedPicture: Uri? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        val view = inflater.inflate(R.layout.fragment_recent_pictures, container, false)

        // Initialize UI elements
        cardView = view.findViewById(R.id.cardView1)
        imageView = view.findViewById(R.id.imageView1)
        icon = view.findViewById(R.id.icon1)

        // Set click listeners for the card view and the icon
        cardView.setOnClickListener {
            openGalleryForPictureSelection()
        }
        icon.setOnClickListener {
            openGalleryForPictureSelection()
        }

        // Find the Next button from the parent activity
        val activity = requireActivity() as? RegisterUser
        activity?.let {
            nextButton = it.findViewById(R.id.btnNext)
        }
        nextButton.setOnClickListener {
            if (selectedPicture != null) {
                val userDataListener = requireActivity() as? UserDataListener
                userDataListener?.onImagesDataReceived(ImagesData(arrayOf(selectedPicture!!)))
                nextButton.performClick()
            } else {
                Toast.makeText(
                    requireContext(),
                    "Please select a picture.",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }

        return view
    }

    private fun openGalleryForPictureSelection() {
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        startActivityForResult(intent, REQUEST_CODE_IMAGE_SELECTION)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == RESULT_OK && data != null) {
            val selectedImageUri: Uri = data.data ?: return
            // Set the selected image to the ImageView
            try {
                val inputStream = requireContext().contentResolver.openInputStream(selectedImageUri)
                val bitmap = BitmapFactory.decodeStream(inputStream)
                inputStream?.close()
                imageView.setImageBitmap(bitmap)
                selectedPicture = selectedImageUri
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }
    }

    companion object {
        private const val REQUEST_CODE_IMAGE_SELECTION = 1
    }
}
