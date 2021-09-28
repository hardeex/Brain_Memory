package com.hardextech.brainmemory

import android.content.Context
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import androidx.cardview.widget.CardView
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.recyclerview.widget.RecyclerView
import com.hardextech.brainmemory.models.BoardSize
import com.hardextech.brainmemory.models.BrainCard
import com.squareup.picasso.Picasso
import kotlin.math.min

// Declare the constructor as private val to enable usage in the body of the class
class BrainMemoryAdapter(
    private val context: Context,
    private val boardSize: BoardSize,
    private val cards: List<BrainCard>,
    private val cardClickedListener: CardClickedListener
) :
    RecyclerView.Adapter<BrainMemoryAdapter.ViewHolder>() {

    companion object {
        private const val MARGIN_SIZE = 10
        private const val TAG = "BrainMemoryAdapter"
    }

    interface CardClickedListener {
        // to inform the mainActivity that the user has made clicks
        fun onCardCLicked(position: Int)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        // For creating one view of the recyclerView
        val cardWidth: Int = parent.width / boardSize.getWidth() - (2 * MARGIN_SIZE)
        val cardHeight: Int = parent.height / boardSize.getHeight() - (2 * MARGIN_SIZE)
        val cardSideLength = min(cardWidth, cardHeight)
        val view = LayoutInflater.from(context).inflate(R.layout.brain_card, parent, false)
        val layoutParams =
            view.findViewById<CardView>(R.id.cardView).layoutParams as ViewGroup.MarginLayoutParams
        layoutParams.width = cardSideLength
        layoutParams.height = cardSideLength
        layoutParams.setMargins(MARGIN_SIZE, MARGIN_SIZE, MARGIN_SIZE, MARGIN_SIZE)
        return ViewHolder(view)
    }

    override fun getItemCount(): Int = boardSize.numCards

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        //counts the elements in the RecyclerView
        holder.bind(position)
    }


    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val imageButton = itemView.findViewById<ImageButton>(R.id.imageButton)
        fun bind(position: Int) {
            val brainCard: BrainCard = cards[position]
            // The logic for playing the custom game
            if (brainCard.isFaceUp) {
                if (brainCard.imageUrl != null) {
                    // this means there is a valid custom image
                    Picasso.get().load(brainCard.imageUrl)
                        .placeholder(R.drawable.ic_picasso_default_image).into(imageButton)
                } else {
                    imageButton.setImageResource(brainCard.identifier)
                }
            } else {
                // if the brainCard is faceDown
                imageButton.setImageResource(R.drawable.lightupdatedbackgrd)

            }
            /*
               if the logic,flipCard function from the BrainGame is correct , then set the boolean property is matched when it is indeed matched

               apha property refers to opacity, how visible is the imageButton
               */
            imageButton.alpha = if (brainCard.isMatched) .4f else 1.0f
            // if matched, make the background gray
            val colorStateList = if (brainCard.isMatched) ContextCompat.getColorStateList(
                context,
                R.color.gray
            ) else null
            ViewCompat.setBackgroundTintList(imageButton, colorStateList)

            imageButton.setOnClickListener {
                Log.i(TAG, "Clicked Position $position")
                cardClickedListener.onCardCLicked(position)
            }
        }
    }


}// end of the adapter class






