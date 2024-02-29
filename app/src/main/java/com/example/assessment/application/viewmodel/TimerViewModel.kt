package com.example.assessment.application.viewmodel

import TimerData
import android.os.CountDownTimer
import android.util.Log
import androidx.lifecycle.ViewModel
import com.example.assessment.utils.Constants
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import javax.inject.Inject


@HiltViewModel
class TimerViewModel @Inject constructor() : ViewModel() {

    var lastTimerStamp: Long? = null
    private lateinit var countDownTimer: CountDownTimer

    val flow: Flow<TimerData> = callbackFlow {
        countDownTimer = object : CountDownTimer(lastTimerStamp?:Constants.TOTALTIME.toLong(), 1) {
            override fun onTick(millisUntilFinished: Long) {
                Log.d("CtimesscscsrData", (lastTimerStamp).toString())
                lastTimerStamp=millisUntilFinished
                trySend(TimerData(status = Constants.RUNNING, timeLeft = millisUntilFinished))
            }

            override fun onFinish() {
                lastTimerStamp=null
                trySend(TimerData(status = Constants.STOPPED, timeLeft = 0L))
            }
        }

        awaitClose {
            countDownTimer.cancel()
        }
    }

    fun startTimer() {
         countDownTimer.start()
    }

    fun stopTimer() {
        countDownTimer.cancel()
    }
}