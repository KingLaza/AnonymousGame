package com.example.anonymousgame

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.ListView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class BrowseLobbiesActivity : AppCompatActivity() {

    private lateinit var database: DatabaseReference
    private lateinit var lobbiesListView: ListView
    private lateinit var playerName: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_browse_lobbies)

        lobbiesListView = findViewById(R.id.lobbiesListView)
        database = FirebaseDatabase.getInstance("https://anonymousgame-7c8ee-default-rtdb.europe-west1.firebasedatabase.app").reference

        playerName = intent.getStringExtra("playerName") ?: "Player${(100000..999999).random()}"
        val returnButton = findViewById<Button>(R.id.returnButton)
        returnButton.setOnClickListener {
            finish()
        }

        loadLobbies()
    }

    private fun loadLobbies() {
        database.child("rooms").addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val lobbies = snapshot.children.mapNotNull { roomSnapshot ->
                    val roomName = roomSnapshot.key ?: return@mapNotNull null
                    val maxPlayers = roomSnapshot.child("maxPlayers").getValue(Int::class.java) ?: return@mapNotNull null
                    val playersCount = roomSnapshot.child("players").childrenCount.toInt()
                    val currentRound = roomSnapshot.child("gameState").child("round").getValue(Int::class.java) ?: 0

                    // Only show lobbies that haven't started (round == 0) and have space
                    if (currentRound == 0 && playersCount < maxPlayers) {
                        "$roomName ($playersCount/$maxPlayers)"
                    } else {
                        null
                    }
                }

                val adapter = ArrayAdapter(this@BrowseLobbiesActivity, android.R.layout.simple_list_item_1, lobbies)
                lobbiesListView.adapter = adapter

                lobbiesListView.setOnItemClickListener { _, _, position, _ ->
                    val selectedLobby = lobbies[position].substringBefore(" (")
                    database.child("rooms").child(selectedLobby).child("password").addListenerForSingleValueEvent(object : ValueEventListener {
                        override fun onDataChange(passwordSnapshot: DataSnapshot) {
                            val password = passwordSnapshot.getValue(String::class.java)
                            if (!password.isNullOrEmpty()) {
                                promptForPassword(selectedLobby, password)
                            } else {
                                joinLobby(selectedLobby)
                            }
                        }

                        override fun onCancelled(error: DatabaseError) {
                            Log.e("BrowseLobbiesActivity", "Error: ${error.message}")
                        }
                    })
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("BrowseLobbiesActivity", "Error: ${error.message}")
            }
        })
    }

    private fun promptForPassword(lobbyName: String, correctPassword: String) {
        val passwordInput = EditText(this)
        AlertDialog.Builder(this)
            .setTitle("Enter Password")
            .setView(passwordInput)
            .setPositiveButton("Join") { _, _ ->
                if (passwordInput.text.toString() == correctPassword) {
                    joinLobby(lobbyName)
                } else {
                    AlertDialog.Builder(this)
                        .setMessage("Incorrect Password!")
                        .setPositiveButton("OK", null)
                        .show()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun joinLobby(lobbyName: String) {
        val intent = Intent(this, LobbyActivity::class.java)
        intent.putExtra("roomName", lobbyName)
        intent.putExtra("playerName", playerName)
        startActivity(intent)
    }
}
