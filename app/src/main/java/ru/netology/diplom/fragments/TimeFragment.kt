package ru.netology.diplom.fragments

import android.app.DatePickerDialog
import android.app.Dialog
import android.app.TimePickerDialog
import android.os.Bundle
import android.text.format.DateFormat
import androidx.fragment.app.DialogFragment
import java.util.*

class TimeFragment(
    private val calendar: Calendar,
    private val listener: TimePickerDialog.OnTimeSetListener
) : DialogFragment() {


    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val hour = calendar.get(Calendar.HOUR_OF_DAY)
        val min = calendar.get(Calendar.MINUTE)


        return TimePickerDialog(
            requireContext(),
            listener,
            hour,
            min,
            DateFormat.is24HourFormat(activity)
        )
    }
}

class DateFragment(
    private val calendar: Calendar,
    private val listener: DatePickerDialog.OnDateSetListener
) :
    DialogFragment() {


    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val year = calendar.get(Calendar.YEAR)
        val month = calendar.get(Calendar.MONTH)
        val day = calendar.get(Calendar.DAY_OF_MONTH)


        return DatePickerDialog(
            requireContext(),
            listener,
            year,
            month,
            day,
        )
    }
}