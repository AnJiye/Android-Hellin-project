package com.example.hellinproject.camera

import android.Manifest
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.example.hellinproject.R
import com.gun0912.tedpermission.PermissionListener
import com.gun0912.tedpermission.TedPermission

class LaunchActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_launch)

        // 카메라 권한이 부여되었는지 확인하고 카메라를 실행하거나 에러로그를 띄움
        TedPermission.with(this)
            .setPermissionListener(object : PermissionListener{
                override fun onPermissionGranted() {
                    startActivity(Intent(this@LaunchActivity, CameraActivity::class.java))
                    finish()
                }

                override fun onPermissionDenied(deniedPermissions: MutableList<String>?) {
                    for(i in deniedPermissions!!)
                        i.showErrLog()
                }
            })
            .setDeniedMessage("앱을 실행하려면 권한을 허가하셔야합니다.")
            .setPermissions(Manifest.permission.CAMERA)
            .check()
    }
}