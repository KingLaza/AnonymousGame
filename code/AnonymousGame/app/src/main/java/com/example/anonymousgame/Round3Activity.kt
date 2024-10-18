package com.example.anonymousgame

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase

class Round3Activity : AppCompatActivity() {

    private lateinit var database: DatabaseReference
    private lateinit var roomName: String
    private lateinit var playerName: String



    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_round_3)

        roomName = intent.getStringExtra("roomName") ?: ""
        playerName = intent.getStringExtra("playerName") ?: ""
        database = FirebaseDatabase.getInstance("https://anonymousgame-7c8ee-default-rtdb.europe-west1.firebasedatabase.app")
            .getReference("rooms").child(roomName)
    }

}