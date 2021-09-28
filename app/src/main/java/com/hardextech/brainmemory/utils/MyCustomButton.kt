package com.hardextech.brainmemory.utils

import android.content.Context
import android.graphics.Typeface
import android.util.AttributeSet
import androidx.appcompat.widget.AppCompatButton

class MyCustomButton(context: Context, attributeSet: AttributeSet):
    AppCompatButton(context, attributeSet) {
    init {
        applyMyCustomButton()
    }

    private fun applyMyCustomButton() {
        val typeface = Typeface.createFromAsset(context.assets, "myCustomFonts.ttf")
        setTypeface(typeface)
    }
}