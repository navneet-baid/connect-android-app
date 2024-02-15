package `in`.app.connect.adapter

import android.content.Context
import android.graphics.BitmapFactory
import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.ImageView
import androidx.cardview.widget.CardView
import `in`.app.connect.R

class ProfileImageAdapter(private val context: Context, private val images: Array<Uri?>) : BaseAdapter() {

    override fun getCount(): Int {
        return images.size
    }

    override fun getItem(position: Int): Any? {
        return images[position]
    }

    override fun getItemId(position: Int): Long {
        return position.toLong()
    }

    override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
        val view = convertView ?: LayoutInflater.from(context).inflate(R.layout.profile_picture_single_card, parent, false)
        val imageView: ImageView = view.findViewById(R.id.imageView)
        val imageViewCard: CardView = view.findViewById(R.id.imageViewCard)

        val imageUri = images[position]
        if (imageUri != null) {
            val inputStream = context.contentResolver.openInputStream(imageUri)
            val bitmap = BitmapFactory.decodeStream(inputStream)
            inputStream?.close()
            imageView.setImageBitmap(bitmap)
        } else {
            // if the image is null
            imageViewCard.visibility=View.GONE
        }

        return view
    }
}
