package com.example.anonymousgame

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import androidx.appcompat.app.AlertDialog
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
        val returnButton = findViewById<Button>(R.id.returnButton)
        playerName = intent.getStringExtra("playerName") ?: "Player${(100000..999999).random()}"

        findViewById<Button>(R.id.joinLobbyButton).setOnClickListener {
            val lobbyName = findViewById<EditText>(R.id.lobbyNameInput).text.toString()
            val password = findViewById<EditText>(R.id.passwordInput).text.toString()

            joinSpecificLobby(lobbyName, password)
        }

        returnButton.setOnClickListener {
            finish()
        }
    }

    private fun joinSpecificLobby(lobbyName: String, password: String) {
        database.child("rooms").child(lobbyName).get().addOnSuccessListener { snapshot ->
            val storedPassword = snapshot.child("password").value as? String
            val maxPlayers = snapshot.child("maxPlayers").getValue(Int::class.java) ?: 4
            val playersCount = snapshot.child("players").childrenCount.toInt()
            val currentRound = snapshot.child("gameState").child("round").getValue(Int::class.java) ?: 0

            if (playersCount < maxPlayers && currentRound == 0) {
                if (storedPassword == password) {
                    val playersRef = database.child("rooms").child(lobbyName).child("players")
                    val playerData = mapOf("name" to playerName, "ready" to false)
                    playersRef.child(playerName).setValue(playerData).addOnCompleteListener { task ->
                        if (task.isSuccessful) {
                            val intent = Intent(this, LobbyActivity::class.java)
                            intent.putExtra("roomName", lobbyName)
                            intent.putExtra("playerName", playerName)
                            startActivity(intent)
                        } else {
                            Log.e("JoinSpecificLobbyActivity", "Failed to add player: ${task.exception?.message}")
                        }
                    }
                } else {
                    Log.e("JoinSpecificLobbyActivity", "Incorrect password")
                }
            } else if (currentRound > 0) {
                AlertDialog.Builder(this)
                    .setMessage("This lobby has already started!")
                    .setPositiveButton("OK", null)
                    .show()
            } else {
                AlertDialog.Builder(this)
                    .setMessage("This lobby is full!")
                    .setPositiveButton("OK", null)
                    .show()
            }
        }.addOnFailureListener {
            Log.e("JoinSpecificLobbyActivity", "Lobby not found")
        }
    }
}
