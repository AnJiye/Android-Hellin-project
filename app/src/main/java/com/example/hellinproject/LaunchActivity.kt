package com.example.hellinproject

import android.Manifest
import android.app.AlertDialog
import android.app.Dialog
import android.content.DialogInterface
import android.content.Intent
import android.content.pm.PackageManager
import android.media.MediaPlayer
import android.os.Build
import android.os.Bundle
import android.os.CountDownTimer
import android.os.Process
import android.util.Log
import android.view.SurfaceView
import android.view.View
import android.view.WindowManager
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import com.example.hellinproject.camera.CameraSource
import com.example.hellinproject.data.Device
import com.example.hellinproject.ml.*
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import kotlinx.android.synthetic.main.activity_launch.*
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.*
import kotlin.concurrent.timer
import kotlin.math.roundToInt

class LaunchActivity : AppCompatActivity() {
    companion object {
        private const val FRAGMENT_DIALOG = "dialog"
    }

    @RequiresApi(Build.VERSION_CODES.O)
    var now = LocalDate.now()
    private val TAG = "[IC]LaunchActivity"
    private var time = 0
    private var timerTask : Timer? = null
    private var isRunning = true
    var uid = FirebaseAuth.getInstance().currentUser?.uid
    var database : FirebaseDatabase? = null

    /** A [SurfaceView] for camera preview.   */
    private lateinit var surfaceView: SurfaceView

    /** Default pose estimation model is 1 (MoveNet Thunder)
     * 0 == MoveNet Lightning model
     * 1 == MoveNet Thunder model
     * 2 == MoveNet MultiPose model
     * 3 == PoseNet model
     **/
    private var modelPos = 1

    /** Default device is CPU */
    private var device = Device.CPU

//    private lateinit var tvClassificationValue1: TextView
//    private lateinit var tvClassificationValue2: TextView
//    private lateinit var tvClassificationValue3: TextView
    private var cameraSource: CameraSource? = null
    private val requestPermissionLauncher =
        registerForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { isGranted: Boolean ->
            if (isGranted) {
                // Permission is granted. Continue the action or workflow in your
                // app.
                openCamera()
            } else {
                // Explain to the user that the feature is unavailable because the
                // features requires a permission that the user has denied. At the
                // same time, respect the user's decision. Don't link to system
                // settings in an effort to convince the user to change their
                // decision.
                ErrorDialog.newInstance("This app needs camera permission.")
                    .show(supportFragmentManager, FRAGMENT_DIALOG)
            }
        }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_launch)
        // keep screen on while app is running
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        surfaceView = findViewById(R.id.surfaceView)
//        tvClassificationValue1 = findViewById(R.id.tvClassificationValue1)
//        tvClassificationValue2 = findViewById(R.id.tvClassificationValue2)
//        tvClassificationValue3 = findViewById(R.id.tvClassificationValue3)

        database = FirebaseDatabase.getInstance()
        var databaseRef : DatabaseReference = database!!.reference

        val formatter = DateTimeFormatter.ofPattern("yyyyMMdd")
        val formatted = now.format(formatter)

        isPoseClassifier()

        if (!isCameraPermissionGranted()) {
            requestPermission()
        }

        // 종료 버튼
        // 칼로리 - 스쿼트 1세트(30개) 기준 13kcal => 1개당 약 0.44kcal
        // 추후에 키, 몸무게 기준으로 좀 더 정확하게 측정할 수 있도록 조정 필요
        stop_btn.setOnClickListener {
            pause()
            val intent = Intent(this, ResultSquatActivity::class.java)
            val dlg: AlertDialog.Builder = AlertDialog.Builder(this@LaunchActivity, android.R.style.Theme_DeviceDefault_Light_Dialog_NoActionBar_MinWidth)
            dlg.setTitle("정지")
            dlg.setMessage("운동을 종료하시겠습니까?")
            dlg.setPositiveButton("확인", DialogInterface.OnClickListener { dialogInterface, i ->
                val recordTime = time
                val recordCount = count
                val recordCalorie = (count * 0.44).roundToInt()

                databaseRef.child("users").child(uid!!).child("exercise").child(formatted).child("squatTime").setValue(recordTime)
                databaseRef.child("users").child(uid!!).child("exercise").child(formatted).child("squatCount").setValue(recordCount)
                databaseRef.child("users").child(uid!!).child("exercise").child(formatted).child("squatCalorie").setValue(recordCalorie)

                startActivity(intent)
                finish()
            })
            dlg.setNegativeButton("취소", DialogInterface.OnClickListener { dialogInterface, i ->
                counterStart()
            })
            dlg.show()
        }

        // 일시정지 버튼
        pause_btn.setOnClickListener {
            isRunning = !isRunning
            if (isRunning) counterStart() else pause()
        }

        val countDown = object : CountDownTimer(1000 * 3, 1000) {
            override fun onTick(p0: Long) {
                timer.text = (p0 / 1000 + 1).toString()
            }
            override fun onFinish() {
                var mediaplayer = MediaPlayer.create(this@LaunchActivity, R.raw.start).start()
                timer.visibility = View.GONE
                surfaceView.visibility = View.VISIBLE

                counterStart()
            }
        }.start()
    }

    private fun counterStart() {
        pause_btn.setImageResource(R.drawable.pause)
        timerTask = timer(period = 10) {
            time ++
            val min = (time / 100) / 60
            val sec = (time / 100) % 60
//            val milli = time % 100

            runOnUiThread {
//                txtTime?.text = "${min} : ${sec}"
            }
        }
    }

    private fun pause() {
        pause_btn.setImageResource(R.drawable.play_button)
        timerTask?.cancel()
    }

    override fun onStart() {
        super.onStart()
        openCamera()
    }

    override fun onResume() {
        cameraSource?.resume()
        super.onResume()
    }

    override fun onPause() {
        cameraSource?.close()
        cameraSource = null
        super.onPause()
    }

    // check if permission is granted or not.
    private fun isCameraPermissionGranted(): Boolean {
        return checkPermission(
            Manifest.permission.CAMERA,
            Process.myPid(),
            Process.myUid()
        ) == PackageManager.PERMISSION_GRANTED
    }

    var index : String? = null
    var score : Float? = null
    var status = "stand"
    var count = 0

    // open camera
    private fun openCamera() {
        if (isCameraPermissionGranted()) {
            if (cameraSource == null) {
                cameraSource =
                    CameraSource(surfaceView, object : CameraSource.CameraSourceListener {
                        override fun onFPSListener(fps: Int) {
                        }

                        override fun onDetectedInfo(
                            personScore: Float?,
                            poseLabels: List<Pair<String, Float>>?

                        ) {
                            poseLabels?.sortedByDescending { it.second }?.let {
                                var (first, second) = it!!
                                index = first.first
                                score = first.second

                                if (index == "stand" && score!! > 0.9) {
                                    if (status == "squat") {
                                        count++
                                        Log.d(TAG, count.toString())
                                        var soundTitle = "count${count%10}"
                                        var res = this@LaunchActivity.resources
                                        var soundId = res.getIdentifier(soundTitle, "raw", this@LaunchActivity.packageName)
                                        var mediaplayer = MediaPlayer.create(this@LaunchActivity, soundId).start()
                                    }
                                    status = "stand"
                                } else if (index == "squat" && score!! > 0.9) {
                                    status = "squat"
                                }

//                                tvClassificationValue1.text = getString(
//                                    R.string.tfe_pe_tv_classification_value,
//                                    convertPoseLabels(if (it.isNotEmpty()) it[0] else null)
//                                )
//                                tvClassificationValue2.text = getString(
//                                    R.string.tfe_pe_tv_classification_value,
//                                    convertPoseLabels(if (it.size >= 2) it[1] else null)
//                                )
//                                tvClassificationValue3.text = getString(
//                                    R.string.tfe_pe_tv_classification_value,
//                                    convertPoseLabels(if (it.size >= 3) it[2] else null)
//                                )
                            }
                        }

                    }).apply {
                        prepareCamera()
                    }
                isPoseClassifier()
                lifecycleScope.launch(Dispatchers.Main) {
                    cameraSource?.initCamera()
                }
            }
            createPoseEstimator()
        }
    }

    private fun convertPoseLabels(pair: Pair<String, Float>?): String {
        if (pair == null) return "empty"
        return "${pair.first} (${String.format("%.2f", pair.second)})"
    }

    private fun isPoseClassifier() {
        cameraSource?.setClassifier(PoseClassifier.create(this))
    }

    private fun createPoseEstimator() {
        // For MoveNet MultiPose, hide score and disable pose classifier as the model returns
        // multiple Person instances.
        val poseDetector = when (modelPos) {
            0 -> {
                // MoveNet Lightning (SinglePose)
                MoveNet.create(this, device, ModelType.Lightning)
            }
            1 -> {
                // MoveNet Thunder (SinglePose)
                MoveNet.create(this, device, ModelType.Thunder)
            }
            else -> {
                null
            }
        }
        poseDetector?.let { detector ->
            cameraSource?.setDetector(detector as PoseDetector)
        }
    }

    private fun requestPermission() {
        when (PackageManager.PERMISSION_GRANTED) {
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.CAMERA
            ) -> {
                // You can use the API that requires the permission.
                openCamera()
            }
            else -> {
                // You can directly ask for the permission.
                // The registered ActivityResultCallback gets the result of this request.
                requestPermissionLauncher.launch(
                    Manifest.permission.CAMERA
                )
            }
        }
    }

    /**
     * Shows an error message dialog.
     */
    class ErrorDialog : DialogFragment() {

        override fun onCreateDialog(savedInstanceState: Bundle?): Dialog =
            AlertDialog.Builder(activity)
                .setMessage(requireArguments().getString(ARG_MESSAGE))
                .setPositiveButton(android.R.string.ok) { _, _ ->
                    // do nothing
                }
                .create()

        companion object {

            @JvmStatic
            private val ARG_MESSAGE = "message"

            @JvmStatic
            fun newInstance(message: String): ErrorDialog = ErrorDialog().apply {
                arguments = Bundle().apply { putString(ARG_MESSAGE, message) }
            }
        }
    }
}
