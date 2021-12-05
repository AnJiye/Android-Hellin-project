package com.example.hellinproject

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.hellinproject.camera.LaunchActivity
import kotlinx.android.synthetic.main.profile_setting.*

class ProfileSettingActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.profile_setting)

        profile_next_btn.setOnClickListener {
            val intent = Intent(this, SurveyActivity::class.java)
            startActivity(intent)
        }
    }
}