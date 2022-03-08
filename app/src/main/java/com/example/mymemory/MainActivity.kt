package com.example.mymemory

import android.animation.ArgbEvaluator
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.RadioGroup
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.mymemory.models.BoardSize
import com.example.mymemory.models.MemoryGame
import com.example.mymemory.utils.EXTRA_BOARD_SIZE
import com.google.android.material.snackbar.Snackbar

/*
  RecyclerView has two core components 1. adapter 2. layout manager
  Layout manager : measures and positions item views
  Adapter: provide a binding for the data set to the views of
            the recycler view
 */


class MainActivity : AppCompatActivity() {

    companion object {
        private const val TAG = "MainActivity"
        private const val CREATE_REQUEST_CODE = 788001
    }

    private lateinit var clRoot : ConstraintLayout
    private lateinit var rvBoard : RecyclerView
    private lateinit var tvPairs : TextView
    private lateinit var tvMoves : TextView
    private lateinit var adapter : MemoryBoardAdapter


    private lateinit var memoryGame: MemoryGame

    // lateinit : (late initialization) Because these functions will set during invoking onCreate
    // and not during creation of MainActivity

    private var boardSize :BoardSize = BoardSize.EASY

    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        // R stands for resource ,check out in android ->resource ->layout-> activity main
        clRoot = findViewById(R.id.clRoot);
        rvBoard = findViewById(R.id.rvBoard)
        tvPairs = findViewById(R.id.tvPairs)
        tvMoves = findViewById(R.id.tvMoves)

        val intent = Intent(this, CreateActivity::class.java)
        intent.putExtra(EXTRA_BOARD_SIZE,BoardSize.MEDIUM);
        startActivity(intent);

        setUpBoard()


    }



    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_main,menu);
        return  true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when(item.itemId) {
            R.id.mi_refresh -> {
                if(memoryGame.getNumMoves() > 0 && !memoryGame.hasWonMatch()) {
                    showAlertDialog("Quit your current Game?", null, View.OnClickListener {
                        setUpBoard()
                    })
                }
                else {
                    // Setup the game again
                    setUpBoard()
                }
                return true

            }
            R.id.mi_new_size -> {
                showNewSizeDialog()
                return true
            }
            R.id.mi_custom -> {
                showCreationDialog()
                return true
            }
        }
        return  super.onOptionsItemSelected(item)
    }

    private fun showCreationDialog() {
        val boardSizeView = LayoutInflater.from(this).inflate(R.layout.dialog_board_size, null)
        val radioGroupSize = boardSizeView.findViewById<RadioGroup>(R.id.radioGroup)
        showAlertDialog("Create your own memory board",boardSizeView, View.OnClickListener {
            // set anew value for board size in main activity
            val desiredBoardSize = when(radioGroupSize.checkedRadioButtonId) {
                R.id.rbEasy -> BoardSize.EASY
                R.id.rbMedium -> BoardSize.MEDIUM
                else -> BoardSize.HARD

            }
            // Navigate user to new activity
            val intent = Intent(this, CreateActivity::class.java)
            intent.putExtra(EXTRA_BOARD_SIZE, desiredBoardSize)
            startActivityForResult(intent, CREATE_REQUEST_CODE)
            /*
            In order to actually navigate to the create activity we need to call this method
            StartActivity and there are two versions
            startActivity()
            startActivityForResult(): if u want to get some data back from the activity that u have
                launched.

             */
        })
    }

    private fun showNewSizeDialog() {
        val boardSizeView = LayoutInflater.from(this).inflate(R.layout.dialog_board_size, null)
        val radioGroupSize = boardSizeView.findViewById<RadioGroup>(R.id.radioGroup)
                when (boardSize){
                    BoardSize.EASY -> radioGroupSize.check(R.id.rbEasy)
                    BoardSize.MEDIUM -> radioGroupSize.check(R.id.rbMedium)
                    BoardSize.HARD -> radioGroupSize.check(R.id.rbHard)
                }
        showAlertDialog("Choose new size",boardSizeView, View.OnClickListener {
            // set anew value for board size in main activity
            boardSize = when(radioGroupSize.checkedRadioButtonId) {
                R.id.rbEasy -> BoardSize.EASY
                R.id.rbMedium -> BoardSize.MEDIUM
                else -> BoardSize.HARD

            }
            setUpBoard()
        })
    }

    private fun showAlertDialog(title: String, view: View?, positiveClickListener : View.OnClickListener) {
        AlertDialog.Builder(this)
            .setTitle(title)
            .setView(view)
            .setNegativeButton("Cancel",null)
            .setPositiveButton("OK") { _, _ ->
                positiveClickListener.onClick(null)
            }.show()

    }

    private fun setUpBoard() {
        when (boardSize){
            BoardSize.EASY -> {
                tvMoves.text = "Easy: 4 x 2"
                tvPairs.text = "Pairs: 0 / 4"
            }
            BoardSize.MEDIUM -> {
                tvMoves.text = "Medium: 6 x 3"
                tvPairs.text = "Pairs: 0 / 9"
            }
            BoardSize.HARD -> {
                tvMoves.text = "Hard: 6 x 4"
                tvPairs.text = "Pairs: 0 / 12"
            }
        }
        tvPairs.setTextColor(ContextCompat.getColor(this,R.color.color_progress_none))
        memoryGame = MemoryGame(boardSize)
        adapter = MemoryBoardAdapter(this,boardSize, memoryGame.cards,object : MemoryBoardAdapter.CardClickListener{
            override fun onCardClicked(position: Int) {
                updateGameWithFlip(position)
            }

        })
        rvBoard.adapter = adapter
        rvBoard.setHasFixedSize(true)
        rvBoard.layoutManager = GridLayoutManager(this,boardSize.getWidth())
    }

    private fun updateGameWithFlip(position: Int) {
        // Error checking
        if(memoryGame.hasWonMatch()) {
            // alert the user of invalid move
            Snackbar.make(clRoot,"You already won!!",Snackbar.LENGTH_LONG).show();
            return;
        }
        if(memoryGame.isCardFaceUp(position)){
            // alert the user of invalid move
            Snackbar.make(clRoot,"Invalid Move!!",Snackbar.LENGTH_SHORT).show();
            return;
        }
        //Actual flip of the card
        if(memoryGame.flipCard(position)){
            Log.i(TAG,"Found a match : Number of Pairs found ${memoryGame.numPairsFound}");

            val color = ArgbEvaluator().evaluate(
                memoryGame.numPairsFound.toFloat() / boardSize.getNumPairs(),
                ContextCompat.getColor(this,R.color.color_progress_none),
                ContextCompat.getColor(this,R.color.color_progress_full)
            ) as Int;
            tvPairs.setTextColor(color);
            tvPairs.text = "Pairs: ${memoryGame.numPairsFound} / ${boardSize.getNumPairs()}";
            if(memoryGame.hasWonMatch()){
                Snackbar.make(clRoot,"You have won !! Congratulations....  :)",Snackbar.LENGTH_LONG).show();
            }
        }
        tvMoves.text = "Moves: ${memoryGame.getNumMoves()}";
        adapter.notifyDataSetChanged()
    }
}