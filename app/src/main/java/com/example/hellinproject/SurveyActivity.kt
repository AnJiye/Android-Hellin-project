package com.example.hellinproject

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import kotlinx.android.synthetic.main.activity_survey.*

class SurveyActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_survey)

        survey_next_btn.setOnClickListener {
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
        }

        setupSpinnerBirth()
        setupSpinnerGender()
        setupSpinnerHeight()
        setupSpinnerWeight()
        setupSpinnerHandler()
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
        spinner_gender.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(p0: AdapterView<*>?, p1: View?, p2: Int, p3: Long) {
                var userGender = spinner_gender.getItemAtPosition(p2)
            }
            override fun onNothingSelected(p0: AdapterView<*>?) {
            }
        }
        spinner_birth.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(p0: AdapterView<*>?, p1: View?, p2: Int, p3: Long) {
                var userBirth = spinner_birth.getItemAtPosition(p2)
            }
            override fun onNothingSelected(p0: AdapterView<*>?) {
            }
        }
        spinner_height.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(p0: AdapterView<*>?, p1: View?, p2: Int, p3: Long) {
                var userHeight = spinner_height.getItemAtPosition(p2)
            }
            override fun onNothingSelected(p0: AdapterView<*>?) {
            }
        }
        spinner_weight.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(p0: AdapterView<*>?, p1: View?, p2: Int, p3: Long) {
                var userWeight = spinner_weight.getItemAtPosition(p2)
            }
            override fun onNothingSelected(p0: AdapterView<*>?) {
            }
        }
    }
}