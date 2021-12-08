package com.example.hellinproject

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.example.hellinproject.dto.UserDTO
import com.google.android.gms.tasks.Task
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.UploadTask
import kotlinx.android.synthetic.main.profile_setting.*
import java.text.SimpleDateFormat
import java.util.*

class ProfileSettingActivity : AppCompatActivity() {
    var PICK_IMAGE_FROM_ALBUM = 0
    var storage : FirebaseStorage? = null
    var photoUri : Uri? = null
    var auth : FirebaseAuth? = null
    var firestore : FirebaseFirestore? = null
    var inputNickname : String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.profile_setting)

        storage = FirebaseStorage.getInstance()
        auth = FirebaseAuth.getInstance()
        firestore = FirebaseFirestore.getInstance()

        profile_setting_btn.setOnClickListener {
            ActivityCompat.requestPermissions(this, arrayOf(android.Manifest.permission.READ_EXTERNAL_STORAGE), 1)
            if(ContextCompat.checkSelfPermission(this, android.Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
                // Open the album
                var photoPickerIntent = Intent(Intent.ACTION_PICK)
                photoPickerIntent.type = "image/*"
                startActivityForResult(photoPickerIntent, PICK_IMAGE_FROM_ALBUM)
            }
        }

        profile_next_btn.setOnClickListener {
            profileUpload()
            val intent = Intent(this, SurveyActivity::class.java)
            startActivity(intent)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == PICK_IMAGE_FROM_ALBUM)
            if(resultCode == Activity.RESULT_OK) {
                // This is path to the selected image
                photoUri = data?.data
                Glide.with(this).load(photoUri).apply(RequestOptions.circleCropTransform()).into(profile_image)
//                profile_image.setImageURI(photoUri)
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

        // File upload
//        storageRef?.putFile(photoUri!!)?.addOnSuccessListener {
////            Toast.makeText(this, "Upload success", Toast.LENGTH_LONG).show()
//            storageRef.downloadUrl.addOnSuccessListener { uri ->

        storageRef?.putFile(photoUri!!)?.continueWithTask { task: Task<UploadTask.TaskSnapshot> ->
            return@continueWithTask storageRef.downloadUrl
        }?.addOnSuccessListener { uri ->
            var userDTO = UserDTO()

            // Insert downloadUrl of image
            userDTO.userProfile = uri.toString()

            // Insert uid of user
            userDTO.uid = auth?.currentUser?.uid

            // Insert userId
            inputNickname = nickname_input.text.toString()
            userDTO.nickname = inputNickname

            // Insert timestamp
            userDTO.timestamp = System.currentTimeMillis()

            firestore?.collection("images")?.document()?.set(userDTO)

            setResult(Activity.RESULT_OK)

            finish()
        }
    }
}