import android.Manifest
import android.animation.ValueAnimator
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.hardware.camera2.CameraAccessException
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.view.LayoutInflater
import android.view.SurfaceHolder
import android.view.SurfaceView
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import androidx.fragment.app.Fragment
import com.google.android.gms.vision.CameraSource
import com.google.android.gms.vision.Detector
import com.google.android.gms.vision.Frame
import com.google.android.gms.vision.barcode.Barcode
import com.google.android.gms.vision.barcode.BarcodeDetector
import com.google.firebase.dynamiclinks.FirebaseDynamicLinks
import `in`.app.connect.PopupViewProfile
import `in`.app.connect.R

import java.io.IOException

class ScanQRFragment : Fragment() {
    private val REQUEST_CAMERA_PERMISSION = 1001
    private val REQUEST_GALLERY_IMAGE = 1002
    private var cameraSource: CameraSource? = null
    private var barcodeDetector: BarcodeDetector? = null
    private var cameraPreview: SurfaceView? = null
    private var phoneNumberExtracted = false
    private lateinit var scannerLineView: View
    private lateinit var galleryIcon: ImageView
    private var scannerLinePosition = 0
    private var scannerLineDirection = 1 // 1 for down, -1 for up


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_scan_q_r, container, false)
        cameraPreview = view.findViewById(R.id.cameraPreview)
        scannerLineView = view.findViewById(R.id.scannerLine)
        galleryIcon = view.findViewById(R.id.galleryIcon)

        startCamera()

        galleryIcon.setOnClickListener {
            openGallery()
        }

        return view
    }

    private fun openGallery() {
        val galleryIntent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        startActivityForResult(galleryIntent, REQUEST_GALLERY_IMAGE)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == REQUEST_GALLERY_IMAGE && resultCode == Activity.RESULT_OK) {
            val selectedImage: Uri? = data?.data

            if (selectedImage != null) {
                // Decode the selected image and check for QR codes
                checkQRCodeInGalleryImage(selectedImage)
            }
        }
    }

    private fun checkQRCodeInGalleryImage(imageUri: Uri) {
        val bitmap = decodeImageUri(imageUri)
        if (bitmap != null) {
            val barcodeDetector = BarcodeDetector.Builder(requireContext())
                .setBarcodeFormats(Barcode.QR_CODE)
                .build()

            if (barcodeDetector.isOperational) {
                val frame = Frame.Builder().setBitmap(bitmap).build()
                val qrCodes = barcodeDetector.detect(frame)

                if (qrCodes.size() > 0) {
                    val value = qrCodes.valueAt(0).displayValue

                    // Extract the phone number from the QR code link
                    FirebaseDynamicLinks.getInstance()
                        .getDynamicLink(value.toUri())
                        .addOnSuccessListener(requireActivity()) { pendingDynamicLinkData ->
                            // Get deep link from the dynamic link, if available
                            val deepLink = pendingDynamicLinkData?.link
                            if (deepLink != null) {
                                // Extract the phone number parameter from the deep link
                                val parts = deepLink.toString().split("/")
                                if (parts.size >= 6) {
                                    // The last part contains the data you're interested in
                                    val phoneNumber = parts[parts.size - 1]
                                    // Use the phone number to set up the Profile Activity
                                    if (phoneNumber.isNotEmpty()) {
                                        phoneNumberExtracted = true
                                        // Stop the scanner and animations
                                        stopScannerAndAnimations()
                                        // Start the Profile Activity with the extracted phone number
                                        val profileIntent = Intent(requireContext(), PopupViewProfile::class.java)
                                        profileIntent.putExtra("phoneNumber", phoneNumber)
                                        startActivity(profileIntent)
                                        requireActivity().finish()
                                    }
                                }
                            } else {
                                // Handle the case when the dynamic link couldn't be retrieved
                                Toast.makeText(requireContext(), "No valid QR Detected", Toast.LENGTH_SHORT).show()
                            }
                        }
                        .addOnFailureListener(requireActivity()) { e ->
                            // Handle errors if the dynamic link couldn't be retrieved
                            Toast.makeText(requireContext(), "No valid QR Detected", Toast.LENGTH_SHORT).show()
                        }
                } else {
                    // No QR code found in the image
                    Toast.makeText(requireContext(), "No QR code found in the selected image", Toast.LENGTH_SHORT).show()
                }
            } else {
                Toast.makeText(requireContext(), "Could not set up the detector", Toast.LENGTH_SHORT).show()
            }
        } else {
            Toast.makeText(requireContext(), "Failed to load image", Toast.LENGTH_SHORT).show()
        }
    }

    private fun decodeImageUri(imageUri: Uri): Bitmap? {
        return try {
            val imageStream = requireContext().contentResolver.openInputStream(imageUri)
            BitmapFactory.decodeStream(imageStream)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    var scanQr=true

    private fun startCamera() {
        if (ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(
                requireActivity(),
                arrayOf(Manifest.permission.CAMERA),
                REQUEST_CAMERA_PERMISSION
            )
            return
        }

        barcodeDetector = BarcodeDetector.Builder(requireContext())
            .setBarcodeFormats(Barcode.QR_CODE)
            .build()

        if (!barcodeDetector!!.isOperational) {
            Toast.makeText(requireContext(), "Could not set up the detector", Toast.LENGTH_SHORT)
                .show()
            return
        }

        cameraSource = CameraSource.Builder(requireContext(), barcodeDetector)
            .setRequestedPreviewSize(640, 480)
            .build()

        cameraPreview!!.holder.addCallback(object : SurfaceHolder.Callback {
            override fun surfaceCreated(holder: SurfaceHolder) {
                try {
                    if (ActivityCompat.checkSelfPermission(
                            requireContext(),
                            Manifest.permission.CAMERA
                        ) != PackageManager.PERMISSION_GRANTED
                    ) {

                        ActivityCompat.requestPermissions(
                            requireActivity(),
                            arrayOf(Manifest.permission.CAMERA),
                            REQUEST_CAMERA_PERMISSION
                        )

                        return
                    }
                    cameraSource!!.start(cameraPreview!!.holder)
                } catch (e: IOException) {
                    e.printStackTrace()
                }
            }

            override fun surfaceChanged(
                holder: SurfaceHolder,
                format: Int,
                width: Int,
                height: Int
            ) {
            }

            override fun surfaceDestroyed(holder: SurfaceHolder) {
                cameraSource!!.stop()
            }
        })

        // Call startScannerLineAnimation after the layout is measured
        cameraPreview?.viewTreeObserver?.addOnGlobalLayoutListener {
            startScannerLineAnimation()
        }
        barcodeDetector!!.setProcessor(object : Detector.Processor<Barcode> {
            override fun release() {}

            override fun receiveDetections(detections: Detector.Detections<Barcode>) {
                val qrCodes = detections.detectedItems
                if (qrCodes.size() > 0 && !phoneNumberExtracted && scanQr) {
                    val value = qrCodes.valueAt(0).displayValue
                    // Extract the phone number from the QR code link
                    scanQr=false
                    FirebaseDynamicLinks.getInstance()
                        .getDynamicLink(value.toUri())
                        .addOnSuccessListener(requireActivity()) { pendingDynamicLinkData ->
                            // Get deep link from the dynamic link, if available
                            val deepLink = pendingDynamicLinkData?.link
                            if (deepLink != null) {
                                // Extract the phone number parameter from the deep link
                                val parts = deepLink.toString().split("/")
                                if (parts.size >= 6) {
                                    // The last part contains the data you're interested in
                                    val phoneNumber = parts[parts.size - 1]
                                    // Use the phone number to set up the Profile Activity
                                    if (phoneNumber.isNotEmpty()) {
                                        scanQr=false
                                        phoneNumberExtracted = true
                                        // Stop the scanner and animations
                                        stopScannerAndAnimations()
                                        // Start the Profile Activity with the extracted phone number
                                        val profileIntent = Intent(requireContext(), PopupViewProfile::class.java)
                                        profileIntent.putExtra("phoneNumber", phoneNumber)
                                        startActivity(profileIntent)
                                        requireActivity().finish()
                                    }
                                }
                            } else {
                                scanQr=true
                                // Handle the case when the dynamic link couldn't be retrieved
                                Toast.makeText(requireContext(), "No valid QR Detected", Toast.LENGTH_SHORT).show()
                            }
                        }
                        .addOnFailureListener(requireActivity()) { e ->
                            // Handle errors if the dynamic link couldn't be retrieved
                            scanQr=true
                            Toast.makeText(requireContext(), "No valid QR Detected", Toast.LENGTH_SHORT).show()
                        }
                }
            }
        })
    }

    private fun stopScannerAndAnimations() {
        if (cameraSource != null) {
            cameraSource!!.release()
        }
        // Stop any ongoing animations or hide the scanner line view
        scannerLineView.visibility = View.GONE
    }

    private fun startScannerLineAnimation() {
        // Calculate the initial position of the scanner line
        val initialPosition = (cameraPreview?.height ?: 0) - (scannerLineView.height)

        val scannerLineAnimator = ValueAnimator.ofInt(initialPosition, 0)
        scannerLineAnimator.duration = 2000 // Adjust the animation duration as needed
        scannerLineAnimator.repeatCount = ValueAnimator.INFINITE
        scannerLineAnimator.repeatMode = ValueAnimator.REVERSE

        scannerLineAnimator.addUpdateListener { animation ->
            val animatedValue = animation.animatedValue as Int
            scannerLinePosition = animatedValue
            scannerLineView.translationY = scannerLinePosition.toFloat()
        }

        scannerLineAnimator.start()
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        if (requestCode == REQUEST_CAMERA_PERMISSION) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startCamera()
            } else {
                Toast.makeText(context, "Grant the camera permission", Toast.LENGTH_SHORT).show()
            }
        }
    }
    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putBoolean("phoneNumberExtracted", phoneNumberExtracted)
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (savedInstanceState != null) {
            phoneNumberExtracted = savedInstanceState.getBoolean("phoneNumberExtracted", false)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        if (cameraSource != null) {
            cameraSource!!.release()
        }
    }

    override fun onResume() {
        super.onResume()
        startCamera()
        scanQr=true
    }


}
