package com.example.hellinproject

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import kotlinx.android.synthetic.main.explain_pushup.*

class ExplainPushupActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.explain_pushup)

        pushup_start_btn.setOnClickListener {
            val intent = Intent(this, LaunchActivity::class.java)
            startActivity(intent)
        }

        back_btn.setOnClickListener {
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
        }
    }
}