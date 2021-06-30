package com.lomovskiy.customsrl

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle

class MainActivity : AppCompatActivity() {

    private lateinit var srl: Srl
    private lateinit var progressBar: EAProgressBar

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        srl = findViewById(R.id.srl)
        progressBar = findViewById(R.id.progress_bar)
    }

}
