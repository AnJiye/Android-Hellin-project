package com.example.hellinproject

import android.content.Intent
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.annotation.RequiresApi
import kotlinx.android.synthetic.main.activity_main.*
import java.time.LocalDate

class MainActivity : AppCompatActivity() {
    @RequiresApi(Build.VERSION_CODES.O)
    var now = LocalDate.now()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // 오늘 날짜
        today_textview.text = now.toString()

        // 스쿼트 버튼
        squat_btn.setOnClickListener {
            val intent = Intent(this, ExplainSquatActivity::class.java)
            startActivity(intent)
        }
        // 플랭크 버튼
        plank_btn.setOnClickListener {
            val intent = Intent(this, ExplainPlankActivity::class.java)
            startActivity(intent)
        }
        // 푸시업 버튼
        pushup_btn.setOnClickListener {
            val intent = Intent(this, ExplainPushupActivity::class.java)
            startActivity(intent)
        }
        // 마이페이지
        mypage_btn.setOnClickListener {
            val intent = Intent(this, MypageActivity::class.java)
            startActivity(intent)
        }
    }
}