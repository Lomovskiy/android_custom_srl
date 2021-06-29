package com.lomovskiy.customsrl

import android.graphics.drawable.AnimationDrawable
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Button
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout

class MainActivity : AppCompatActivity() {

    private lateinit var srl: Srl

//    private lateinit var loaderView: LoaderView

//    private lateinit var buttonStart: Button
//    private lateinit var buttonStop: Button

//    private lateinit var animation: AnimationDrawable

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        srl = findViewById(R.id.srl)

//        loaderView = findViewById(R.id.loader_view)
//        buttonStart = findViewById(R.id.button_start)
//        buttonStop = findViewById(R.id.button_stop)

//        buttonStart.setOnClickListener {
//            animation.start()
//        }
//        buttonStop.setOnClickListener {
//            animation.stop()
//        }
//
//        animation = loaderView.background as AnimationDrawable
    }

}
