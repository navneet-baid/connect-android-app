package `in`.app.connect.usermanagment

import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import `in`.app.connect.usermanagment.models.UserData
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.GridView
import android.widget.ImageView
import android.widget.SeekBar
import android.widget.TextView
import androidx.cardview.widget.CardView
import `in`.app.connect.R
import `in`.app.connect.adapter.ProfileImageAdapter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.concurrent.TimeUnit


class UserProfileFragment : Fragment() {
    private lateinit var toolbarText:TextView
    private lateinit var profilePictureLayout: CardView
    private lateinit var profilePicture: ImageView
    private lateinit var profileName: TextView
    private lateinit var userAge: TextView
    private lateinit var userGender: TextView
    private lateinit var userBio: TextView
    private lateinit var backArrow:ImageView
    private lateinit var seekBar:SeekBar
    private lateinit var userData:UserData
    lateinit var images: Array<Uri?>
    lateinit var nextButton: Button
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
       val view= inflater.inflate(R.layout.fragment_user_profile_preview, container, false)
        toolbarText=view.findViewById(R.id.toolbarText)

        profilePictureLayout=view.findViewById(R.id.profilePictureLayout)
        profilePicture=view.findViewById(R.id.profilePicture)
        profileName=view.findViewById(R.id.profileName)
        userAge=view.findViewById(R.id.userAge)
        userGender=view.findViewById(R.id.userGender)
        userBio=view.findViewById(R.id.userBio)

        val activity = requireActivity() as? RegisterUser
        // Check if the activity is not null and the nextButton is accessible (public)
        activity?.let {
          userData =it.userData
          images =it.imagesUri
          seekBar=it.seekBar
          backArrow=it.backArrow
          nextButton=it.findViewById(R.id.btnNext)
        }
        backArrow.visibility=View.GONE
        seekBar.visibility=View.GONE
        toolbarText.text="Profile Preview"
        nextButton.text="DONE"

        val inputStream = requireContext().contentResolver.openInputStream(images[0]!!)
        val bitmap = BitmapFactory.decodeStream(inputStream)
        inputStream?.close()
        profilePicture.setImageBitmap(bitmap)
        profileName.text=userData.userName
        val sdf = SimpleDateFormat("dd/MM/yyyy")
        val birthDate: Date = sdf.parse(userData.dob) as Date
        val currentDate = Date()
        val diffInMillis: Long = currentDate.time - birthDate.time
        val ageInDays: Long = TimeUnit.MILLISECONDS.toDays(diffInMillis)
        // Assuming 365.25 days per year for more accurate results considering leap years.
        val ageInYears: Int = (ageInDays / 365.25).toInt()
        userAge.text=ageInYears.toString()
        userGender.text=userData.gender
        userBio.text=userData.bio


        return view
    }


}