package com.example.hellinproject

import android.app.Activity
import android.app.AlarmManager
import android.app.PendingIntent
import android.app.TimePickerDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.example.hellinproject.Constant.Companion.ALARM_TIMER
import com.example.hellinproject.Constant.Companion.NOTIFICATION_ID
import com.google.android.gms.tasks.Task
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.UploadTask
import kotlinx.android.synthetic.main.activity_setting.*
import kotlinx.android.synthetic.main.profile_setting.*
import java.text.SimpleDateFormat
import java.util.*

// !! 앨범에서 사진 선택 안했을 시 FileNotFoundException 처리해야함
class SettingActivity : AppCompatActivity() {
//    @RequiresApi(Build.VERSION_CODES.O)
//    var now = LocalDateTime.now()
    var PICK_IMAGE_FROM_ALBUM = 0
    var photoUri : Any? = null
    var uid = FirebaseAuth.getInstance().currentUser?.uid
    var database : FirebaseDatabase? = null
    var storage : FirebaseStorage? = null
    lateinit var cal : Calendar

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_setting)

        storage = FirebaseStorage.getInstance()
        database = FirebaseDatabase.getInstance()
        var databaseRef : DatabaseReference = database!!.reference

        val alarmManager = getSystemService(ALARM_SERVICE) as AlarmManager

        val intent = Intent(this, MyReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            this, NOTIFICATION_ID, intent,
            PendingIntent.FLAG_UPDATE_CURRENT
        )

        switch1.setOnCheckedChangeListener { _, check ->
            val toastMessage = if (check) {
                val repeatInterval : Long = ALARM_TIMER * 1000L
                alarmManager.setRepeating(
                    AlarmManager.RTC_WAKEUP,
                    cal.timeInMillis,
                    repeatInterval,
                    pendingIntent
                )
                val time = SimpleDateFormat("HH:mm").format(cal.time)
                "${time}에 알림이 설정되었습니다."
            } else {
                alarmManager.cancel(pendingIntent)
                "알림 예약을 취소하였습니다."
            }
            Toast.makeText(this, toastMessage.toString(), Toast.LENGTH_SHORT).show()
        }

        time_btn.setOnClickListener {
            cal = Calendar.getInstance()
            val timeSetListener = TimePickerDialog.OnTimeSetListener {timePicker, hour, minute ->
                cal.set(Calendar.HOUR_OF_DAY, hour)
                cal.set(Calendar.MINUTE, minute)

                time_btn.text = SimpleDateFormat("HH:mm").format(cal.time)
            }
            TimePickerDialog(this, timeSetListener, cal.get(Calendar.HOUR_OF_DAY), cal.get(Calendar.MINUTE), true).show()
        }

        setting_profile_image_btn.setOnClickListener {
            ActivityCompat.requestPermissions(this, arrayOf(android.Manifest.permission.READ_EXTERNAL_STORAGE), 1)
            if(ContextCompat.checkSelfPermission(this, android.Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
                // Open the album
                var photoPickerIntent = Intent(Intent.ACTION_PICK)
                photoPickerIntent.type = "image/*"
                startActivityForResult(photoPickerIntent, PICK_IMAGE_FROM_ALBUM)
            }
        }

        setting_back_btn.setOnClickListener {
            profileUpload()
            val intent = Intent(this, MypageActivity::class.java)
            startActivity(intent)
        }

        databaseRef.child("users").child(uid!!).addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (this@SettingActivity.isFinishing)
                    return;
                setting_nickname_input.setText(snapshot.child("nickname").value as String?)
                setting_height_input.setText(snapshot.child("height").value.toString())
                setting_weight_input.setText(snapshot.child("weight").value.toString())
                photoUri = snapshot.child("userProfile").value
                Glide.with(this@SettingActivity).load(photoUri).apply(RequestOptions.circleCropTransform()).into(setting_profile_image_btn)
            }
            override fun onCancelled(error: DatabaseError) {
            }
        })
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == PICK_IMAGE_FROM_ALBUM)
            if(resultCode == Activity.RESULT_OK) {
                // This is path to the selected image
                photoUri = data?.data
                Glide.with(this@SettingActivity).load(photoUri).apply(RequestOptions.circleCropTransform()).into(setting_profile_image_btn)
            } else {
                // exit the ProfileSettingActivity if you leave the album without selecting it
                finish()
            }
    }

    fun profileUpload() {
        // Make file name
        var timestamp = SimpleDateFormat("yyyyMMdd_HHmmss").format(Date())
        var imageFileName = "IMAGE_" + timestamp + "_.png"

        var storageRef = storage?.reference?.child("images")?.child(imageFileName)
        var databaseRef : DatabaseReference = database!!.reference

        var profileUri = Uri.parse(photoUri.toString())

        // File upload
        storageRef?.putFile(profileUri!!)?.continueWithTask { task: Task<UploadTask.TaskSnapshot> ->
            return@continueWithTask storageRef.downloadUrl
        }?.addOnSuccessListener { uri ->
            var nickname = setting_nickname_input.text.toString()
            var height = Integer.parseInt(setting_height_input.text.toString())
            var weight = Integer.parseInt(setting_weight_input.text.toString())

            databaseRef.child("users").child(uid!!).child("nickname").setValue(nickname)
            databaseRef.child("users").child(uid!!).child("height").setValue(height)
            databaseRef.child("users").child(uid!!).child("weight").setValue(weight)
            databaseRef.child("users").child(uid!!).child("userProfile").setValue(uri.toString())

            setResult(Activity.RESULT_OK)
            finish()
        }
    }
}