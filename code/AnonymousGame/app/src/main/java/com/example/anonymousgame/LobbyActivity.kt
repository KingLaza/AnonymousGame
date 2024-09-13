package com.example.anonymousgame

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.database.*

class LobbyActivity : AppCompatActivity() {

    private lateinit var database: DatabaseReference
    private lateinit var roomName: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_lobby)

        roomName = intent.getStringExtra("roomName") ?: ""
        database = FirebaseDatabase.getInstance("https://anonymousgame-7c8ee-default-rtdb.europe-west1.firebasedatabase.app").getReference("rooms").child(roomName)

        // Display the number of players
        displayPlayerCount()

        //I added this to listen to round changes
        listenForGameState()

        findViewById<Button>(R.id.startGameButton).setOnClickListener {
            startGame()
        }
        findViewById<Button>(R.id.exitLobbyButton).setOnClickListener {
            exitLobby()
        }
    }

    private fun displayPlayerCount() {
        database.child("players").addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val playerCount = snapshot.childrenCount
                findViewById<TextView>(R.id.playerCountTextView).text = "Players: $playerCount"
                Log.i("LobbyActivity", "Player count updated: $playerCount")
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("LobbyActivity", "Error fetching player count: ${error.message}")
            }
        })
    }


    private fun startGame() {
        database.child("gameState").child("round").setValue(1).addOnCompleteListener {
            val intent = Intent(this, GameActivity::class.java)
            intent.putExtra("roomName", roomName)
            startActivity(intent)
        }
    }

    private fun exitLobby() {
        // Remove the current player from the lobby
        val playerRef = database.child("players").child("HostName")  // Adjust this to get the correct player
        playerRef.removeValue().addOnCompleteListener {
            // Check if the lobby is empty and delete it if necessary
            checkAndDeleteLobby()

            // Return to MainActivity
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
            finish() // Close the lobby activity
        }
    }

    private fun checkAndDeleteLobby() {
        database.child("players").addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (!snapshot.hasChildren()) {
                    // If no players are left, delete the lobby
                    database.removeValue()
                }
            }

            override fun onCancelled(error: DatabaseError) {
                // Handle possible errors here
            }
        })
    }

    private fun listenForGameState() {
        database.child("gameState").child("round").addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val round = snapshot.getValue(Int::class.java)
                if (round == 1) {
                    // Start the game when the round is set to 1
                    startGame()
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("LobbyActivity", "Error fetching game state: ${error.message}")
            }
        })
    }



}

