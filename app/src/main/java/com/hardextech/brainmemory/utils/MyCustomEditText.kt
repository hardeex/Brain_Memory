package com.hardextech.brainmemory.utils

import android.content.Context
import android.graphics.Typeface
import android.util.AttributeSet
import androidx.appcompat.widget.AppCompatEditText

class MyCustomEditText(context: Context, attributeSet: AttributeSet): AppCompatEditText(context, attributeSet) {
    init {
        applyMyCustomEditText()
    }

    private fun applyMyCustomEditText() {
        val typeface = Typeface.createFromAsset(context.assets, "myCustomFonts.ttf")
        setTypeface(typeface)
    }
}