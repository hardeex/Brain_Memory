package com.hardextech.brainmemory
// The .kt class that allows the user to create custom game

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.MediaStore
import android.text.Editable
import android.text.InputFilter
import android.text.TextWatcher
import android.util.Log
import android.view.MenuItem
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.ktx.storage
import com.hardextech.brainmemory.models.BoardSize
import com.hardextech.brainmemory.models.EXTRA_GAME_NAME
import com.hardextech.brainmemory.models.requestPermission
import com.hardextech.brainmemory.models.userGrantPermission
import com.hardextech.brainmemory.utils.BitmapScaler
import java.io.ByteArrayOutputStream


class CreateActivity : AppCompatActivity() {

    companion object{
        const val EXTRA_BOARD_SIZE = "EXTRA_BOARD_SIZE"

        const val PICK_PHOTO_REQUEST_CODE= 2021
        const val READ_PHOTO_PERMISSION = android.Manifest.permission.READ_EXTERNAL_STORAGE
        const val READ_PHOTO_PERMISSION_CODE = 445
        const val TAG ="CreateActivity"
        const val MAXIMUM_GAME_NAME_LENGTH = 14
        const val MINIMUM__GAME_NAME_LENGTH = 3
        const val GAME_NAME = "GAME NAME"
    }

    private lateinit var rvCreateCustomGame:RecyclerView
    private lateinit var etCreateGameName: EditText
    private lateinit var btnSavaCreatedGame: Button
    private  lateinit var adapter: ImagePickerAdapter
    private lateinit var pbUploadingUI: ProgressBar

    private lateinit var boardSize: BoardSize
    private var numPairsRequired = -1
    private val chosenImageUris = mutableListOf<Uri>()
    /*
    URI-- Uniform Resource identifier
    it is more like a string that unambiguously identifies where a particular resource live
     */


    private val storage = Firebase.storage
    private val db= Firebase.firestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_create)

        // getting the data from the intent in the mainActivity
       boardSize =intent.getSerializableExtra(EXTRA_BOARD_SIZE) as BoardSize
        // indicating the number of selected pictures
        numPairsRequired = boardSize.getNumPairs()
        supportActionBar?.title = "Choose Pictures (0/$numPairsRequired)"
        // modifying the action bar to show a back button
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        // initializing the variables from the xml
        initiateVariables()
         adapter= ImagePickerAdapter(this, chosenImageUris, boardSize,
            object : ImagePickerAdapter.ImageClickListener {
                override fun onPlaceholderClicked() {
                    if (userGrantPermission(this@CreateActivity, READ_PHOTO_PERMISSION)){
                        // launch the images after the onPlaceholderClicked()
                        launchIntentForPhoto()
                    }else{
                        requestPermission(this@CreateActivity, READ_PHOTO_PERMISSION, READ_PHOTO_PERMISSION_CODE)
                    }

                }

            })
        rvCreateCustomGame.adapter= adapter
        rvCreateCustomGame.setHasFixedSize(true) // for optimization
        rvCreateCustomGame.layoutManager=GridLayoutManager(this, boardSize.getWidth()) // setting the context and the number of columns

        // setting the maximum text length for the game name
        etCreateGameName.filters = arrayOf(InputFilter.LengthFilter(MAXIMUM_GAME_NAME_LENGTH))
        // enabling the save button when there is a change in the editText, game
        etCreateGameName.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {
                btnSavaCreatedGame.isEnabled = shouldEnableSaveButton()
            }

            override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) { }
            override fun afterTextChanged(p0: Editable?) { }
        })  // end of enabling the save button when there is a change in the editText, game

        // saving the custom game to firebase after the user click the save button
        btnSavaCreatedGame.setOnClickListener {
            saveDataToFirebase()
        }

    } // end of onCreate

    private fun saveDataToFirebase() {
        // disable the save button as soon as the user click on it
        btnSavaCreatedGame.isEnabled= false
        val customGameName = etCreateGameName.text.toString().trim{it <=' '}
        db.collection(GAME_NAME).document(customGameName).get().addOnSuccessListener { document->
            if (document!=null && document.data !=null){
                AlertDialog.Builder(this).setTitle("$customGameName Exists Already...")
                    .setMessage("Choose another Custom Game Name")
                    .setPositiveButton("OK", null).show()
                btnSavaCreatedGame.isEnabled= true

            } else{
                handleUploadingImages(customGameName)
            }
        }.addOnFailureListener {exception ->
            Log.e(TAG, "Encounter some error while saving Brain Game", exception)
            Toast.makeText(this, "Encounter some error while saving $customGameName ", Toast.LENGTH_SHORT).show()
            btnSavaCreatedGame.isEnabled= true

        }

    }

    private fun handleUploadingImages(gameName: String) {
        // displaying the progressBar to the user
        pbUploadingUI.visibility = View.VISIBLE
        var didEncounterError = false
        val uploadedImageUrl = mutableListOf<String>()
        // The aim of this method is to reduce the size of the image to be uploaded to the firebase console
        Log.i(TAG, "saveDataToFirebase")
        // loops to iterate through the chosen the image
        for ((index, photoUrl) in chosenImageUris.withIndex()){
            // what will be uploaded to the firebase storage is the imageByteArray
            val imageByteArray = getImageByteArray(photoUrl)

            // uploading the user chosen images to the firebase storage
            // first, naming the uploaded images filepath or file name
            val filepath = "Images/${gameName}/${System.currentTimeMillis()}-${index}.jpg"
            // we can save the uploaded images to firebase storage--- reference for saving the uploaded photos
            val photoReference = storage.reference.child(filepath)
            photoReference.putBytes(imageByteArray)
                // getting the feedback if the upload is successful or failed --- once it Concludes, execute this task and one and task that is defined
                .continueWithTask { photoUploadtask->
                    Log.i(TAG, "Photo bytes ${photoUploadtask.result?.bytesTransferred}")
                    /*
                    The block will be ended with another task
                    once the uploaded is completed, we want to get the uploaded complete URL
                     */
                    photoReference.downloadUrl
                        /*   photoReference.downloadUrl is going to enable a task and we have to wait for the completion of the task.
                        in order to get notify of the task-- inorder to get notify, add the method below
                        .addOnCompleteListener will be called every time image is uploaded, there is need to control the order of the upload
                         */

                        .addOnCompleteListener { downnloadUrlTask->
                            if (!downnloadUrlTask.isSuccessful){
                                Log.e(TAG, " Exception with Firebase storage", downnloadUrlTask.exception)
                                Toast.makeText(this, "Failed to upload the image", Toast.LENGTH_LONG).show()
                                didEncounterError = true
                                return@addOnCompleteListener
                            }
                            // if not all but some of of the images failed to upload
                            if (didEncounterError){
                                // displaying the progressBar to the user
                                pbUploadingUI.visibility = View.GONE
                                return@addOnCompleteListener
                            }
                            // if the upload is successfully
                            val downloadUrl = downnloadUrlTask.result.toString()
                            // notify that the images have been successful uploaded --- the way to do that, is by keeping the array of the uploaded images
                            uploadedImageUrl.add(downloadUrl)
                            // displaying the progressBar to the user-- the progress report
                            pbUploadingUI.progress = uploadedImageUrl.size * 100/chosenImageUris.size
                            // log the success rate of the uploaded images
                            Log.i(TAG, "Successful uploaded $photoUrl, num uploaded ${uploadedImageUrl.size}")
                            if (uploadedImageUrl.size == chosenImageUris.size){
                                handleAllImagesUploaded(gameName, uploadedImageUrl)
                            }
                        }
                }

        }

    }

    private fun handleAllImagesUploaded(gameName: String, imageUrl: MutableList<String>) {
        // uploading the images to the Firestore
        db.collection(GAME_NAME).document(gameName).set(mapOf("images" to  imageUrl) )
            .addOnCompleteListener { customGameCreationTask->
                // the progressBar
                pbUploadingUI.visibility = View.GONE
                if (!customGameCreationTask.isSuccessful){
                    Log.e(TAG, "Exception with game creation", customGameCreationTask.exception )
                    Toast.makeText(this, "Creating New Custom Game Failed", Toast.LENGTH_SHORT).show()
                    return@addOnCompleteListener
                }
                Log.i(TAG, "Successfully created new custom Game")
                // show alert dialogue to the user for navigation
                AlertDialog.Builder(this).setTitle("Successfully Created!!! Let's play $gameName now?")
                    .setPositiveButton("OK"){_,_->
                        // inform the mainActivity of the new custom game that's been created-- first, create an empty intent
                        val resultData = Intent()
                        // after the empty intent, put in the resultData
                        resultData.putExtra(EXTRA_GAME_NAME, gameName)
                        setResult(Activity.RESULT_OK, resultData)
                        finish()
                    }.show()
            }
    }

    private fun getImageByteArray(photoUri: Uri): ByteArray {
        /*
        This method takes care of downscaling the images the user chooses
        the first step is to:
        a. get the original bitmap based on the photoUri which depends on the API version of the phone, this app is running on
         */
        // if the user device is running android version pie, android version 9 or higher
            val originalBitmap = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P){
            val source = ImageDecoder.createSource(contentResolver, photoUri)
            ImageDecoder.decodeBitmap(source)
        }else{
            // if the user device is a lower version android device
            MediaStore.Images.Media.getBitmap(contentResolver, photoUri)
        }
        // log the original width and height for comparison between the original copy and reduced copy
        Log.i(TAG, "Original Width: ${originalBitmap.width} and height ${originalBitmap.height}")
        val scaleBitmap = BitmapScaler.scaleToFitHeight(originalBitmap, 250)
        // log the scaled width
        Log.i(TAG, "Scale Width: ${scaleBitmap.width} and height ${scaleBitmap.height}")
        //returning the byteArray
        val byteOutputStream = ByteArrayOutputStream()
        scaleBitmap.compress(Bitmap.CompressFormat.JPEG, 60, byteOutputStream)
        return byteOutputStream.toByteArray()
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        if (requestCode== READ_PHOTO_PERMISSION_CODE){
            if (grantResults.isNotEmpty() && grantResults[0]== PackageManager.PERMISSION_GRANTED){
                launchIntentForPhoto()
            }else{
                Toast.makeText(this, "Permission is required in order to create custom game", Toast.LENGTH_LONG).show()
            }
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }

    @SuppressLint("NotifyDataSetChanged")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode!= PICK_PHOTO_REQUEST_CODE || resultCode!= RESULT_OK || data==null){
            Log.w(TAG, "Data could no be received from the launched activity, user probably cancelled the flow")
            return
        }
        // if the launched app for selecting photo on the user device supports picking a single image
        val selectedUri = data.data
            // if it supports selecting multiple images
        val clipData = data.clipData
        if (clipData!=null){
            Log.i(TAG, "ClipData numImages ${clipData.itemCount}: $clipData")
            for (i in 0 until clipData.itemCount){
                // get the clipData at the position of the loop
                val clipItem = clipData.getItemAt(i)
                // get the imageUri out of the clipItem object
                if (chosenImageUris.size < numPairsRequired){
                    chosenImageUris.add(clipItem.uri)
                }
            } // end of the for loop
        } else if (selectedUri!=null){
            Log.i(TAG, "data: $selectedUri")
            chosenImageUris.add(selectedUri)
        }
        // notify the adapter that the dataSet has changed
        adapter.notifyDataSetChanged()
        // display the number of selected images at the action bar
        supportActionBar?.title = "Choose Picture (${chosenImageUris.size}/ ${numPairsRequired})"
        // enabling the save button after the required conditions are meet
        btnSavaCreatedGame.isEnabled = shouldEnableSaveButton()
    }

    private fun shouldEnableSaveButton(): Boolean {
        // the requirement and tests for enabling the save button

        // TODO: Progress dialogue
        if(chosenImageUris.size != numPairsRequired){
            return false
        }
        if (etCreateGameName.text.isBlank() || etCreateGameName.text.length < MINIMUM__GAME_NAME_LENGTH){
            return false
        }
        return true
    }

    private fun launchIntentForPhoto() {
        // launch the user gallery app
        val intent = Intent(Intent.ACTION_PICK)
        intent.type ="image/*"
        intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
        startActivityForResult(Intent.createChooser(intent, "Choose Pictures"), PICK_PHOTO_REQUEST_CODE)
       // startActivity(intent)
    }

    private fun initiateVariables() {
        rvCreateCustomGame=findViewById(R.id.rvCreateGame)
        etCreateGameName = findViewById(R.id.etCustomGameName)
        btnSavaCreatedGame=findViewById(R.id.myCustomButton)
        pbUploadingUI=findViewById(R.id.pbUploading)
    }

    // enabling the home back button at the action bar to return to the main activity
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId==android.R.id.home){
            finish()
            return true
        }
        return super.onOptionsItemSelected(item)
    }
}