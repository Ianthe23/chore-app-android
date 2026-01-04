package com.choreapp.android

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Geocoder
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.lifecycle.lifecycleScope
import com.choreapp.android.databinding.ActivityLocationPickerBinding
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import kotlinx.coroutines.launch
import java.util.*

class LocationPickerActivity : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var binding: ActivityLocationPickerBinding
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var googleMap: GoogleMap

    private var selectedLat: Double? = null
    private var selectedLng: Double? = null
    private var initialLat: Double? = null
    private var initialLng: Double? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        try {
            binding = ActivityLocationPickerBinding.inflate(layoutInflater)
            setContentView(binding.root)

            // Get initial location if provided
            initialLat = intent.getDoubleExtra("latitude", 0.0).takeIf { it != 0.0 }
            initialLng = intent.getDoubleExtra("longitude", 0.0).takeIf { it != 0.0 }

            fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

            // Setup map - use try-catch to handle potential fragment issues
            try {
                val mapFragment = supportFragmentManager.findFragmentById(R.id.mapFragment) as? SupportMapFragment
                if (mapFragment != null) {
                    mapFragment.getMapAsync(this)
                } else {
                    Toast.makeText(this, "Error loading map", Toast.LENGTH_SHORT).show()
                    finish()
                    return
                }
            } catch (e: Exception) {
                Toast.makeText(this, "Map error: ${e.message}", Toast.LENGTH_SHORT).show()
                finish()
                return
            }

            // Setup buttons
            binding.btnConfirm.setOnClickListener {
                if (selectedLat != null && selectedLng != null) {
                    lifecycleScope.launch {
                        val locationName = getAddressFromLocation(selectedLat!!, selectedLng!!)
                        val resultIntent = Intent().apply {
                            putExtra("latitude", selectedLat!!)
                            putExtra("longitude", selectedLng!!)
                            putExtra("location_name", locationName)
                        }
                        setResult(Activity.RESULT_OK, resultIntent)
                        finish()
                    }
                } else {
                    Toast.makeText(this@LocationPickerActivity, "Please select a location", Toast.LENGTH_SHORT).show()
                }
            }

            binding.btnCancel.setOnClickListener {
                setResult(Activity.RESULT_CANCELED)
                finish()
            }
        } catch (e: Exception) {
            Toast.makeText(this, "Error initializing: ${e.message}", Toast.LENGTH_LONG).show()
            e.printStackTrace()
            finish()
        }
    }

    override fun onMapReady(map: GoogleMap) {
        googleMap = map
        googleMap.mapType = GoogleMap.MAP_TYPE_NORMAL
        googleMap.uiSettings.isZoomControlsEnabled = true

        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            googleMap.isMyLocationEnabled = true
        }

        // Set initial position
        if (initialLat != null && initialLng != null) {
            val initialPosition = LatLng(initialLat!!, initialLng!!)
            googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(initialPosition, 15f))
            selectedLat = initialLat
            selectedLng = initialLng
            updateLocationText(initialLat!!, initialLng!!)
        } else {
            // Get current location
            if (ActivityCompat.checkSelfPermission(
                    this,
                    Manifest.permission.ACCESS_FINE_LOCATION
                ) == PackageManager.PERMISSION_GRANTED
            ) {
                fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                    location?.let {
                        val currentPosition = LatLng(it.latitude, it.longitude)
                        googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(currentPosition, 15f))
                        selectedLat = it.latitude
                        selectedLng = it.longitude
                        updateLocationText(it.latitude, it.longitude)
                    }
                }
            }
        }

        // Update location when camera moves
        googleMap.setOnCameraIdleListener {
            val center = googleMap.cameraPosition.target
            selectedLat = center.latitude
            selectedLng = center.longitude
            updateLocationText(center.latitude, center.longitude)
        }
    }

    private fun updateLocationText(lat: Double, lng: Double) {
        lifecycleScope.launch {
            binding.tvSelectedLocation.text = "Loading address..."
            val address = getAddressFromLocation(lat, lng)
            binding.tvSelectedLocation.text = address
        }
    }

    private suspend fun getAddressFromLocation(lat: Double, lng: Double): String {
        return try {
            val geocoder = Geocoder(this, Locale.getDefault())
            val addresses = geocoder.getFromLocation(lat, lng, 1)
            if (addresses != null && addresses.isNotEmpty()) {
                addresses[0].getAddressLine(0) ?: "Lat: $lat, Lon: $lng"
            } else {
                "Lat: $lat, Lon: $lng"
            }
        } catch (e: Exception) {
            "Lat: $lat, Lon: $lng"
        }
    }
}