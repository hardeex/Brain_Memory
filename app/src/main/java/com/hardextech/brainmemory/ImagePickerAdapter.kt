package com.hardextech.brainmemory

import android.content.Context
import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import com.hardextech.brainmemory.models.BoardSize
import kotlin.math.min

class ImagePickerAdapter(
    private val context: Context,
    private val chosenImageUris: List<Uri>,
    private  val boardSize: BoardSize,
private val imageClickListener: ImageClickListener
) : RecyclerView.Adapter<ImagePickerAdapter.ViewHolder>() {

    interface ImageClickListener {
        fun onPlaceholderClicked()
        // onPlaceholderClicked() means the user have clicked on the gray square book with the sole aim of importing images from the user device

    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(context).inflate(R.layout.card_image, parent, false)
        val cardHeight = parent.height/boardSize.getHeight()
        val cardWidth = parent.width/boardSize.getWidth()
        val cardSideLength = min(cardWidth, cardHeight)
        val layoutParam = view.findViewById<ImageView>(R.id.ivCustomImage).layoutParams
        layoutParam.width= cardSideLength
        layoutParam.height = cardSideLength
        return ViewHolder(view)
   }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        // the aim here is to determine how to display the UI given a particular position

        if (position< chosenImageUris.size){
            // show the image selected in the imageview
            holder.bind(chosenImageUris[position])
        } else{
            holder.bind()
        }

    }

    override fun getItemCount() = boardSize.getNumPairs()
    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView){
        private val ivCustomImage = itemView.findViewById<ImageView>(R.id.ivCustomImage)
        fun bind(uri: Uri) {
            // set the imageUri on the imageView
            ivCustomImage.setImageURI(uri)
            // them disable the user clicking on the image after image(s) is picked
           // ivCustomImage.setOnClickListener(null)
        }
        fun bind() {
            // enabling the user the click on the imageView in order to choose an image(s)
            ivCustomImage.setOnClickListener {
                // launch the intent for selecting images from the user gallery device
                imageClickListener.onPlaceholderClicked()
            }

        }

    }

} // end of the class
