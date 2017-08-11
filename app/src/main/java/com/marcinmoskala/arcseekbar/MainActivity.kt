package com.marcinmoskala.arcseekbar

import android.graphics.Color
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        seekArc.onProgressChangedListener = { p ->
            Log.i("SeekBar", "Value is $p")
        }
        val colors = resources.getIntArray(R.array.sliderArc)
        seekArc.setProgressGradient(*colors)
    }

    fun toast(text: String) {
        Toast.makeText(this, text, Toast.LENGTH_SHORT).show()
    }
}
