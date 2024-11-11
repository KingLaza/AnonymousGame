package com.example.anonymousgame

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.database.*

class Round2Activity : AppCompatActivity() {

    private lateinit var database: DatabaseReference
    private lateinit var roomName: String
    private lateinit var playerName: String
    private lateinit var questionsLayout: LinearLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_round_2)

        roomName = intent.getStringExtra("roomName") ?: ""
        playerName = intent.getStringExtra("playerName") ?: ""
        database = FirebaseDatabase.getInstance("https://anonymousgame-7c8ee-default-rtdb.europe-west1.firebasedatabase.app")
            .getReference("rooms").child(roomName)

        questionsLayout = findViewById(R.id.questionsLayout)

        loadQuestions()

        findViewById<Button>(R.id.finishButton).setOnClickListener {
            saveAnswers()
        }

        findViewById<Button>(R.id.exitGameButton).setOnClickListener {
            confirmExitGame()
        }

        listenForAllPlayersReady()
    }

    private fun loadQuestions() {
        // Fetch questions targeted at the current player from Firebase
        database.child("questions")
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    if (snapshot.exists()) {
                        for (questionSnapshot in snapshot.children) {
                            val questionText = questionSnapshot.child("question").getValue(String::class.java)
                            val targetPlayer = questionSnapshot.child("target").getValue(String::class.java)
                            val questionKey = questionSnapshot.key

                            // Only show questions targeted at the current player
                            if (targetPlayer == playerName && questionText != null && questionKey != null) {
                                addQuestionView(questionText, questionKey)
                            }
                        }
                    } else {
                        Log.e("Round2Activity", "No questions found for player: $playerName")
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    Log.e("Round2Activity", "Failed to load questions: ${error.message}")
                }
            })
    }

    private fun addQuestionView(questionText: String, questionKey: String) {
        // Create and add a TextView for each question
        val questionView = TextView(this).apply {
            text = questionText
            textSize = 18f
            setPadding(0, 20, 0, 10)
        }

        // Create and add an EditText for the answer
        val answerInput = EditText(this).apply {
            hint = "Enter your answer"
            tag = questionKey // Store question key as tag to save answers later
        }

        // Add the views to the LinearLayout
        questionsLayout.addView(questionView)
        questionsLayout.addView(answerInput)
    }

    private fun saveAnswers() {
        // Loop through children and ensure they are the correct types
        for (i in 0 until questionsLayout.childCount step 2) {
            val questionView = questionsLayout.getChildAt(i)
            val answerInput = questionsLayout.getChildAt(i + 1)

            if (questionView is TextView && answerInput is EditText) {
                val questionKey = answerInput.tag as? String
                val answer = answerInput.text.toString()

                if (questionKey != null) {
                    // Save answer directly under the question using its key
                    database.child("questions").child(questionKey).child("answer").setValue(answer)
                        .addOnCompleteListener { task ->
                            if (task.isSuccessful) {
                                Log.d("Round2Activity", "Answer saved for question: ${questionView.text}")
                            } else {
                                Log.e("Round2Activity", "Failed to save answer: ${task.exception?.message}")
                            }
                        }
                } else {
                    Log.e("Round2Activity", "Question key is null")
                }
            }
        }

        // Mark the player as ready
        database.child("players").child(playerName).child("ready").setValue(true)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    checkAllPlayersReady()
                } else {
                    Log.e("Round2Activity", "Failed to set player ready: ${task.exception?.message}")
                }
            }

        // Change the appearance of the Finish button
        val finishButton = findViewById<Button>(R.id.finishButton)
        finishButton.text = "Finished!"
        finishButton.setBackgroundColor(Color.parseColor("#4CAF50")) // Set to green
        finishButton.isEnabled = false
    }

    private fun checkAllPlayersReady() {
        database.child("players").addListenerForSingleValueEvent(object : ValueEventListener {
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
                    resetAllPlayersReady()
                    database.child("allPlayersReadyRound2").setValue(true)
                } else {
                    Toast.makeText(this@Round2Activity, "Waiting for other players...", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("Round2Activity", "Failed to check players readiness: ${error.message}")
            }
        })
    }

    private fun resetAllPlayersReady() {
        database.child("players").addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                for (playerSnapshot in snapshot.children) {
                    playerSnapshot.ref.child("ready").setValue(false)
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("Round2Activity", "Failed to reset player readiness: ${error.message}")
            }
        })
    }

    private fun listenForAllPlayersReady() {
        // Add listener to the allPlayersReadyRound2 flag
        database.child("allPlayersReadyRound2").addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val allPlayersReady = snapshot.getValue(Boolean::class.java) ?: false
                if (allPlayersReady) {
                    database.child("allPlayersReadyRound2").setValue(false)
                    goToRound3()
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("Round2Activity", "Failed to listen for allPlayersReadyRound2: ${error.message}")
            }
        })
    }

    private fun goToRound3() {
        // Logic to proceed to round 3
        val intent = Intent(this, Round3Activity::class.java)
        intent.putExtra("roomName", roomName)
        intent.putExtra("playerName", playerName)
        startActivity(intent)
        finish()
    }

    private fun confirmExitGame() {
        val builder = android.app.AlertDialog.Builder(this)
        builder.setTitle("Exit Game")
        builder.setMessage("Are you sure you want to quit the game?")

        builder.setPositiveButton("Yes") { dialog, _ -> exitGame() }
        builder.setNegativeButton("No") { dialog, _ -> dialog.dismiss() }

        builder.show()
    }

    private fun exitGame() {
        val playerRef = database.child("players").child(playerName)
        playerRef.removeValue().addOnCompleteListener {
            checkAndDeleteLobby()
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
            finish()
        }
    }

    private fun checkAndDeleteLobby() {
        database.child("players").addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (!snapshot.hasChildren()) {
                    database.removeValue()
                    Log.i("GameActivity", "Lobby $roomName deleted as it's empty.")
                }
            }
            override fun onCancelled(error: DatabaseError) {
                Log.e("GameActivity", "Failed to check players: ${error.message}")
            }
        })
    }
}
