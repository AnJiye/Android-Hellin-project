package com.example.hellinproject

import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import kotlinx.android.synthetic.main.show_work_out_done_squat.*
import java.time.LocalDate
import java.time.format.DateTimeFormatter

// squat_count_textview
// squat_time_textview
class ResultSquatActivity : AppCompatActivity() {
    @RequiresApi(Build.VERSION_CODES.O)
    var now = LocalDate.now()
    var uid = FirebaseAuth.getInstance().currentUser?.uid
    var database : FirebaseDatabase? = null

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.show_work_out_done_squat)

        database = FirebaseDatabase.getInstance()
        var databaseRef : DatabaseReference = database!!.reference
        today.text = now.toString()

        val formatter = DateTimeFormatter.ofPattern("yyyyMMdd")
        val formatted = now.format(formatter)

        squat_finish_btn.setOnClickListener {
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
        }

        databaseRef.child("users").child(uid!!).child("exercise").child(formatted).addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                var time = snapshot.child("squatTime").value as Long?
                val min = (time?.div(100))?.div(60)
                val sec = (time?.div(100))?.rem(60)
                squat_time_textview.text = "${min}분 ${sec}초"

                var count = snapshot.child("squatCount").value
                squat_count_textview.text = "${count}회"

                var calorie = snapshot.child("squatCalorie").value
                squat_calories_textview.text = "${calorie}kcal"
            }
            override fun onCancelled(error: DatabaseError) {
            }
        })
    }
}