package com.example.myweatherapp.Forecast

import android.annotation.SuppressLint
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.TextView
import com.airbnb.lottie.LottieAnimationView
import com.example.myweatherapp.R
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class ForecastAdapter(context: Context, forecastDataList: List<ForecastItem>) :
    ArrayAdapter<ForecastItem>(context, 0, forecastDataList) {

    @SuppressLint("SetTextI18n")
    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        var listItemView = convertView
        if (listItemView == null) {
            listItemView = LayoutInflater.from(context).inflate(R.layout.item_forecast, parent, false)
        }

        val currentForecast = getItem(position)
        val dayFormat = SimpleDateFormat("E", Locale.getDefault())
        val hourMinuteFormat = SimpleDateFormat("HH:mm", Locale.getDefault())

        val calendar = Calendar.getInstance()
        calendar.timeInMillis = currentForecast?.dt?.times(1000) ?: 0

        val dayTextView = listItemView!!.findViewById<TextView>(R.id.day)
        val dayOfWeek = dayFormat.format(calendar.time)
        val hourMinute = hourMinuteFormat.format(calendar.time)

        val combinedText = "$dayOfWeek $hourMinute"
        dayTextView.text = combinedText



        val tempTextView = listItemView.findViewById<TextView>(R.id.temp)
        val iconImageView = listItemView.findViewById<LottieAnimationView>(R.id.icon)
        val conditionDay = listItemView.findViewById<TextView>(R.id.conditionDay)


        tempTextView.text = "${currentForecast?.main?.temp?.toInt()} °C"
        iconImageView.cancelAnimation()
        currentForecast?.getIconResource()?.let { iconImageView.setAnimation(it) }
        iconImageView.playAnimation()
        conditionDay.text = currentForecast?.weatherCondition
        return listItemView
    }
}
