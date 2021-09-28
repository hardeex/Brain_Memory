package com.hardextech.brainmemory


import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.google.android.material.snackbar.Snackbar

open class BaseActivity : AppCompatActivity() {


    fun showSnackBar(message: String, errorMessage: Boolean){
        val snackBar = Snackbar.make(findViewById(android.R.id.content), message, Snackbar.LENGTH_SHORT)
        val snackBarView = snackBar.view

        if (errorMessage){
            snackBarView.setBackgroundColor(ContextCompat.getColor(this, R.color.jardsColor))
        } else {
            snackBarView.setBackgroundColor(ContextCompat.getColor(this, R.color.green))
        }
        snackBar.show()
    }



}