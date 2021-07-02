package com.lomovskiy.customsrl

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Looper
import android.widget.Toast

class MainActivity : AppCompatActivity(), EASwipeToRefreshLayout.OnRefreshListener {

    private lateinit var srl: EASwipeToRefreshLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        srl = findViewById(R.id.srl)
        srl.setOnRefreshListener(this)
    }

    override fun onRefresh() {
        Toast.makeText(this, "onRefresh()", Toast.LENGTH_SHORT).show()
        android.os.Handler(Looper.getMainLooper()).postDelayed({
            srl.stopRefreshing()
        }, 500)
    }

}
