package com.example.hellinproject

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.hellinproject.camera.LaunchActivity
import kotlinx.android.synthetic.main.explain_plank.*

class ExplainPlankActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.explain_plank)

        plank_start_btn.setOnClickListener {
            val intent = Intent(this, LaunchActivity::class.java)
            startActivity(intent)
            println("버튼을 눌렀습니다.")
        }
    }
}