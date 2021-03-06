package com.example.farmerscollective

import android.graphics.Color
import androidx.lifecycle.ViewModelProvider
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.ListView
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.constraintlayout.widget.Guideline
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.components.AxisBase
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.formatter.IAxisValueFormatter
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import com.github.mikephil.charting.formatter.ValueFormatter
import com.github.mikephil.charting.interfaces.datasets.ILineDataSet
import com.github.mikephil.charting.listener.ChartTouchListener
import com.github.mikephil.charting.listener.OnChartGestureListener
import java.text.DecimalFormat
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import java.time.temporal.TemporalUnit
import java.util.stream.Stream

class CropPredictedFragment : Fragment() {


    private lateinit var viewModel: CropPredictedViewModel
    private lateinit var chart: LineChart
    private lateinit var recomm: LinearLayout


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.crop_predicted_fragment, container, false)
        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)


        viewModel = ViewModelProvider(this).get(CropPredictedViewModel::class.java)
        chart = view.findViewById(R.id.predict_chart)
        recomm = view.findViewById(R.id.recomm)

        val data = ArrayList<ILineDataSet>()

        chart.setDrawGridBackground(false)
        chart.description.isEnabled = false
        chart.setDrawBorders(false)

        chart.axisRight.isEnabled = false
        chart.axisLeft.setDrawAxisLine(false)
        chart.axisLeft.setDrawGridLines(false)
        chart.xAxis.setDrawAxisLine(true)
        chart.xAxis.setDrawGridLines(false)
        chart.xAxis.position = XAxis.XAxisPosition.BOTTOM
        chart.xAxis.granularity = 1f
        chart.isDragEnabled = true
        chart.setScaleEnabled(true)
        chart.isDoubleTapToZoomEnabled = false
        chart.legend.isEnabled = false

        // if disabled, scaling can be done on x- and y-axis separately
        chart.setPinchZoom(false)

        viewModel.data.observe(viewLifecycleOwner, {

//            recomm.adapter = CustomAdapter(ArrayList(it.reversed().subList(1, 4)), context!!)

            var max = 0.0f
            it.forEach { pred ->
                val l = pred.loss * (1 - pred.confidence)
                val g = pred.gain * pred.confidence

                max = maxOf(max, l, g)
            }

            val list = it.reversed().subList(1, 4)

            for(item in list) {
                val row = LayoutInflater.from(context).inflate(R.layout.recomm_item, recomm, false)

                val l = item.loss * (1 - item.confidence) / max
                val g = item.gain * item.confidence / max


                val prediction: ConstraintLayout = row.findViewById(R.id.prediction)
                val date: TextView = row.findViewById(R.id.date)

                date.text = item.date

                val left: Guideline = prediction.findViewById(R.id.left)
                val right: Guideline = prediction.findViewById(R.id.right)

                val param1 = left.layoutParams as ConstraintLayout.LayoutParams
                param1.guidePercent = (1f - l) / 2
                left.layoutParams = param1

                val param2 = right.layoutParams as ConstraintLayout.LayoutParams
                param2.guidePercent = (1f + g) / 2
                right.layoutParams = param2

                val loss = prediction.findViewById<TextView>(R.id.loss_val)
                val gain = prediction.findViewById<TextView>(R.id.gain_val)

                loss.text = (Math.round(-item.loss * 10.0) / 10.0).toString()
                gain.text = (Math.round(item.gain * 10.0) / 10.0).toString()

                recomm.addView(row)

            }



        })

        viewModel.graph.observe(viewLifecycleOwner, {
            chart.clear()
            data.clear()

            val dates = ArrayList<String>()

            dates.addAll(it.keys)

            dates.sortWith { date1, date2 ->
                val d1 = LocalDate.parse(date1)
                val d2 = LocalDate.parse(date2)

                d1.compareTo(d2)
            }

            val pred_dates = dates.subList(dates.size - 30, dates.size)
            val real_dates = dates.subList(0, dates.size - 30)

            Log.d("k", dates.toString())

            val values1 = ArrayList<Entry>()
            val values2 = ArrayList<Entry>()

            for(date in real_dates) {
                val i = dates.indexOf(date)
                values1.add(Entry(i.toFloat(), it[date]!!))
            }

            for(date in pred_dates) {
                val i = dates.indexOf(date)
                values2.add(Entry(i.toFloat(), it[date]!!))
            }

            val dataset1 = LineDataSet(values1, "Adilabad")
            val dataset2 = LineDataSet(values2, "Predicted")

            dataset1.setDrawCircles(false)
            dataset1.color = Color.parseColor("#FF0000")

            data.add(dataset1)

            dataset2.setDrawCircles(false)
            dataset2.color = Color.parseColor("#0000FF")

            data.add(dataset2)

            chart.xAxis.valueFormatter = IndexAxisValueFormatter(ArrayList(dates.map { date->
                //2022-07-25
                date.substring(8) + date.substring(4, 8) + date.substring(2, 4)
            }))
            // enable scaling and dragging

            chart.data = LineData(data)
            chart.setVisibleXRangeMaximum(30.0f)
            chart.moveViewToX((dates.size - 45).toFloat())


            chart.invalidate()
        })

    }

}