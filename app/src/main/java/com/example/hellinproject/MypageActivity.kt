package com.example.hellinproject

import android.content.Intent
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.view.View
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.github.mikephil.charting.charts.BarChart
import com.github.mikephil.charting.components.Description
import com.github.mikephil.charting.components.Legend
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.components.YAxis
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import kotlinx.android.synthetic.main.activity_mypage.*
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.*
import kotlin.collections.ArrayList

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

        setChartView()

        databaseRef.child("users").child(uid!!).addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                mypage_username.text = snapshot.child("nickname").value as CharSequence?
                var photoUri = snapshot.child("userProfile").value
                Glide.with(this@MypageActivity).load(photoUri).apply(RequestOptions.circleCropTransform()).into(mypage_image)

                var count = snapshot.child("exercise").child(formatted).child("squatCount").value
                if (count == null) {
                    squat_info_btn.text = "0회"
                } else {
                    squat_info_btn.text = "${count}회"
                }
            }
            override fun onCancelled(error: DatabaseError) {
            }
        })
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun setChartView() {
        var chartWeek = chart_week
        setWeek(chartWeek)
    }

    private fun initBarDataSet(barDataSet: BarDataSet) {
        barDataSet.color = Color.parseColor("#CCE0AB")
        barDataSet.formSize = 15f
        barDataSet.setDrawIcons(false)
        barDataSet.setDrawValues(false)
        barDataSet.valueTextSize = 12f
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun setWeek(barChart: BarChart) {
        initBarChart(barChart)

        barChart.setScaleEnabled(false)

        var valueList = ArrayList<Int>()
        var weekList = week(now.toString())
        val entries: ArrayList<BarEntry> = ArrayList()
        val title = "개수"

        // input data
        for (i in 0..70 step 10) {
            valueList.add(i)
        }

//        valueList = week(now.toString())

        // fit the data into a bar
        // BarEntry(x축 좌표 이름, value)
        for (i in 1 until valueList.size) {
            val barEntry = BarEntry(i.toFloat(), valueList[i-1].toFloat())
            entries.add(barEntry)
        }

        val barDataSet = BarDataSet(entries, title)
        initBarDataSet(barDataSet)
        val data = BarData(barDataSet)
        barChart.data = data
        barChart.invalidate()
    }

    private fun initBarChart(barChart: BarChart) {
        barChart.setDrawGridBackground(false)
        barChart.setDrawBarShadow(false)
//        barChart.setDrawBorders(false)

        val description = Description()
        description.setEnabled(false)
        barChart.setDescription(description)

        barChart.animateY(1000)
        barChart.animateX(1000)

        //바텀 좌표 값
        val xAxis: XAxis = barChart.xAxis
        //change the position of x-axis to the bottom
        xAxis.position = XAxis.XAxisPosition.BOTTOM
        //set the horizontal distance of the grid line
        xAxis.granularity = 1f
        xAxis.textColor = Color.BLACK
        //hiding the x-axis line, default true if not set
        xAxis.setDrawAxisLine(false)
        //hiding the vertical grid lines, default true if not set
        xAxis.setDrawGridLines(false)


        //좌측 값 hiding the left y-axis line, default true if not set
        val leftAxis: YAxis = barChart.axisLeft
        leftAxis.setDrawAxisLine(false)
        leftAxis.textColor = Color.BLACK


        //우측 값 hiding the right y-axis line, default true if not set
        val rightAxis: YAxis = barChart.axisRight
        rightAxis.setEnabled(false)
        rightAxis.setDrawAxisLine(false)
        rightAxis.textColor = Color.BLACK


        //바차트의 타이틀
        val legend: Legend = barChart.legend
        legend.setEnabled(false)
        //setting the shape of the legend form to line, default square shape
        legend.form = Legend.LegendForm.LINE
        //setting the text size of the legend
        legend.textSize = 11f
        legend.textColor = Color.BLACK
        //setting the alignment of legend toward the chart
        legend.verticalAlignment = Legend.LegendVerticalAlignment.TOP
        legend.horizontalAlignment = Legend.LegendHorizontalAlignment.CENTER
        //setting the stacking direction of legend
        legend.orientation = Legend.LegendOrientation.HORIZONTAL
        //setting the location of legend outside the chart, default false if not set
        legend.setDrawInside(false)
    }

    fun week(eventDate: String): ArrayList<String> {
        val weekList = ArrayList<String>()
        val dateArray = eventDate.split("-").toTypedArray()

        val cal = Calendar.getInstance()
        cal[dateArray[0].toInt(), dateArray[1].toInt() - 1] = dateArray[2].toInt()

        // 일주일의 첫 날을 일요일로 지정
        cal.firstDayOfWeek = Calendar.SUNDAY

        // 시작일과 특정 날짜의 차이
        val dayOfWeek = cal[Calendar.DAY_OF_WEEK] - cal.firstDayOfWeek

        // 해당 주차의 첫째날
        cal.add(Calendar.DAY_OF_MONTH, -dayOfWeek)
        val sf = SimpleDateFormat("MM.dd")

        val startDt = sf.format(cal.time)
        weekList.add(startDt)

        for (i in 1..7) {
            cal.add(Calendar.DAY_OF_MONTH, 1)
            val dt = sf.format(cal.time)
            weekList.add(dt)
        }

        return weekList

        // 해당 주차의 마지막 날짜
//        cal.add(Calendar.DAY_OF_MONTH, 6)
//        val endDt = sf.format(cal.time)
    }
}