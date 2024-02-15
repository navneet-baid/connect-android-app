package `in`.app.connect

import android.content.Context
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.fragment.app.DialogFragment


class PostOptionsDialog : DialogFragment() {

    interface PostOptionListener {
        fun onOptionSelected(option: String)
    }

    private var listener: PostOptionListener? = null

    override fun onAttach(context: Context) {
        super.onAttach(context)
        listener = context as? PostOptionListener
    }

    private lateinit var cardViewPublic: CardView
    private lateinit var cardViewAnonymous: CardView
    private lateinit var imageViewPublic: ImageView
    private lateinit var imageViewAnonymous: ImageView
    private lateinit var textViewPublic: TextView
    private lateinit var textViewAnonymous: TextView

    private var selectedCard: CardView? = null
    private var postAs: String? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val rootView = inflater.inflate(R.layout.dialog_post_options, container, false)

        cardViewPublic = rootView.findViewById(R.id.cardViewPublic)
        cardViewAnonymous = rootView.findViewById(R.id.cardViewAnonymous)
        imageViewPublic = rootView.findViewById(R.id.imageViewPublic)
        imageViewAnonymous = rootView.findViewById(R.id.imageViewAnonymous)
        textViewPublic = rootView.findViewById(R.id.textViewPublic)
        textViewAnonymous = rootView.findViewById(R.id.textViewAnonymous)

        cardViewPublic.setOnClickListener { selectCard(cardViewPublic) }
        cardViewAnonymous.setOnClickListener { selectCard(cardViewAnonymous) }
        // Remove padding from CardView elements
        cardViewPublic.cardElevation = 0f
        cardViewAnonymous.cardElevation = 0f
        return rootView
    }

    private fun selectCard(cardView: CardView) {
        if (selectedCard != null) {
            selectedCard!!.setCardBackgroundColor(Color.WHITE)
        }
        cardView.setCardBackgroundColor(resources.getColor(R.color.red))
        selectedCard = cardView
        postAs = when (cardView) {
            cardViewPublic -> textViewPublic.text.toString()
            cardViewAnonymous -> textViewAnonymous.text.toString()
            else -> null
        }

        listener?.onOptionSelected(postAs!!)
    }

}
