package com.curiouswizard.locationreminder.locationreminders.savereminder

import android.Manifest
import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.Intent
import android.content.IntentSender
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.RequiresApi
import androidx.core.app.ActivityCompat
import androidx.databinding.DataBindingUtil
import com.curiouswizard.locationreminder.BuildConfig
import com.curiouswizard.locationreminder.R
import com.curiouswizard.locationreminder.base.BaseFragment
import com.curiouswizard.locationreminder.base.NavigationCommand
import com.curiouswizard.locationreminder.databinding.FragmentSaveReminderBinding
import com.curiouswizard.locationreminder.locationreminders.geofence.GeofenceBroadcastReceiver
import com.curiouswizard.locationreminder.locationreminders.reminderslist.ReminderDataItem
import com.curiouswizard.locationreminder.utils.setDisplayHomeAsUpEnabled
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.location.*
import com.google.android.material.snackbar.Snackbar
import org.koin.android.ext.android.inject
import java.util.concurrent.TimeUnit

class SaveReminderFragment : BaseFragment() {
    companion object {
        private const val TAG = "SaveReminderFragment"
        private const val REQUEST_FOREGROUND_AND_BACKGROUND_PERMISSION_RESULT_CODE = 33
        private const val REQUEST_FOREGROUND_ONLY_PERMISSIONS_REQUEST_CODE = 34
        private const val LOCATION_PERMISSION_INDEX = 0
        private const val BACKGROUND_LOCATION_PERMISSION_INDEX = 1
        private const val REQUEST_TURN_DEVICE_LOCATION_ON = 29
    }

    //Get the view model this time as a single to be shared with the another fragment
    override val _viewModel: SaveReminderViewModel by inject()
    private lateinit var binding: FragmentSaveReminderBinding
    private lateinit var geofencingClient: GeofencingClient
    private val runningQOrLater =
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding =
            DataBindingUtil.inflate(inflater, R.layout.fragment_save_reminder, container, false)

        setDisplayHomeAsUpEnabled(true)

        binding.viewModel = _viewModel

        geofencingClient = LocationServices.getGeofencingClient(requireActivity().applicationContext)

        return binding.root
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.lifecycleOwner = this
        binding.selectLocation.setOnClickListener {
            //            Navigate to another fragment to get the user location
            _viewModel.navigationCommand.value =
                NavigationCommand.To(SaveReminderFragmentDirections.actionSaveReminderFragmentToSelectLocationFragment())
        }

        requestForegroundAndBackgroundLocationPermissions()
        checkDeviceLocationSettings()

        binding.saveReminder.setOnClickListener {
            val title = _viewModel.reminderTitle.value
            val description = _viewModel.reminderDescription.value
            val location = _viewModel.reminderSelectedLocationStr.value
            val latitude = _viewModel.latitude.value
            val longitude = _viewModel.longitude.value

            val reminder = ReminderDataItem(title, description, location, latitude, longitude)
            if (foregroundAndBackgroundPermissionApproved()){
                if(_viewModel.validateEnteredData(reminder)) {
                    createGeofence(reminder)
                    _viewModel.saveReminder(reminder)
                }
            } else {
                requestForegroundAndBackgroundLocationPermissions()
            }
        }
    }

    @SuppressLint("MissingPermission")
    fun createGeofence(
        reminder: ReminderDataItem,
        radius: Float = 100f,
        timeout: Long = TimeUnit.HOURS.toMillis(1)
    ) {
        val geofence = Geofence.Builder()
            .setRequestId(reminder.id)
            .setCircularRegion(
                reminder.latitude!!,
                reminder.longitude!!,
                radius
            )
            .setTransitionTypes(Geofence.GEOFENCE_TRANSITION_ENTER)
            .setExpirationDuration(timeout)
            .build()
        val geofencingRequest = GeofencingRequest.Builder()
            .setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_ENTER)
            .addGeofence(geofence)
            .build()


        val intent = Intent(
            requireActivity(),
            GeofenceBroadcastReceiver::class.java
        )
        val geofencePendingIntent = PendingIntent.getBroadcast(
            requireContext(),
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT
        )

        geofencingClient.removeGeofences(geofencePendingIntent)
            .addOnCompleteListener {
                geofencingClient.addGeofences(geofencingRequest, geofencePendingIntent)?.run {
                    addOnSuccessListener {
                        _viewModel.showToast.value = "Added Geofence"
                        Log.e("Added Geofence", geofence.requestId)
                    }
                    addOnFailureListener {
                        _viewModel.showToast.value = "Fail adding Geofence:${it.message}"
                        if ((it.message != null)) {
                            Log.w(TAG, it.message!!)
                        }
                    }
                }
            }

    }

    override fun onDestroy() {
        super.onDestroy()
        //make sure to clear the view model after destroy, as it's a single view model.
        _viewModel.onClear()
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    private fun requestForegroundAndBackgroundLocationPermissions() {
        if (foregroundAndBackgroundPermissionApproved())
            return
        var permissionsArray = arrayOf(Manifest.permission.ACCESS_FINE_LOCATION)
        val resultCode = when {
            runningQOrLater -> {
                permissionsArray += Manifest.permission.ACCESS_BACKGROUND_LOCATION
                REQUEST_FOREGROUND_AND_BACKGROUND_PERMISSION_RESULT_CODE
            }
            else -> REQUEST_FOREGROUND_ONLY_PERMISSIONS_REQUEST_CODE
        }

        ActivityCompat.requestPermissions(
            requireActivity(),
            permissionsArray,
            resultCode
        )
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    private fun foregroundAndBackgroundPermissionApproved(): Boolean {
        val foregroundLocationApproved = ActivityCompat.checkSelfPermission(
            requireContext(),
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        val backgroundPermissionApproved = if (runningQOrLater) {
            ActivityCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_BACKGROUND_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
        return foregroundLocationApproved && backgroundPermissionApproved
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        if (
            grantResults.isEmpty() ||
            grantResults[LOCATION_PERMISSION_INDEX] == PackageManager.PERMISSION_DENIED ||
            (requestCode == REQUEST_FOREGROUND_AND_BACKGROUND_PERMISSION_RESULT_CODE &&
                    grantResults[BACKGROUND_LOCATION_PERMISSION_INDEX] ==
                    PackageManager.PERMISSION_DENIED)
        ) {
            // Permissions were not granted
            Snackbar.make(
                binding.root,
                R.string.permission_denied_explanation,
                Snackbar.LENGTH_INDEFINITE
            )
                .setAction(R.string.settings) {
                    startActivity(Intent().apply {
                        action = Settings.ACTION_APPLICATION_DETAILS_SETTINGS
                        data = Uri.fromParts("package", BuildConfig.APPLICATION_ID, null)
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    })
                }.show()
        } else {
            // Permissions have been granted
            checkDeviceLocationSettings()
        }
    }

    private fun checkDeviceLocationSettings(resolve: Boolean = true) {
        val locationRequest = LocationRequest.create().apply {
            priority = LocationRequest.PRIORITY_LOW_POWER
        }
        val locationSettingsRequest = LocationSettingsRequest.Builder()
            .addLocationRequest(locationRequest)
            .build()

        val settingsClient = LocationServices.getSettingsClient(requireContext())
        val locationSettingsResponseTask = settingsClient
            .checkLocationSettings(locationSettingsRequest)

        locationSettingsResponseTask.addOnFailureListener { exception ->
            if (exception is ResolvableApiException && resolve) {
                try {
                    exception.startResolutionForResult(
                        requireActivity(), REQUEST_TURN_DEVICE_LOCATION_ON
                    )
                } catch (sendEx: IntentSender.SendIntentException) {
                    Log.d(TAG, "Error getting location settings resolution: " + sendEx.message)
                }
            } else {
                Snackbar.make(
                    binding.root,
                    R.string.location_required_error,
                    Snackbar.LENGTH_INDEFINITE
                ).setAction(android.R.string.ok) {
                    checkDeviceLocationSettings()
                }.show()
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_TURN_DEVICE_LOCATION_ON) {
            checkDeviceLocationSettings(false)
        }
    }
}
