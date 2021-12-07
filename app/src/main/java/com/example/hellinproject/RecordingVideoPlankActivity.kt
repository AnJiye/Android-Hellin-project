package com.example.hellinproject

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.hellinproject.camera.LaunchActivity
import kotlinx.android.synthetic.main.explain_plank.*
import kotlinx.android.synthetic.main.recording_video_plank.*

class RecordingVideoPlankActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.recording_video_plank)

        pause_btn.setOnClickListener {
            // 녹화 일시정지
        }

        stop_btn.setOnClickListener {
            // 녹화 정지 및 운동 기록
        }
    }
}