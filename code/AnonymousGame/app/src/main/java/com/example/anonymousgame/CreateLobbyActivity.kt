package com.example.anonymousgame

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class CreateLobbyActivity : AppCompatActivity() {

    private lateinit var database: DatabaseReference
    private lateinit var playerName: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_create_lobby)

        database = FirebaseDatabase.getInstance("https://anonymousgame-7c8ee-default-rtdb.europe-west1.firebasedatabase.app").reference

        playerName = intent.getStringExtra("playerName") ?: "UnknownPlayer"

        val lobbyNameEditText = findViewById<EditText>(R.id.lobbyNameEditText)
        val maxPlayersEditText = findViewById<EditText>(R.id.maxPlayersEditText)
        val passwordEditText = findViewById<EditText>(R.id.passwordEditText)
        val returnButton = findViewById<Button>(R.id.returnButton)
        findViewById<Button>(R.id.createLobbyFinalButton).setOnClickListener {
            val lobbyName = lobbyNameEditText.text.toString().ifEmpty { "Lobby_${System.currentTimeMillis()}" } // Default name if empty
            val maxPlayers = maxPlayersEditText.text.toString().toIntOrNull() ?: 4 // Default to 4 players
            val password = passwordEditText.text.toString()

            if (maxPlayers < 2) {
                maxPlayersEditText.setText("2")
                Toast.makeText(this, "Minimum number of players is 2", Toast.LENGTH_SHORT).show()
            } else {
                createRoom(lobbyName, maxPlayers, password)
            }


        }
        returnButton.setOnClickListener {
            finish()
        }

    }

    private fun createRoom(lobbyName: String, maxPlayers: Int, password: String?) {
        // Check if a lobby with this name already exists
        database.child("rooms").child(lobbyName).addListenerForSingleValueEvent(object :
            ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.exists()) {
                    AlertDialog.Builder(this@CreateLobbyActivity)
                        .setTitle("Error")
                        .setMessage("Lobby name already exists. Please choose another name.")
                        .setPositiveButton("OK") { dialog, _ -> dialog.dismiss() }
                        .show()
                } else {
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
                            val intent = Intent(this@CreateLobbyActivity, LobbyActivity::class.java)
                            intent.putExtra("roomName", lobbyName)
                            intent.putExtra("playerName", playerName)
                            startActivity(intent)
                        } else {
                            Log.e("CreateLobbyActivity", "Failed to create room due to ${task.exception?.message}")
                        }
                    }
                }
            }
            override fun onCancelled(error: DatabaseError) {
                Log.e("CreateLobbyActivity", "Database error: ${error.message}")
            }
        })
    }

}
