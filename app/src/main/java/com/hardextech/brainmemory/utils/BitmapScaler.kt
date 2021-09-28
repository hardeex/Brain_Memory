package com.hardextech.brainmemory.utils

import android.graphics.Bitmap

object BitmapScaler {
    /*
    The idea of the bitmapScaler is similar to the permission utils--- it is simply to create a nicer looking API or method around
    some of the Android API

    scale and maintain aspect ratio given a desired width
    BitMapScaler.scaleToFitHeight(bitmap, 100)
     */

    fun scaleToFitWidth(b: Bitmap, width:Int): Bitmap{
        val factor = width/b.width.toFloat()
        return  Bitmap.createScaledBitmap(b, width, (b.height*factor).toInt(), true)
    }

    fun scaleToFitHeight(b: Bitmap, height:Int):Bitmap{
        val factor = height/b.height.toFloat()
        return Bitmap.createScaledBitmap(b, (b.width*factor).toInt(), height, true)
    }

}
