package com.example.anonymousgame

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.ArrayAdapter
import android.widget.ListView
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class BrowseLobbiesActivity : AppCompatActivity() {

    private lateinit var database: DatabaseReference
    private lateinit var lobbiesListView: ListView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_browse_lobbies)

        lobbiesListView = findViewById(R.id.lobbiesListView)
        database = FirebaseDatabase.getInstance("https://anonymousgame-7c8ee-default-rtdb.europe-west1.firebasedatabase.app").reference

        loadLobbies()
    }

    private fun loadLobbies() {
        database.child("rooms").addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val lobbies = snapshot.children.map { it.key ?: "" }.toList()
                val adapter = ArrayAdapter(this@BrowseLobbiesActivity, android.R.layout.simple_list_item_1, lobbies)
                lobbiesListView.adapter = adapter

                lobbiesListView.setOnItemClickListener { _, _, position, _ ->
                    val selectedLobby = lobbies[position]
                    val intent = Intent(this@BrowseLobbiesActivity, LobbyActivity::class.java)
                    intent.putExtra("roomName", selectedLobby)
                    startActivity(intent)
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("BrowseLobbiesActivity", "Error: ${error.message}")
            }
        })
    }
}