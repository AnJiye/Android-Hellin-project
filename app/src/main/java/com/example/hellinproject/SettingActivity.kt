package com.example.hellinproject

import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import kotlinx.android.synthetic.main.activity_setting.*

class SettingActivity : AppCompatActivity() {
    var PICK_IMAGE_FROM_ALBUM = 0
    var photoUri : Any? = null
    var uid = FirebaseAuth.getInstance().currentUser?.uid
    var database : FirebaseDatabase? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_setting)

        database = FirebaseDatabase.getInstance()
        var databaseRef : DatabaseReference = database!!.reference

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
            var nickname = setting_nickname_input.text.toString()
            var height = Integer.parseInt(setting_height_input.text.toString())
            var weight = Integer.parseInt(setting_weight_input.text.toString())
            databaseRef.child("users").child(uid!!).child("nickname").setValue(nickname)
            databaseRef.child("users").child(uid!!).child("height").setValue(height)
            databaseRef.child("users").child(uid!!).child("weight").setValue(weight)
            databaseRef.child("users").child(uid!!).child("userProfile").setValue(photoUri.toString())

            val intent = Intent(this, MypageActivity::class.java)
            startActivity(intent)
        }

        databaseRef.child("users").child(uid!!).addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
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
        var databaseRef : DatabaseReference = database!!.reference
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
}