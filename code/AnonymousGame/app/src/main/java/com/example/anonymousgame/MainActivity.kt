package com.example.anonymousgame

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import androidx.appcompat.app.AppCompatActivity
import com.example.anonymousgame.LobbyActivity
import com.example.anonymousgame.R
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class MainActivity : AppCompatActivity() {

    private lateinit var database: DatabaseReference

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        database = FirebaseDatabase.getInstance("https://anonymousgame-7c8ee-default-rtdb.europe-west1.firebasedatabase.app").reference

        val lobbyNameEditText = findViewById<EditText>(R.id.lobbyNameEditText)
        val maxPlayersEditText = findViewById<EditText>(R.id.maxPlayersEditText)
        val passwordEditText = findViewById<EditText>(R.id.passwordEditText)

        findViewById<Button>(R.id.createRoomButton).setOnClickListener {
            val lobbyName = lobbyNameEditText.text.toString()
            val maxPlayers = maxPlayersEditText.text.toString().toIntOrNull() ?: 4 // Default to 4 players
            val password = passwordEditText.text.toString()

            createRoom(lobbyName, maxPlayers, password)
        }

        findViewById<Button>(R.id.joinRoomButton).setOnClickListener {
            // Here you could implement logic to join a specific or random room
            val intent = Intent(this, LobbyActivity::class.java)
            startActivity(intent)
        }

        findViewById<Button>(R.id.joinSpecificLobbyButton).setOnClickListener {
            val intent = Intent(this, JoinSpecificLobbyActivity::class.java)
            startActivity(intent)
        }

        findViewById<Button>(R.id.joinRandomLobbyButton).setOnClickListener {
            joinRandomLobby()
        }

        findViewById<Button>(R.id.browseLobbiesButton).setOnClickListener {
            val intent = Intent(this, BrowseLobbiesActivity::class.java)
            startActivity(intent)
        }

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
                    // Handle no available lobbies
                    Log.e("MainActivity", "No lobbies available")
                }
            }

            override fun onCancelled(error: DatabaseError) {
                // Handle error
                Log.e("MainActivity", "Error: ${error.message}")
            }
        })
    }


    private fun createRoom(lobbyName: String, maxPlayers: Int, password: String?) {
        val roomData = mapOf(
            "host" to "HostName",
            "players" to mapOf("HostName" to mapOf("name" to "Host Name")),
            "maxPlayers" to maxPlayers,
            "password" to password,
            "gameState" to mapOf(
                "round" to 0,
                "questions" to listOf<String>(),
                "answers" to listOf<String>()
            )
        )

        // Save the room data under the 'rooms' section
        database.child("rooms").child(lobbyName).setValue(roomData).addOnCompleteListener { task ->
            if (task.isSuccessful) {
                val intent = Intent(this, LobbyActivity::class.java)
                intent.putExtra("roomName", lobbyName)
                Log.i("MainActivity", "Succeeded in creating a room with these parameters: $roomData")
                startActivity(intent)
            } else {
                Log.e("MainActivity", "Failed to create a room with these parameters due to ${task.exception?.message}")
            }
        }
    }
}