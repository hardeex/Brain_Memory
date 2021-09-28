package com.hardextech.brainmemory.utils

import android.content.Context
import android.graphics.Typeface
import android.util.AttributeSet
import androidx.appcompat.widget.AppCompatTextView

class MyCustomFont( context: Context,  attributeSet: AttributeSet): AppCompatTextView( context,  attributeSet) {
    init {
        myCustomFont()
    }

    private fun myCustomFont() {
        val typeface: Typeface = Typeface.createFromAsset(context.assets, "myCustomFonts.ttf")
        setTypeface(typeface)

    }
}