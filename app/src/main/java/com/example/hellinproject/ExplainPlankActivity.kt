package com.example.hellinproject

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import kotlinx.android.synthetic.main.explain_plank.*

class ExplainPlankActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.explain_plank)

        // 버튼 액션이랑 코틀린 파일 동작 연결하기
        plank_start_btn.setOnClickListener {
            val intent = Intent(this, LaunchActivity::class.java)
            startActivity(intent)
            // 페이지 recording_video_plank로 이동
            // 카메라 실행
        }

        back_btn.setOnClickListener {
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
        }
    }
}