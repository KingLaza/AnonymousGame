package com.example.anonymousgame

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.database.*

class Round3Activity : AppCompatActivity() {

    private lateinit var database: DatabaseReference
    private lateinit var roomName: String
    private lateinit var playerName: String
    private lateinit var answersLayout: LinearLayout
    private lateinit var finishButton: Button
    private var waitingForOthersTextView: TextView? = null
    private lateinit var playersListener: ValueEventListener

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_round_3)

        roomName = intent.getStringExtra("roomName") ?: ""
        playerName = intent.getStringExtra("playerName") ?: ""
        database = FirebaseDatabase.getInstance("https://anonymousgame-7c8ee-default-rtdb.europe-west1.firebasedatabase.app")
            .getReference("rooms").child(roomName)

        answersLayout = findViewById(R.id.answersLayout)
        finishButton = findViewById(R.id.finishButton)

        loadQuestionsAndAnswers()

        finishButton.setOnClickListener {
            setPlayerFinished()
            showWaitingMessage()
        }

        listenForAllPlayersReady()
    }

    private fun loadQuestionsAndAnswers() {
        database.child("questions")
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    if (snapshot.exists()) {
                        for (questionSnapshot in snapshot.children) {
                            val question = questionSnapshot.child("question").getValue(String::class.java)
                            val answer = questionSnapshot.child("answer").getValue(String::class.java)
                            val targetPlayer = questionSnapshot.child("target").getValue(String::class.java)

                            if (question != null && answer != null && targetPlayer != null) {
                                addAnswerView(question, answer, targetPlayer)
                            }
                        }
                    } else {
                        Log.e("Round3Activity", "No questions found")
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    Log.e("Round3Activity", "Failed to load questions: ${error.message}")
                }
            })
    }

    private fun addAnswerView(question: String, answer: String, targetPlayer: String) {
        val questionView = TextView(this).apply {
            text = "Question for $targetPlayer: $question"
            textSize = 18f
        }
        val answerView = TextView(this).apply {
            text = "Answer: $answer"
            textSize = 16f
            setPadding(0, 10, 0, 20)
        }

        answersLayout.addView(questionView)
        answersLayout.addView(answerView)
    }

    private fun setPlayerFinished() {
        database.child("players").child(playerName).child("ready").setValue(true)
    }

    private fun listenForAllPlayersReady() {
        playersListener = database.child("players").addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                var allReady = true
                for (playerSnapshot in snapshot.children) {
                    val isReady = playerSnapshot.child("ready").getValue(Boolean::class.java) ?: false
                    if (!isReady) {
                        allReady = false
                        break
                    }
                }
                if (allReady) {
                    deleteQuestionsAndAnswers()
                    resetGameState()
                    returnToLobby()
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("Round3Activity", "Failed to listen for all players ready: ${error.message}")
            }
        })
    }

    private fun deleteQuestionsAndAnswers() {
        database.child("questions").removeValue().addOnCompleteListener { task ->
            if (task.isSuccessful) {
                Log.d("Round3Activity", "Questions and answers deleted successfully")
            } else {
                Log.e("Round3Activity", "Failed to delete questions and answers: ${task.exception}")
            }
        }
    }

    private fun resetGameState() {
        // Reset the round or any other states needed to prevent immediate start
        database.child("gameState").child("round").setValue(0)
        database.child("players").child(playerName).child("ready").setValue(false)
    }

    private fun returnToLobby() {
        // Remove the listener to prevent further updates after returning to lobby
        database.child("players").removeEventListener(playersListener)

        val intent = Intent(this, LobbyActivity::class.java)
        intent.putExtra("roomName", roomName)
        intent.putExtra("playerName", playerName)
        startActivity(intent)
        finish()
    }

    private fun showWaitingMessage() {
        finishButton.apply {
            text = "Waiting for other players..."
            setBackgroundColor(Color.parseColor("#32CD32"))  // Bright green for better visibility
            setTextColor(Color.WHITE) // White text for contrast
            isEnabled = false
        }
        if (waitingForOthersTextView == null) {
            waitingForOthersTextView = TextView(this).apply {
                text = "Waiting for other players to finish..."
                textSize = 18f
                setTextColor(Color.DKGRAY)
                answersLayout.addView(this)
            }
        }
    }
}
