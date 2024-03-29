package com.example.farmerscollective.realtime

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.core.content.FileProvider
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.example.farmerscollective.R
import com.example.farmerscollective.databinding.FragmentIntPriceBinding
import com.example.farmerscollective.utils.Utils
import com.example.farmerscollective.utils.Utils.Companion.adjustAxis
import com.example.farmerscollective.utils.Utils.Companion.internationalPricesCrops
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import com.github.mikephil.charting.interfaces.datasets.ILineDataSet
import com.google.firebase.analytics.FirebaseAnalytics
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.time.LocalDate

// Int Price Fragment
class IntPriceFragment : Fragment() {

    companion object {
        fun newInstance() = IntPriceFragment()
    }

    // Late initialized variables
    private val viewModel by viewModels<IntPriceViewModel>()
    private lateinit var binding: FragmentIntPriceBinding
    private lateinit var analytics: FirebaseAnalytics

    @SuppressLint("SetTextI18n")
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Use viewbinding to bind the Int Price Fragment layout(fragment_int_price.xml) to Int Price Fragment.
        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_int_price, container, false)
        // Firebase Analytics
        analytics = FirebaseAnalytics.getInstance(requireContext())

        // Shared Preferences
        val sharedPref =
            requireContext().getSharedPreferences(
                "prefs",
                Context.MODE_PRIVATE
            )

        // Setting functions for UI components using viewbinding
        with(binding) {

            // Setting onClick listener to navigate to ZoomedInFragment
            lineZoom.setOnClickListener {
                val action =
                    IntPriceFragmentDirections.actionInternationalPricesFragmentToZoomedInFragment(2)
                findNavController().navigate(action)
            }

            // Setting up adapter for dropdown menu to select crop
            val cropAdapter = ArrayAdapter(
                requireContext(),
                android.R.layout.simple_spinner_item,
                resources.getStringArray(R.array.internationalCropName)
            )
            cropAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            cropSpinner.adapter = cropAdapter
            cropSpinner.setSelection(viewModel.crop.value!!)

            // Setting up onItemSelected listener to change selection and update values
            cropSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(p0: AdapterView<*>?, p1: View?, p2: Int, p3: Long) {
                    viewModel.changeCropId(p2)
                }

                override fun onNothingSelected(p0: AdapterView<*>?) {

                }
            }

            // Creating list of years to show data for
            val current = if (LocalDate.now().isBefore(LocalDate.of(LocalDate.now().year, 6, 30)))
                LocalDate.now().year - 1
            else LocalDate.now().year

            val initial = 2022

            val arr = (initial..current).map {
                "${it}-${(it + 1) % 100}"
            }

            // Setting up adapter for dropdown menu to select year
            val yearAdapter =
                ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, arr)
            yearAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            yearSpinner.adapter = yearAdapter
            yearSpinner.setSelection(yearAdapter.getPosition("${current}-${(current + 1) % 100}"))

            // Setting up onItemSelected listener to change selection and update values
            yearSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(p0: AdapterView<*>?, p1: View?, p2: Int, p3: Long) {
                    viewModel.changeYear(arr.toList()[p2].substring(0, 4).toInt())
                }

                override fun onNothingSelected(p0: AdapterView<*>?) {
                    //do nothing
                }
            }

            // Observing international prices from viewModel to add changes when prices update
            viewModel.prices.observe(viewLifecycleOwner) {
                lineChart.clear()
                Log.e("Tag", it.toString())

                // Initial variables
                val list = it
                val axis = ArrayList<String>()
                val entries: ArrayList<ILineDataSet> = ArrayList()
                val values = ArrayList<Entry>()

                // Max Price and Min Price for graph
                var maxPrice: Float = 0f
                var minPrice: Float = 35000f

                // Add axis labels to axis list and obtain maximum and minimum price
                if (list != null) {
                    var pos = 0
                    for (i in list) {
                        axis.add(i.date.substring(5, 10))
                        values.add(Entry(pos.toFloat(), i.price))
                        if (i.price > maxPrice - 100f) {
                            maxPrice = i.price + 200f
                        }
                        if (i.price < minPrice + 100f) {
                            minPrice = i.price - 200f
                        }
                        pos += 1
                    }
                }

                // onChartGestureListener from Utils
                lineChart.onChartGestureListener =
                    Utils.Companion.CustomChartListener(requireContext(), lineChart, axis)


                // Create dataset and set values
                val dataSet = LineDataSet(values, internationalPricesCrops[viewModel.crop.value!!])
                dataSet.color = Color.parseColor("#000000")
                dataSet.setDrawCircles(false)
                entries.add(dataSet)

                // Set custom specifications for lineChart as needed
                lineChart.data = LineData(entries)
                lineChart.description.isEnabled = false
                lineChart.axisRight.isEnabled = true
                lineChart.axisLeft.isEnabled = false
                lineChart.axisRight.axisMaximum = maxPrice
                lineChart.axisRight.axisMinimum = minPrice
                lineChart.xAxis.position = XAxis.XAxisPosition.BOTTOM
                lineChart.xAxis.granularity = 1f
                lineChart.xAxis.valueFormatter = IndexAxisValueFormatter(axis)
                lineChart.legend.isWordWrapEnabled = true
                lineChart.axisRight.setDrawGridLines(false)
                lineChart.axisLeft.setDrawGridLines(false)
                lineChart.xAxis.setDrawGridLines(false)
                // Compress settings
                if (it.isNotEmpty()) {
                    if (!sharedPref.getBoolean("compress", false)) {
                        lineChart.moveViewToX(entries[0].xMax)
                        lineChart.setVisibleXRangeMaximum(10.0f)
                    } else {
                        lineChart.moveViewToX(entries[0].xMax)
                        lineChart.setVisibleXRangeMaximum(365.0f)
                    }
                }
                // Update lineChart with new values
                lineChart.invalidate()
            }

            // Setting onClick listener to share a picture of the graph
            lineChartShare.setOnClickListener {
                val icon: Bitmap = lineChart.chartBitmap
                val share = Intent(Intent.ACTION_SEND)
                share.type = "image/png"

                // Create bitmap and push to file
                try {
                    val file = File(requireContext().cacheDir, "temp.png")
                    val fOut = FileOutputStream(file)
                    icon.compress(Bitmap.CompressFormat.PNG, 100, fOut)
                    fOut.flush()
                    fOut.close()
                    file.setReadable(true, false)
                    share.putExtra(
                        Intent.EXTRA_STREAM,
                        FileProvider.getUriForFile(
                            requireContext(),
                            requireContext().packageName + ".provider",
                            file
                        )
                    )
                    // Create intent containing image to be shared
                    share.putExtra(
                        Intent.EXTRA_TEXT,
                        cropSpinner.selectedItem.toString() + " international prices in " + yearSpinner.selectedItem.toString()
                    )

                    // Log event on Firebase Analytics
                    val bundle = Bundle()
                    bundle.putString(
                        FirebaseAnalytics.Param.ITEM_ID,
                        cropSpinner.selectedItem.toString() + " international prices in " + yearSpinner.selectedItem.toString()
                    )
                    bundle.putString(FirebaseAnalytics.Param.CONTENT_TYPE, "image")
                    analytics.logEvent(FirebaseAnalytics.Event.SHARE, bundle)

                    // Share intent
                    startActivity(share)
                } catch (e: IOException) {
                    e.printStackTrace()
                    Toast.makeText(
                        requireContext(),
                        "Error occurred, please try later",
                        Toast.LENGTH_SHORT
                    ).show()
                }

            }
        }

        // Returning binding.root to update the layout with above code
        return binding.root
    }


}