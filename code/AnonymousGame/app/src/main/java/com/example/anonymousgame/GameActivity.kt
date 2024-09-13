package com.example.anonymousgame

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener

class GameActivity : AppCompatActivity() {

    private lateinit var database: DatabaseReference
    private lateinit var roomName: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_game)

        // Get the room name from the intent
        roomName = intent.getStringExtra("roomName") ?: ""

        // Initialize the Firebase database reference
        database = FirebaseDatabase.getInstance("https://anonymousgame-7c8ee-default-rtdb.europe-west1.firebasedatabase.app")
            .getReference("rooms").child(roomName)

        // Set up the exit button with confirmation
        findViewById<Button>(R.id.exitGameButton).setOnClickListener {
            confirmExitGame()
        }
    }

    // Ask for confirmation before exiting the game
    private fun confirmExitGame() {
        val builder = android.app.AlertDialog.Builder(this)
        builder.setTitle("Exit Game")
        builder.setMessage("Are you sure you want to quit the game?")

        builder.setPositiveButton("Yes") { dialog, which ->
            exitGame()
        }

        builder.setNegativeButton("No") { dialog, which ->
            dialog.dismiss()
        }

        builder.show()
    }

    // Exit the game and clean up the lobby if necessary
    private fun exitGame() {
        // Remove the current player from the lobby
        val playerRef = database.child("players").child("HostName")  // Adjust this to the correct player
        playerRef.removeValue().addOnCompleteListener {
            // Check if the lobby is empty and delete it if necessary
            checkAndDeleteLobby()

            // Return to the main activity
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
            finish() // Close the game activity
        }
    }

    // Check if the lobby is empty and delete it if there are no players
    private fun checkAndDeleteLobby() {
        database.child("players").addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (!snapshot.hasChildren()) {
                    // If no players are left, delete the lobby
                    database.removeValue()
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("GameActivity", "Failed to check players: ${error.message}")
            }
        })
    }
}
