package com.example.assessment.application.view

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.icu.util.Calendar
import android.os.Build
import android.os.Bundle
import android.view.View
import android.viewbinding.library.fragment.viewBinding
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import com.example.assessment.R
import com.example.assessment.application.viewmodel.TimerViewModel
import com.example.assessment.databinding.FragmentTimerBinding
import com.example.assessment.utils.Constants
import com.example.assessment.utils.Constants.TOTALTIME
import com.karumi.dexter.Dexter
import com.karumi.dexter.PermissionToken
import com.karumi.dexter.listener.PermissionDeniedResponse
import com.karumi.dexter.listener.PermissionGrantedResponse
import com.karumi.dexter.listener.PermissionRequest
import com.karumi.dexter.listener.single.PermissionListener
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import java.lang.String
import java.util.Locale


@AndroidEntryPoint
class Timer : Fragment(R.layout.fragment_timer), View.OnClickListener {

    var appStateInBackground = false
    private val viewModel by viewModels<TimerViewModel>()
    private val binding by viewBinding<FragmentTimerBinding>()
    var timerState = Constants.NOTSTARTED

    override fun onResume() {
        super.onResume()
        appStateInBackground = false
    }

    override fun onPause() {
        super.onPause()
        appStateInBackground = true
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.timerStateChange.setOnClickListener(this)
        binding.timerStopper.setOnClickListener(this)
        checkNotificationPermission()
        observeData()
    }

    private fun checkNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            Dexter.withContext(requireContext())
                .withPermission(Manifest.permission.POST_NOTIFICATIONS)
                .withListener(object : PermissionListener {
                    override fun onPermissionGranted(response: PermissionGrantedResponse) {
                    }

                    override fun onPermissionDenied(response: PermissionDeniedResponse) {
                        Toast.makeText(
                            requireContext(),
                            getString(R.string.notificationError),
                            Toast.LENGTH_LONG
                        ).show()
                    }

                    override fun onPermissionRationaleShouldBeShown(
                        permission: PermissionRequest?,
                        token: PermissionToken?
                    ) {
                        token?.continuePermissionRequest()
                    }
                }).check()
        }
    }


    private fun observeData() {
        lifecycleScope.launch {
            viewModel.flow.collect {
                val calendar = Calendar.getInstance()
                when (it.status) {
                    Constants.RUNNING -> {
                        timerState = Constants.RUNNING
                        requireActivity().runOnUiThread {
//                            Log.d("CtimerData", it.timeLeft.toString())
                            calendar.timeInMillis = it.timeLeft
                            val output = String.format(
                                Locale.US,
                                "%02d:%02d:%03d",
                                0,
                                calendar.get(Calendar.SECOND),
                                calendar.get(Calendar.MILLISECOND)
                            )
                            binding.timerLabel.text = output
                            updateProgressBar(it.timeLeft)
                            updateButton(Constants.RUNNING)
                        }
                    }

                    Constants.STOPPED -> {
                        if (appStateInBackground) {
                            createNotification()
                        }
                        timerState = Constants.STOPPED
                        updateButton(Constants.STOPPED)
                        binding.timerLabel.text = "00:00:000"
                        binding.progressBar.progress = (TOTALTIME / 1000)
                    }
                }
            }
        }
    }

    private fun createNotification() {
        var notificationManager =
            requireActivity().getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        var builder: Notification.Builder

        val intent = Intent(requireContext(), Timer::class.java)

        val pendingIntent = PendingIntent.getActivity(
            requireActivity(), 0, intent, PendingIntent.FLAG_IMMUTABLE
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationChannel =
                NotificationChannel("channelId", "description", NotificationManager.IMPORTANCE_HIGH)
            notificationChannel.enableLights(true)
            notificationChannel.enableVibration(false)
            notificationManager.createNotificationChannel(notificationChannel)

            builder = Notification.Builder(requireContext(), "channelId")
                .setSmallIcon(R.mipmap.ic_launcher).setContentTitle(getString(R.string.app_name))
                .setContentText("Timer Completed").setContentIntent(pendingIntent)
                .setAutoCancel(true)
        } else {

            builder = Notification.Builder(requireContext()).setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle(getString(R.string.app_name)).setContentText("Timer Stopped")
                .setContentIntent(pendingIntent).setAutoCancel(true)
        }
        notificationManager.notify(1234, builder.build())


    }

    private fun updateProgressBar(l: Long) {
        binding.progressBar.max = (TOTALTIME / 1000)
        binding.progressBar.progress = ((TOTALTIME - l) / (1000)).toInt()

    }

    private fun updateButton(state: kotlin.String) {
        when (state) {
            Constants.RUNNING -> {
                binding.timerStateChange.text = getString(R.string.restart)
            }

            Constants.STOPPED -> {
                binding.timerStateChange.text = getString(R.string.start)
            }
        }

    }


    override fun onClick(v: View?) {
        when (v) {
            binding.timerStopper -> {
                viewModel.stopTimer()
            }

            binding.timerStateChange -> {
                viewModel.startTimer()
            }
        }

    }

    override fun onDestroy() {
        super.onDestroy()
        viewModel.stopTimer()
    }

}