package com.example.hellinproject

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import kotlinx.android.synthetic.main.recording_video_pushup.*

class RecordingVideoPushupActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.recording_video_pushup)

        pause_btn.setOnClickListener {
            // 녹화 일시정지
        }

        stop_btn.setOnClickListener {
            // 녹화 정지 및 운동 기록
        }
    }
}