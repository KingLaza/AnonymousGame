package com.example.anonymousgame

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase

class JoinSpecificLobbyActivity : AppCompatActivity() {

    private lateinit var database: DatabaseReference
    private lateinit var playerName: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_join_specific_lobby)

        database = FirebaseDatabase.getInstance("https://anonymousgame-7c8ee-default-rtdb.europe-west1.firebasedatabase.app").reference

        // Retrieve playerName from intent or generate a default
        playerName = intent.getStringExtra("playerName") ?: "Player${(100000..999999).random()}"

        findViewById<Button>(R.id.joinLobbyButton).setOnClickListener {
            val lobbyName = findViewById<EditText>(R.id.lobbyNameInput).text.toString()
            val password = findViewById<EditText>(R.id.passwordInput).text.toString()

            // Verify lobby and password
            joinSpecificLobby(lobbyName, password)
        }
    }


    private fun joinSpecificLobby(lobbyName: String, password: String) {
        database.child("rooms").child(lobbyName).get().addOnSuccessListener { snapshot ->
            val storedPassword = snapshot.child("password").value as? String
            if (storedPassword == password) {
                // Add the player to the lobby's player list
                val playersRef = database.child("rooms").child(lobbyName).child("players")
                val playerData = mapOf("name" to playerName)
                playersRef.child(playerName).setValue(playerData).addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        val intent = Intent(this, LobbyActivity::class.java)
                        intent.putExtra("roomName", lobbyName)
                        intent.putExtra("playerName", playerName) // Pass player name to LobbyActivity
                        startActivity(intent)
                    } else {
                        Log.e("JoinSpecificLobbyActivity", "Failed to add player: ${task.exception?.message}")
                    }
                }
            } else {
                // Handle incorrect password
                Log.e("JoinSpecificLobbyActivity", "Incorrect password")
            }
        }.addOnFailureListener {
            // Handle lobby not found
            Log.e("JoinSpecificLobbyActivity", "Lobby not found")
        }
    }

}