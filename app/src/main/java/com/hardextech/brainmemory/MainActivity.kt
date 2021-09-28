package com.hardextech.brainmemory

import android.animation.ArgbEvaluator
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.github.jinatonic.confetti.CommonConfetti

import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.hardextech.brainmemory.CreateActivity.Companion.EXTRA_BOARD_SIZE
import com.hardextech.brainmemory.CreateActivity.Companion.GAME_NAME
import com.hardextech.brainmemory.models.BoardSize
import com.hardextech.brainmemory.models.BrainGame
import com.hardextech.brainmemory.models.EXTRA_GAME_NAME
import com.hardextech.brainmemory.models.UserImageListDataClass
import com.squareup.picasso.Picasso
import java.lang.Exception


class MainActivity : BaseActivity() {

    companion object {
        private const val TAG = "MainActivity"
        private const val CREATE_REQUEST_CODE = 21
    }


    //start of declaring the variable in the mainActivity Layout
    private lateinit var rvBoard: RecyclerView
    private lateinit var tvNumMoves: TextView
    private lateinit var tvNumPairs: TextView
    private lateinit var clRoot: ConstraintLayout


    private var boardSize: BoardSize = BoardSize.EASY
    private lateinit var brainGame: BrainGame
    private lateinit var adapter: BrainMemoryAdapter

    private val db = Firebase.firestore
    private var gameName: String? = null


    private var customGameImages: List<String>? = null

    //End of declaring the variable in the mainActivity Layout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)



        initializeVariable()
        setupBoardGame()

    } // end of the onCreate method

    private fun setupBoardGame() {
        // change the title of the action bar to the user custom game if it exists
        supportActionBar?.title = gameName ?: getString(R.string.app_name)
        when (boardSize) {
            BoardSize.EASY -> {
                tvNumMoves.text = getString(R.string.main_easy)
                tvNumPairs.text = getString(R.string.main_pairs_easy)
            }
            BoardSize.MEDIUM -> {
                tvNumMoves.text = getString(R.string.main_medium)
                tvNumPairs.text = getString(R.string.main_medium_pairs)
            }
            BoardSize.HARD -> {
                tvNumMoves.text = getString(R.string.main_hard)
                tvNumPairs.text = getString(R.string.main_hard_pairs)
            }
        }
        tvNumPairs.setTextColor(ContextCompat.getColor(this, R.color.red))
        brainGame = BrainGame(boardSize, customGameImages)
        adapter = BrainMemoryAdapter(this, boardSize, brainGame.cards,
            object : BrainMemoryAdapter.CardClickedListener {
                override fun onCardCLicked(position: Int) {
                    Log.i(TAG, "Card Clicked $position")
                    updateGameWithFlip(position)
                }

            })
        rvBoard.adapter = adapter
        rvBoard.setHasFixedSize(true) // For Performance Optimization
        rvBoard.layoutManager = GridLayoutManager(this, boardSize.getWidth())


    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        // inflating the created menu resource file
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // getting notify when the menu icons are selected
        when (item.itemId) {
            R.id.refresh -> {
                if (brainGame.getNumMoves() > 0 && !brainGame.isWon()) {
                    // show alert dialogue to the user
                    showAlertDialogue("Do you want to quit your current game?", null) {
                        setupBoardGame()
                    }
                } else {
                    // Restart the game
                    setupBoardGame()
                }
                return true
            } // end of refresh button
            R.id.miGameLevel -> {
                showNewGameLevelDialogue()
                return true
            }
            R.id.miCreateCustomGame -> {
                showCustomGameDialogue()
                return true
            }
            R.id.miDownloadCustomGame -> {
                showDownloadDialog()
                return true
            }
            R.id.miAbout -> {
                AlertDialog.Builder(this).setTitle("ABOUT")
                    .setMessage(R.string.about_main)
                    .setPositiveButton("DISMISS") { _, _ ->
                        return@setPositiveButton
                    }.show()
            }
            R.id.miContact -> {
                // launch user gmail app
                try {
                    val emailIntent = Intent(
                        Intent.ACTION_SENDTO,
                        Uri.fromParts("mailto", "jamiuadewaleyusuf@gmail.com", null)
                    )
                    emailIntent.putExtra(Intent.EXTRA_TEXT, "Email message text")
                    startActivity(Intent.createChooser(emailIntent, "Send email"))
                } catch (e: Exception) {
                    Toast.makeText(this, "This function requires Gmail app", Toast.LENGTH_SHORT)
                        .show()
                }

            }
        }
        return super.onOptionsItemSelected(item)
    }


    @SuppressLint("InflateParams")
    private fun showDownloadDialog() {
        // display an editText for the use to enter the name of the custom game they wants to download
        val boardSizeDownload =
            LayoutInflater.from(this).inflate(R.layout.download_custom_game, null)
        showAlertDialogue("Download Custom Game", boardSizeDownload) {
            // download the specify download game from the Firestore
            val etDownloadCustomGameName =
                boardSizeDownload.findViewById<EditText>(R.id.etDownloadCustomGameName)
            val gameToDownload = etDownloadCustomGameName.text.toString().trim()
            downloadGameName(gameToDownload)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        // The objective here is to enable the user to play the created custom game
        if (requestCode == CREATE_REQUEST_CODE && resultCode == Activity.RESULT_OK) {
            // Retrieve the created custom game
            val retrieveCustomGameName = data?.getStringExtra(EXTRA_GAME_NAME)
            if (retrieveCustomGameName == null) {
                Log.e(TAG, "Got null custom game from the CreateActivity")
                Toast.makeText(this, "Access file is empty.... check your internet connection", Toast.LENGTH_SHORT).show()
                return
            }
            // if the custom game is not null
            downloadGameName(retrieveCustomGameName)
        }
        super.onActivityResult(requestCode, resultCode, data)
    }


    private fun downloadGameName(customGamaName: String) {
        // Retrieve the uploaded images from the Firestore and use it tp play the game in-place of the custom icons
        db.collection(GAME_NAME).document(customGamaName).get().addOnSuccessListener { document ->
            // converting the uploaded images into a kotlin data class
            val userImageListDataClass = document.toObject(UserImageListDataClass::class.java)
            // if the images attributes of the userImageListDataClass is null
            if (userImageListDataClass?.images == null) {
                Log.e(TAG, "Invalid custom game data from Firestore")
                // show alert dialogue
                AlertDialog.Builder(this).setTitle("SEARCH RESULT")
                    .setMessage("Your search is not in the database. Please, enter a valid registered/created custom name")
                    .setPositiveButton("OK") { _, _ ->
                        return@setPositiveButton
                    }.show()
                return@addOnSuccessListener
            }
            // if custom game name is found successfully
            // check for the number of cards in the memory game
            val numCard = userImageListDataClass.images.size * 2
            boardSize = BoardSize.getByteValue(numCard)
            customGameImages = userImageListDataClass.images
            // pre-fetched all the images with Picasso
            for (imageUrl in userImageListDataClass.images) {
                Picasso.get().load(imageUrl).fetch()
            }
            // showSnackBar("You are playing $customGamaName", false)
            showAlertDialogue("MATCH FOUND: Play $customGamaName?", null, View.OnClickListener {
                gameName = customGamaName
                setupBoardGame()
            })
//            gameName = customGamaName
//            setupBoardGame()
        }.addOnFailureListener { exception ->
            Log.e(TAG, "Exception when retrieving $gameName", exception)
            Toast.makeText(this, "Check your Internet Connection...Error retrieving $gameName ", Toast.LENGTH_LONG).show()
        }
    }

    @SuppressLint("InflateParams")
    private fun showCustomGameDialogue() {
        // displaying different game level to the user
        val boardSizeView = LayoutInflater.from(this).inflate(R.layout.dialogue_board_size, null)
        //initializing the user options, radioGroup
        val radioGroupSize = boardSizeView.findViewById<RadioGroup>(R.id.radioGroup)
        // changing game level dialogue
        showAlertDialogue("CREATE NEW MEMORY BOARD", boardSizeView) {
            // changing the game boardSize to the selected game level
            val desiredBoardSize = when (radioGroupSize.checkedRadioButtonId) {
                R.id.rb_easy -> BoardSize.EASY
                R.id.rb_medium -> BoardSize.MEDIUM
                else -> BoardSize.HARD
            }
            // after the user have selected the desired custom game level---- navigate the user to the activity
            val intent = Intent(this, CreateActivity::class.java)
            intent.putExtra(EXTRA_BOARD_SIZE, desiredBoardSize)
            startActivityForResult(intent, CREATE_REQUEST_CODE)

        }
    }

    @SuppressLint("InflateParams")
    private fun showNewGameLevelDialogue() {
        // displaying different game level to the user
        val boardSizeView = LayoutInflater.from(this).inflate(R.layout.dialogue_board_size, null)
        //initializing the user options, radioGroup
        val radioGroupSize = boardSizeView.findViewById<RadioGroup>(R.id.radioGroup)
        //showing which of the button to be clicked for each game level
        when (boardSize) {
            BoardSize.EASY -> radioGroupSize.check(R.id.rb_easy)
            BoardSize.MEDIUM -> radioGroupSize.check(R.id.rb_medium)
            BoardSize.HARD -> radioGroupSize.check(R.id.rb_hard)
        }
        // changing game level dialogue
        showAlertDialogue("CHOOSE NEW GAME LEVEL", boardSizeView) {
            // changing the game boardSize to the selected game level
            boardSize = when (radioGroupSize.checkedRadioButtonId) {
                R.id.rb_easy -> BoardSize.EASY
                R.id.rb_medium -> BoardSize.MEDIUM
                else -> BoardSize.HARD
            }
            gameName = null
            customGameImages = null
            // after the user have selected the desired game level
            setupBoardGame()

        }
    }

    private fun showAlertDialogue(
        title: String,
        view: View?,
        positiveButtonClickListener: View.OnClickListener
    ) {
        // method for displaying alert dialogue to the user
        AlertDialog.Builder(this)
            .setTitle(title)
            .setView(view)
            .setNegativeButton("NO", null)
            .setPositiveButton("YES") { _, _ ->
                positiveButtonClickListener.onClick(null)
            }.show()

    }

    @SuppressLint("NotifyDataSetChanged", "SetTextI18n")
    private fun updateGameWithFlip(position: Int) {
        /*
        perform some error handling..... the two kind of anticipated error are:
        a. if the user won the brain memory game
        b. if the card is already faced up
         */
        if (brainGame.isWon()) {
            //Alert the user of the invalid move
            showSnackBar("You Won Already...", false)
            return
        }
        if (brainGame.isCardAlreadyFaceUp(position)) {
            // Alert the user of the invalid move
            showSnackBar("Invalid move...", true)
            return
        }
        // the if statement is responsible for updating the BrainGame with an attempted flip at this position
        if (brainGame.flipCard(position)) {
            Log.i(TAG, "Match Found: ${brainGame.numPairsFound}")
            // show the user the progress of the game using linear extrapolation
            val color = ArgbEvaluator().evaluate(
                brainGame.numPairsFound.toFloat() / boardSize.getNumPairs(),    // the fraction of the progress made
                ContextCompat.getColor(
                    this,
                    R.color.red
                ), // start value, representing no progress made
                ContextCompat.getColor(
                    this,
                    R.color.green
                )  // end value, representing complete progress made

            ) as Int
            tvNumPairs.setTextColor(color)
            // display to the user, the result of the match
            tvNumPairs.text = "Pairs: ${brainGame.numPairsFound}/ ${boardSize.getNumPairs()}"
            if (brainGame.isWon()) {
                // showSnackBar("CONGRATULATIONS!!! YOU WON....", false)
                Toast.makeText(this, "CONGRATULATIONS!!! YOU WON.... ", Toast.LENGTH_LONG).show()
                // show a confetti when the user won the game
                CommonConfetti.rainingConfetti(
                    clRoot,
                    intArrayOf(Color.RED, Color.GREEN, Color.MAGENTA, Color.BLACK)
                ).oneShot()
            }
        }
        // display to the user, the number of moves performed
        tvNumMoves.text = "Moves: ${brainGame.getNumMoves()}"
        // after the card is flipped, inform the adapter that the content has changed
        adapter.notifyDataSetChanged()
    }

    private fun initializeVariable() {
        rvBoard = findViewById(R.id.rvBoardSize)
        tvNumMoves = findViewById(R.id.tvNumMoves)
        tvNumPairs = findViewById(R.id.tvNumPairs)
        clRoot = findViewById(R.id.clRoot)
    }
} // End of the class
