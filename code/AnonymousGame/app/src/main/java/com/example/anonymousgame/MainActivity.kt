package com.example.anonymousgame

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import kotlin.random.Random

class MainActivity : AppCompatActivity() {

    private lateinit var database: DatabaseReference
    private lateinit var playerName: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        database = FirebaseDatabase.getInstance("https://anonymousgame-7c8ee-default-rtdb.europe-west1.firebasedatabase.app").reference

        val lobbyNameEditText = findViewById<EditText>(R.id.lobbyNameEditText)
        val maxPlayersEditText = findViewById<EditText>(R.id.maxPlayersEditText)
        val passwordEditText = findViewById<EditText>(R.id.passwordEditText)

        playerName = "Player${Random.nextInt(10000, 99999)}"
        val playerNameTextView = findViewById<TextView>(R.id.playerNameDisplay)
        playerNameTextView.text = playerName

        // Edit Player Name Functionality
        findViewById<ImageView>(R.id.editPlayerNameIcon).setOnClickListener {
            val editNameDialog = EditText(this)
            AlertDialog.Builder(this)
                .setTitle("Edit Player Name")
                .setView(editNameDialog)
                .setPositiveButton("OK") { dialog, _ ->
                    playerName = editNameDialog.text.toString()
                    playerNameTextView.text = playerName
                    dialog.dismiss()
                }
                .setNegativeButton("Cancel") { dialog, _ -> dialog.dismiss() }
                .show()
        }

        // Leave Game Button
        /*findViewById<Button>(R.id.leaveGameButton).setOnClickListener {
            leaveGame()
        }*/
        findViewById<Button>(R.id.leaveGameButton).setOnClickListener {
            // Logic to leave the game or exit the app
            finish() // You can modify this to leave the lobby or handle any other logic
        }


        findViewById<Button>(R.id.createRoomButton).setOnClickListener {
            val lobbyName = lobbyNameEditText.text.toString().ifEmpty { "Lobby_${System.currentTimeMillis()}" } // Default name if empty
            val maxPlayers = maxPlayersEditText.text.toString().toIntOrNull() ?: 4 // Default to 4 players
            val password = passwordEditText.text.toString()

            if (!playerNameTextView.text.isNullOrEmpty()) {
                playerName = playerNameTextView.text.toString()
            }

            createRoom(lobbyName, maxPlayers, password, playerName)
        }

        findViewById<Button>(R.id.joinSpecificLobbyButton).setOnClickListener {
            val intent = Intent(this, JoinSpecificLobbyActivity::class.java)
            if (!playerNameTextView.text.isNullOrEmpty()) {
                playerName = playerNameTextView.text.toString()
            }
            intent.putExtra("playerName", playerName)
            startActivity(intent)
        }

        findViewById<Button>(R.id.joinRandomLobbyButton).setOnClickListener {
            joinRandomLobby()
        }

        findViewById<Button>(R.id.browseLobbiesButton).setOnClickListener {
            val intent = Intent(this, BrowseLobbiesActivity::class.java)
            if (!playerNameTextView.text.isNullOrEmpty()) {
                playerName = playerNameTextView.text.toString()
            }
            intent.putExtra("playerName", playerName)
            startActivity(intent)
        }
    }

    private fun leaveGame() {
        // Logic to leave the game, e.g., clear player data, return to a previous screen, etc.
        val intent = Intent(this, MainActivity::class.java)
        startActivity(intent)
        finish()
    }

    private fun joinRandomLobby() {
        database.child("rooms").orderByKey().limitToFirst(1).addListenerForSingleValueEvent(object :
            ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val firstLobby = snapshot.children.firstOrNull()
                if (firstLobby != null) {
                    val intent = Intent(this@MainActivity, LobbyActivity::class.java)
                    intent.putExtra("roomName", firstLobby.key)
                    startActivity(intent)
                } else {
                    Log.e("MainActivity", "No lobbies available")
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("MainActivity", "Error: ${error.message}")
            }
        })
    }

    private fun createRoom(lobbyName: String, maxPlayers: Int, password: String?, playerName: String) {
        val roomData = mapOf(
            "host" to playerName,
            "players" to mapOf(
                playerName to mapOf(
                    "name" to playerName
                )
            ),
            "maxPlayers" to maxPlayers,
            "password" to password,
            "gameState" to mapOf(
                "round" to 0,
                "questions" to listOf<String>(),
                "answers" to listOf<String>()
            )
        )

        database.child("rooms").child(lobbyName).setValue(roomData).addOnCompleteListener { task ->
            if (task.isSuccessful) {
                val intent = Intent(this, LobbyActivity::class.java)
                intent.putExtra("roomName", lobbyName)
                intent.putExtra("playerName", playerName)
                startActivity(intent)
            } else {
                Log.e("MainActivity", "Failed to create room due to ${task.exception?.message}")
            }
        }
    }
}
