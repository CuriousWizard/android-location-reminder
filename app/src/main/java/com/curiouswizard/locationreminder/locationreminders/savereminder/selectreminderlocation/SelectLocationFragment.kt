package com.curiouswizard.locationreminder.locationreminders.savereminder.selectreminderlocation


import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.Resources
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.view.*
import androidx.core.content.ContextCompat
import androidx.databinding.DataBindingUtil
import com.curiouswizard.locationreminder.BuildConfig
import com.curiouswizard.locationreminder.R
import com.curiouswizard.locationreminder.base.BaseFragment
import com.curiouswizard.locationreminder.base.NavigationCommand
import com.curiouswizard.locationreminder.databinding.FragmentSelectLocationBinding
import com.curiouswizard.locationreminder.locationreminders.savereminder.SaveReminderViewModel
import com.curiouswizard.locationreminder.utils.setDisplayHomeAsUpEnabled
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MapStyleOptions
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.material.snackbar.Snackbar
import org.koin.android.ext.android.inject
import java.util.*

class SelectLocationFragment : BaseFragment(), OnMapReadyCallback {

    // Use Koin to get the view model of the SaveReminder
    override val _viewModel: SaveReminderViewModel by inject()
    private lateinit var binding: FragmentSelectLocationBinding
    private var map: GoogleMap? = null
    private var marker: Marker? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        binding =
            DataBindingUtil.inflate(inflater, R.layout.fragment_select_location, container, false)

        binding.viewModel = _viewModel
        binding.lifecycleOwner = this

        setHasOptionsMenu(true)
        setDisplayHomeAsUpEnabled(true)

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        val mapFragment = childFragmentManager.findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)

        binding.saveButton.setOnClickListener {
            if (marker == null) {
                _viewModel.showSnackBar.postValue("Please select a location for your reminder.")
            } else {
                _viewModel.navigationCommand.postValue(NavigationCommand.Back)
            }
        }

        return binding.root
    }

    override fun onStart() {
        super.onStart()
        if (map != null){
            enableMyLocation()
        }
    }

    private fun onLocationSelected(marker: Marker) {
        // When the user confirms on the selected location,
        // send back the selected location details to the view model
        // and navigate back to the previous fragment to save the reminder and add the geofence
        _viewModel.latitude.postValue(marker.position.latitude)
        _viewModel.longitude.postValue(marker.position.longitude)
        _viewModel.reminderSelectedLocationStr.postValue(marker.title)
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.map_options, menu)
    }

    override fun onOptionsItemSelected(item: MenuItem) = when (item.itemId) {
        R.id.normal_map -> {
            map!!.mapType = GoogleMap.MAP_TYPE_NORMAL
            true
        }
        R.id.hybrid_map -> {
            map!!.mapType = GoogleMap.MAP_TYPE_HYBRID
            true
        }
        R.id.satellite_map -> {
            map!!.mapType = GoogleMap.MAP_TYPE_SATELLITE
            true
        }
        R.id.terrain_map -> {
            map!!.mapType = GoogleMap.MAP_TYPE_TERRAIN
            true
        }
        else -> super.onOptionsItemSelected(item)
    }

    override fun onMapReady(p0: GoogleMap) {
        map = p0

        // Default location
        val lat = 37.4221696
        val lng = -122.0840171
        val zoom = 15f

        val defaultLatLng = LatLng(lat, lng)
        map!!.moveCamera(CameraUpdateFactory.newLatLngZoom(defaultLatLng,zoom))

        setMapStyle(map!!)
        setPoiClick(map!!)
        setMapLongClick(map!!)
        enableMyLocation()
    }

    private fun isForegroundLocationGranted(): Boolean =
        ContextCompat.checkSelfPermission(
            requireContext(),
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

    private fun requestForegroundLocation() {
        requestPermissions(
            arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
            REQUEST_FOREGROUND_ONLY_PERMISSIONS_REQUEST_CODE
        )
    }

    @SuppressLint("MissingPermission")
    private fun enableMyLocation() {
        if (isForegroundLocationGranted()) {
            map?.isMyLocationEnabled = true
        }
        else {
            requestForegroundLocation()
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray) {

        if (isForegroundLocationGranted()){
            enableMyLocation()
        } else {
            if (shouldShowRequestPermissionRationale(Manifest.permission.ACCESS_FINE_LOCATION)){
                Snackbar.make(requireView(), "Access to your location is required.", Snackbar.LENGTH_INDEFINITE)
                    .setAction(R.string.enable) {
                        requestForegroundLocation()
                    }
                    .show()
            } else {
                Snackbar.make(requireView(), "Locations permissions were denied.", Snackbar.LENGTH_INDEFINITE)
                    .setAction(R.string.settings) {
                        startActivity(Intent().apply {
                            action = Settings.ACTION_APPLICATION_DETAILS_SETTINGS
                            data = Uri.fromParts("package", BuildConfig.APPLICATION_ID, null)
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK
                        })
                    }
                    .show()
            }
        }
    }

    private fun setPoiClick(map: GoogleMap) {
        map.setOnPoiClickListener { poi ->
            marker?.remove()
            marker = map.addMarker(
                MarkerOptions()
                    .position(poi.latLng)
                    .title(poi.name)
            )
            _viewModel.selectedPOI.postValue(poi)
            onLocationSelected(marker!!)
            marker!!.showInfoWindow()
        }
    }

    private fun setMapLongClick(map: GoogleMap){
        map.setOnMapLongClickListener { location ->
            marker?.remove()
            val snippet = String.format(
                Locale.getDefault(),
                "Lat: %1$.5f, Long: %2$.5f",
                location.latitude,
                location.longitude
            )

            marker = map.addMarker(
                MarkerOptions()
                    .position(location)
                    .title(getString(R.string.dropped_pin))
                    .snippet(snippet)
            )
            onLocationSelected(marker!!)
            marker!!.showInfoWindow()
        }
    }

    private fun setMapStyle(map: GoogleMap) {
        try {
            // Customize the styling of the base map using a JSON object defined
            // in a raw resource file.
            map.setMapStyle(
                MapStyleOptions.loadRawResourceStyle(
                    requireContext(),
                    R.raw.map_style
                )
            )
        } catch (e: Resources.NotFoundException) {
            // Can't find style.
        }
    }

    companion object {
        private const val REQUEST_FOREGROUND_ONLY_PERMISSIONS_REQUEST_CODE = 34
    }
}
