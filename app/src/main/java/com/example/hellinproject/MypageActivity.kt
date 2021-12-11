package com.example.hellinproject

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.View
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import kotlinx.android.synthetic.main.activity_mypage.*
import java.time.LocalDate
import java.time.format.DateTimeFormatter

class MypageActivity : AppCompatActivity() {
    @RequiresApi(Build.VERSION_CODES.O)
    var now = LocalDate.now()
    var uid = FirebaseAuth.getInstance().currentUser?.uid
    var database : FirebaseDatabase? = null

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_mypage)

        database = FirebaseDatabase.getInstance()
        var databaseRef : DatabaseReference = database!!.reference

        date_picker.text = now.toString()

        val formatter = DateTimeFormatter.ofPattern("yyyyMMdd")
        val formatted = now.format(formatter)

        setting_btn.setOnClickListener {
            val intent = Intent(this, SettingActivity::class.java)
            startActivity(intent)
        }

        mypage_back_btn.setOnClickListener {
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
        }

        databaseRef.child("users").child(uid!!).addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                mypage_username.text = snapshot.child("nickname").value as CharSequence?
                var photoUri = snapshot.child("userProfile").value
                Glide.with(this@MypageActivity).load(photoUri).apply(RequestOptions.circleCropTransform()).into(mypage_image)

                var count = snapshot.child("exercise").child(formatted).child("squatCount").value
                squat_info_btn.text = "${count}회"
            }
            override fun onCancelled(error: DatabaseError) {
            }
        })
    }
}