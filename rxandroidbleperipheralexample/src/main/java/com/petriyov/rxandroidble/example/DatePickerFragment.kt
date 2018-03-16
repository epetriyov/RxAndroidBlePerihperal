package com.petriyov.rxandroidble.example

import android.app.DatePickerDialog
import android.app.Dialog
import android.os.Bundle
import android.support.v4.app.DialogFragment
import android.widget.DatePicker
import org.joda.time.DateTime
import rx.Observable
import rx.subjects.PublishSubject


/**
 * Created by Eugene on 12/22/2017.
 */
class DatePickerFragment : DialogFragment(), DatePickerDialog.OnDateSetListener {

    companion object {
        private val EXTRA_DATE: String = "EXTRA_DATE"
        private val EXTRA_MAX_DATE: String = "EXTRA_MAX_DATE"
        private val EXTRA_MIN_DATE: String = "EXTRA_MIN_DATE"

        fun newInstance(dateTime: DateTime, maxDate: Long? = null, minDate: Long? = null): DatePickerFragment {
            val datePickerFragment = DatePickerFragment()
            val args = Bundle()
            args.putSerializable(EXTRA_DATE, dateTime)
            if (maxDate != null) {
                args.putLong(EXTRA_MAX_DATE, maxDate)
            }
            if (minDate != null) {
                args.putLong(EXTRA_MIN_DATE, minDate)
            }
            datePickerFragment.arguments = args
            return datePickerFragment
        }
    }

    private var selectedDate = DateTime()

    private val selectSubject = PublishSubject.create<DateTime>()

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        arguments?.getSerializable(EXTRA_DATE)?.let {
            selectedDate = it as DateTime
        }
        val datePickerDialog = DatePickerDialog(activity, this, selectedDate.year, selectedDate.monthOfYear - 1, selectedDate.dayOfMonth)
        datePickerDialog.datePicker.maxDate = arguments?.getLong(EXTRA_MAX_DATE, System.currentTimeMillis()) ?: System.currentTimeMillis()
        val minDate = arguments?.getLong(EXTRA_MIN_DATE) ?: 0L
        if (minDate != 0L) {
            datePickerDialog.datePicker.minDate = minDate
        }
        return datePickerDialog
    }

    override fun onDateSet(view: DatePicker, year: Int, month: Int, day: Int) {
        selectedDate = selectedDate.withYear(year)
        selectedDate = selectedDate.withMonthOfYear(month + 1)
        selectedDate = selectedDate.withDayOfMonth(day)
        selectSubject.onNext(selectedDate)
    }

    fun dateSelects(): Observable<DateTime> {
        return selectSubject
    }
    
    
}
