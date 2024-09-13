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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_join_specific_lobby)

        database = FirebaseDatabase.getInstance("https://anonymousgame-7c8ee-default-rtdb.europe-west1.firebasedatabase.app").reference

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
                val intent = Intent(this, LobbyActivity::class.java)
                intent.putExtra("roomName", lobbyName)
                startActivity(intent)
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