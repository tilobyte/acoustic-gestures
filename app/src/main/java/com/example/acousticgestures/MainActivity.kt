package com.example.acousticgestures

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.content.Intent
import android.view.View
import com.paramsen.noise.Noise


class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
    }

    fun startTone(view: View) {
        // do something
        val intent = Intent(this, StartToneActivity::class.java).apply {}
        startActivity(intent)
    }

//    private fun start() {
//        val src = AudioWriter().stream()
//        val noise = Noise.real(4096)
//        // do something
//        val intent = Intent(this, AudioWriter::class.java).apply {}
//        startActivity(intent)
//    }

}

