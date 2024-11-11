package com.example.anonymousgame

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener

class GameActivity : AppCompatActivity() {

    private lateinit var database: DatabaseReference
    private lateinit var roomName: String
    private lateinit var playerName: String
    private lateinit var playersList: MutableList<String> // Store the available players

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_game)

        roomName = intent.getStringExtra("roomName") ?: ""
        playerName = intent.getStringExtra("playerName") ?: "Unknown"
        database = FirebaseDatabase.getInstance("https://anonymousgame-7c8ee-default-rtdb.europe-west1.firebasedatabase.app")
            .getReference("rooms").child(roomName)

        playersList = mutableListOf()

        findViewById<Button>(R.id.exitGameButton).setOnClickListener {
            confirmExitGame()
        }

        loadPlayers()

        listenForAllPlayersReady()
        findViewById<Button>(R.id.finishButton).setOnClickListener {
            setPlayerReady()
            submitQuestion()
        }
    }

    private fun loadPlayers() {
        database.child("players").addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                playersList.clear()
                for (playerSnapshot in snapshot.children) {
                    val player = playerSnapshot.key ?: continue
                    if (player != playerName) {
                        playersList.add(player)
                    }
                }
                setupPlayerSelection()
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("GameActivity", "Failed to load players: ${error.message}")
            }
        })
    }

    private fun setPlayerReady() {
        database.child("players").child(playerName).child("ready").setValue(true).addOnCompleteListener { task ->
            if (task.isSuccessful) {
                checkAllPlayersReady()
            } else {
                Log.e("GameActivity", "Failed to mark player ready: ${task.exception?.message}")
            }
        }
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
                    database.child("allPlayersReady").setValue(true)
                } else {
                    Toast.makeText(this@GameActivity, "Waiting for all players to be ready...", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("GameActivity", "Failed to check player readiness: ${error.message}")
            }
        })
    }

    private fun listenForAllPlayersReady() {
        // Add listener to the allPlayersReady flag
        database.child("allPlayersReady").addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val allPlayersReady = snapshot.getValue(Boolean::class.java) ?: false
                if (allPlayersReady) {
                    database.child("allPlayersReady").setValue(false)
                    goToRound2()
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("GameActivity", "Failed to listen for allPlayersReady: ${error.message}")
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
                Log.e("GameActivity", "Failed to reset player readiness: ${error.message}")
            }
        })
    }

    private fun setupPlayerSelection() {
        val playerSpinner = findViewById<Spinner>(R.id.playerSpinner)
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, playersList)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        playerSpinner.adapter = adapter

        playerSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View, position: Int, id: Long) {
                findViewById<EditText>(R.id.messageInput).visibility = View.VISIBLE
            }

            override fun onNothingSelected(parent: AdapterView<*>) {
                findViewById<EditText>(R.id.messageInput).visibility = View.GONE
            }
        }
    }

    private fun submitQuestion() {
        val selectedPlayer = findViewById<Spinner>(R.id.playerSpinner).selectedItem as String
        val questionText = findViewById<EditText>(R.id.messageInput).text.toString()

        if (questionText.isNotEmpty()) {
            val questionData = mapOf(
                "target" to selectedPlayer,
                "question" to questionText
            )
            database.child("questions").push().setValue(questionData).addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    Toast.makeText(this, "Question submitted to $selectedPlayer", Toast.LENGTH_SHORT).show()
                    setPlayerReady()
                    findViewById<EditText>(R.id.messageInput).text.clear() // Clear the input field
                } else {
                    Log.e("GameActivity", "Failed to submit question: ${task.exception?.message}")
                }
            }
        } else {
            Toast.makeText(this, "Please enter a question", Toast.LENGTH_SHORT).show()
        }
    }

    // Navigate to round 2
    private fun goToRound2() {
        val intent = Intent(this, Round2Activity::class.java)
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
