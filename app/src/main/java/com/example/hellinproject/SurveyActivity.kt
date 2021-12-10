package com.example.hellinproject

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import com.example.hellinproject.dto.UserDTO
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import kotlinx.android.synthetic.main.activity_survey.*

class SurveyActivity : AppCompatActivity() {
//    var firestore : FirebaseFirestore? = null
    var database : FirebaseDatabase? = null
    var userDTO = UserDTO()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_survey)

//        firestore = FirebaseFirestore.getInstance()
        database = FirebaseDatabase.getInstance()
//        var databaseRef : DatabaseReference = database!!.reference
//        val intent: Intent = getIntent()

//        var userDTO = intent.getSerializableExtra("USER") as UserDTO
//        Log.d("Log1", userDTO.toString())

        setupSpinnerBirth()
        setupSpinnerGender()
        setupSpinnerHeight()
        setupSpinnerWeight()
        setupSpinnerHandler()

        survey_next_btn.setOnClickListener {
//            firestore?.collection("users")?.document(userDTO.uid!!)?.set(userDTO)
//            Log.d("Log2", userDTO.toString())
//            databaseRef.child("users").child(userDTO!!.uid!!).setValue(userDTO)
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
        }
    }

    private fun setupSpinnerGender() {
        val gender = resources.getStringArray(R.array.spinner_gender)
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, gender)
        spinner_gender.adapter = adapter
    }

    private fun setupSpinnerBirth() {
        val years = arrayListOf<Int>()
        for (i in 2015 downTo 1950) {
            years.add(i)
        }
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, years)
        spinner_birth.adapter = adapter
        spinner_birth.setSelection(15)
    }

    private fun setupSpinnerHeight() {
        val height = arrayListOf<Int>()
        for (i in 100 .. 200) {
            height.add(i)
        }
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, height)
        spinner_height.adapter = adapter
        spinner_height.setSelection(50)
    }

    private fun setupSpinnerWeight() {
        val weight = arrayListOf<Int>()
        for (i in 10 .. 120) {
            weight.add(i)
        }
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, weight)
        spinner_weight.adapter = adapter
        spinner_weight.setSelection(50)
    }

    private fun setupSpinnerHandler() {
        var databaseRef : DatabaseReference = database!!.reference
        var uid = FirebaseAuth.getInstance().currentUser?.uid
        spinner_gender.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(p0: AdapterView<*>?, p1: View?, p2: Int, p3: Long) {
                userDTO.gender = spinner_gender.getItemAtPosition(p2) as String?
                databaseRef.child("users").child(uid!!).child("gender").setValue(userDTO.gender)
            }
            override fun onNothingSelected(p0: AdapterView<*>?) {
            }
        }
        spinner_birth.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(p0: AdapterView<*>?, p1: View?, p2: Int, p3: Long) {
                userDTO.birth = spinner_birth.getItemAtPosition(p2) as Int?
                databaseRef.child("users").child(uid!!).child("birth").setValue(userDTO.birth)
            }
            override fun onNothingSelected(p0: AdapterView<*>?) {
            }
        }
        spinner_height.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(p0: AdapterView<*>?, p1: View?, p2: Int, p3: Long) {
                userDTO.height = spinner_height.getItemAtPosition(p2) as Int?
                databaseRef.child("users").child(uid!!).child("height").setValue(userDTO.height)
            }
            override fun onNothingSelected(p0: AdapterView<*>?) {
            }
        }
        spinner_weight.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(p0: AdapterView<*>?, p1: View?, p2: Int, p3: Long) {
                userDTO.weight = spinner_weight.getItemAtPosition(p2) as Int?
                databaseRef.child("users").child(uid!!).child("weight").setValue(userDTO.weight)
            }
            override fun onNothingSelected(p0: AdapterView<*>?) {
            }
        }
    }
}