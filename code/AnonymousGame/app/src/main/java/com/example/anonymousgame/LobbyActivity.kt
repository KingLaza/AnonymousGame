package com.example.anonymousgame

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.database.*

class LobbyActivity : AppCompatActivity() {

    private lateinit var database: DatabaseReference
    private lateinit var roomName: String
    private lateinit var playerName: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_lobby)

        roomName = intent.getStringExtra("roomName") ?: ""
        playerName = intent.getStringExtra("playerName") ?: "Unknown"
        database = FirebaseDatabase.getInstance("https://anonymousgame-7c8ee-default-rtdb.europe-west1.firebasedatabase.app").getReference("rooms").child(roomName)

        // Add player to the room's player list
        addPlayerToRoom()

        // Display the number of players
        displayPlayerCount()                //you can just delete this afterwards, because you have displayLobbyInfo that does more

        displayLobbyInfo()

        // Listen for round changes
        listenForGameState()

        findViewById<Button>(R.id.startGameButton).setOnClickListener {
            database.child("players").addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val playerCount = snapshot.childrenCount
                    if (playerCount < 2) {
                        // Show error, can't start with less than 2 players
                        Toast.makeText(this@LobbyActivity, "You need at least 2 players to start the game!", Toast.LENGTH_SHORT).show()
                    } else {
                        startGame()
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    Log.e("LobbyActivity", "Error checking player count: ${error.message}")
                }
            })
        }

        findViewById<Button>(R.id.exitLobbyButton).setOnClickListener {
            exitLobby()
        }
    }

    private fun addPlayerToRoom() {
        database.child("players").child(playerName).setValue(mapOf("name" to playerName))
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    Log.i("LobbyActivity", "Player $playerName added to room $roomName")
                } else {
                    Log.e("LobbyActivity", "Failed to add player: ${task.exception?.message}")
                }
            }
    }

    private fun displayLobbyInfo() {
        database.addValueEventListener(object : ValueEventListener {           //addListenerForSingleValueEvent
            override fun onDataChange(snapshot: DataSnapshot) {
                val lobbyName = snapshot.key ?: ""
                val maxPlayers = snapshot.child("maxPlayers").getValue(Int::class.java) ?: 4
                val password = snapshot.child("password").getValue(String::class.java)
                val playersSnapshot = snapshot.child("players")

                // Display lobby name, player count, and max players
                findViewById<TextView>(R.id.lobbyNameTextView).text = "Lobby: $lobbyName"
                findViewById<TextView>(R.id.playerCountTextView).text = "Players: ${playersSnapshot.childrenCount}/$maxPlayers"

                // Display lobby password if it exists
                if (!password.isNullOrEmpty()) {
                    findViewById<TextView>(R.id.lobbyPasswordTextView).text = "Password: $password"
                } else {
                    findViewById<TextView>(R.id.lobbyPasswordTextView).text = "No Password"
                }

                // Display player names
                val playerNames = playersSnapshot.children.map { it.child("name").getValue(String::class.java) ?: "" }
                findViewById<TextView>(R.id.playersTextView).text = "Players:\n${playerNames.joinToString("\n")}"
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("LobbyActivity", "Error fetching lobby data: ${error.message}")
            }
        })
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
            intent.putExtra("playerName", playerName)  // Pass player name to BrowseLobbiesActivity
            startActivity(intent)
        }
    }

    private fun exitLobby() {
        // Remove the current player from the lobby
        val playerRef = database.child("players").child(playerName)  // Adjust this to get the correct player
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

