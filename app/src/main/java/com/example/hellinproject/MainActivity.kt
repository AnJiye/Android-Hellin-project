package com.example.hellinproject

import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.annotation.RequiresApi
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.profile_setting.*
import java.time.LocalDate
import java.time.format.DateTimeFormatter

class MainActivity : AppCompatActivity() {
    @RequiresApi(Build.VERSION_CODES.O)
    var now = LocalDate.now()
    var uid = FirebaseAuth.getInstance().currentUser?.uid
    var database : FirebaseDatabase? = null

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        database = FirebaseDatabase.getInstance()
        var databaseRef : DatabaseReference = database!!.reference

        // 오늘 날짜
        today_textview.text = now.toString()

        val formatter = DateTimeFormatter.ofPattern("yyyyMMdd")
        val formatted = now.format(formatter)

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

        databaseRef.child("users").child(uid!!).addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                username_textview.text = snapshot.child("nickname").value as CharSequence?
                var time = snapshot.child("exercise").child(formatted).child("squatTime").value as Long?
                val min = (time?.div(100))?.div(60)
//                val sec = (time?.div(100))?.rem(60)
                today_min_textview.text = "${min} 분"

                var count = snapshot.child("exercise").child(formatted).child("squatCount").value
                if (count == null) {
                    today_count_textview.text = "0 회"
                } else {
                    today_count_textview.text = "${count} 회"
                }

                var calorie =snapshot.child("exercise").child(formatted).child("squatCalorie").value
                if (calorie == null) {
                    today_kcal_textview.text = "0 kcal"
                } else {
                    today_kcal_textview.text = "${calorie} kcal"
                }
            }
            override fun onCancelled(error: DatabaseError) {
            }
        })
    }
}