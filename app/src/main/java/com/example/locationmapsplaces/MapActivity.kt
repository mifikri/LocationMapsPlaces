package com.example.locationmapsplaces

import android.Manifest
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Address
import android.location.Geocoder
import android.location.Location
import android.location.LocationManager
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.view.KeyEvent
import android.view.inputmethod.EditorInfo
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.gms.tasks.OnCompleteListener
import com.google.android.gms.tasks.Task
import java.io.IOException
import java.util.*
import kotlin.collections.ArrayList

class MapActivity : AppCompatActivity(), OnMapReadyCallback {

    companion object {
        private const val TAG = "MapActivity"
        private const val FINE_LOCATION = Manifest.permission.ACCESS_FINE_LOCATION
        private const val COURSE_LOCATION = Manifest.permission.ACCESS_COARSE_LOCATION
        private const val LOCATION_PERMISSION_REQUEST_CODE = 1234
        private const val DEFAULT_ZOOM = 15f
        private const val MYLOCATION = "MyLocation"
    }

    private lateinit var mSearchText: EditText
    private var mLocationPermissionsGranted = false
    private lateinit var mMap: GoogleMap
    private lateinit var mFusedLocationProviderClient: FusedLocationProviderClient
    private lateinit var mLocationManager:LocationManager
    private var mLastLatitude:Double = 0.0
    private var mLastLongitude:Double = 0.0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_map)
        mSearchText = findViewById(R.id.input_search)
        mLocationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager

        getLocationPermission()
    }

    override fun onMapReady(googleMap: GoogleMap) {
        Toast.makeText(this, "Map is Ready", Toast.LENGTH_SHORT).show()
        Log.d(TAG, "onMapReady: map is ready")
        mMap = googleMap

        if (!mLocationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            checkGPSEnable()
        }

        if (mLocationPermissionsGranted && mLocationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            getDeviceLocation()

            if (ActivityCompat.checkSelfPermission(
                    this,
                    Manifest.permission.ACCESS_FINE_LOCATION
                ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                    this,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                return
            }
            mMap.isMyLocationEnabled = true
            mMap.uiSettings.isMyLocationButtonEnabled = true

            init()
        }
        else {
            Toast.makeText(this, "Your GPS isn't enabled yet", Toast.LENGTH_SHORT).show()
            Log.d(TAG, "onMapReady: Your GPS isn't enabled yet")
        }

    }

    private fun checkGPSEnable() {
        val dialogBuilder = AlertDialog.Builder(this)
        dialogBuilder.setMessage("Your GPS seems to be disabled, do you want to enable it?")
            .setCancelable(false)
            .setPositiveButton("Yes", DialogInterface.OnClickListener { dialog, id
                ->
                startActivity(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS))
            })
            .setNegativeButton("No", DialogInterface.OnClickListener { dialog, id ->
                dialog.cancel()
            })
        val alert = dialogBuilder.create()
        alert.show()
    }

    private fun init() {
        Log.d(TAG, "init: initializing")

        mSearchText.setOnEditorActionListener { textView, actionId, keyEvent ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH
                || actionId == EditorInfo.IME_ACTION_DONE
                || keyEvent.action == KeyEvent.ACTION_DOWN
                || keyEvent.keyCode == KeyEvent.KEYCODE_ENTER
            ) {

                //execute our method for searching
                geoLocateByName()
                geoLocateByDrag()
            }

            false
        }
    }

    private fun geoLocateByName() {
        Log.d(TAG, "geoLocateByName: geolocating")

        val searchString = mSearchText.text.toString()

        val geocoder = Geocoder(this)
        var locations: List<Address> = ArrayList<Address>()
        try {
            locations = geocoder.getFromLocationName(searchString, 1) as List<Address>
        } catch (e: IOException) {
            Log.e(TAG, "geoLocateByName: IOException: ${e.message}")
        }

        if (locations.isNotEmpty()) {
            val address = locations[0]

            Log.d(TAG, "geoLocateByName: found a location: $address")
//            Toast.makeText(this, address.toString(), Toast.LENGTH_SHORT).show()

            moveCamera(LatLng(address.latitude, address.longitude), DEFAULT_ZOOM, address.getAddressLine(0))
        }
    }

    private fun geoLocateByDrag() {
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(LatLng(mLastLatitude, mLastLongitude), DEFAULT_ZOOM))
        mMap.setOnCameraIdleListener {
            val lat = mMap.cameraPosition.target.latitude
            val lng = mMap.cameraPosition.target.longitude
            val addressTV = findViewById<TextView>(R.id.tv)

            // Initializing Geocoder
            val geocoder = Geocoder(this)
            var addressString= ""

            // Reverse-Geocoding starts
            try {
                val addressList: List<Address> = geocoder.getFromLocation(lat, lng, 1) as List<Address>

                // use your lat, long value here
                if (addressList != null && addressList.isNotEmpty()) {
                    val address = addressList[0]
                    val sb = StringBuilder()
                    for (i in 0 until address.maxAddressLineIndex) {
                        sb.append(address.getAddressLine(i)).append("\n")
                    }

                    // Various Parameters of an Address are appended
                    // to generate a complete Address
                    if (address.premises != null)
                        sb.append(address.premises).append(", ")

                    sb.append(address.subAdminArea).append("\n")
                    sb.append(address.locality).append(", ")
                    sb.append(address.adminArea).append(", ")
                    sb.append(address.countryName).append(", ")
                    sb.append(address.postalCode)

                    addressString = sb.toString()
                }
            } catch (e: IOException) {
                Toast.makeText(applicationContext,"Unable connect to Geocoder",Toast.LENGTH_LONG).show()
            }

            addressTV.text = "Lat: $lat \nLng: $lng \nAddress: $addressString"
        }
    }

    private fun getDeviceLocation() {
        Log.d(TAG, "getDeviceLocation: getting the device's current location")

        mFusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this)

        try {
            if (mLocationPermissionsGranted) {

                val location = mFusedLocationProviderClient.lastLocation
                location.addOnCompleteListener() {  task: Task<Location> ->
                    if (task.isSuccessful) {
                        Log.d(TAG, "onComplete: found location!")
                        val currentLocation = task.result

                        moveCamera(
                            LatLng(
                                currentLocation.latitude,
                                currentLocation.longitude
                            ),
                            DEFAULT_ZOOM,
                            MYLOCATION
                        )
                    } else {
                        Log.d(TAG, "onComplete: current location is null")
                        Toast.makeText(
                            this@MapActivity,
                            "unable to get current location",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            }
        } catch (e: SecurityException) {
            Log.e(TAG, "getDeviceLocation: SecurityException: ${e.message}")
        }
    }

    private fun moveCamera(latLng: LatLng, zoom: Float, title:String) {
        Log.d(
            TAG,
            "moveCamera: moving the camera to: lat: ${latLng.latitude}, lng: ${latLng.longitude}"
        )
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, zoom))
        mLastLatitude = latLng.latitude
        mLastLongitude = latLng.longitude

        if (title != MYLOCATION) {
            val options = MarkerOptions().position(latLng).title(title)
            mMap.addMarker(options)
        }
    }

    private fun initMap() {
        Log.d(TAG, "initMap: initializing map")
        val mapFragment =
            supportFragmentManager.findFragmentById(R.id.map) as SupportMapFragment?

        mapFragment?.getMapAsync(this)
    }

    private fun getLocationPermission() {
        Log.d(TAG, "getLocationPermission: getting location permissions")
        val permissions = arrayOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )

        if (ContextCompat.checkSelfPermission(
                this.applicationContext,
                FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            if (ContextCompat.checkSelfPermission(
                    this.applicationContext,
                    COURSE_LOCATION
                ) == PackageManager.PERMISSION_GRANTED
            ) {
                mLocationPermissionsGranted = true
                initMap()
            } else {
                ActivityCompat.requestPermissions(
                    this,
                    permissions,
                    LOCATION_PERMISSION_REQUEST_CODE
                )
            }
        } else {
            ActivityCompat.requestPermissions(
                this,
                permissions,
                LOCATION_PERMISSION_REQUEST_CODE
            )
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        Log.d(TAG, "onRequestPermissionsResult: called.")
        mLocationPermissionsGranted = false

        when (requestCode) {
            LOCATION_PERMISSION_REQUEST_CODE -> {
                if (grantResults.isNotEmpty()) {
                    for (i in grantResults.indices) {
                        if (grantResults[i] != PackageManager.PERMISSION_GRANTED) {
                            mLocationPermissionsGranted = false
                            Log.d(TAG, "onRequestPermissionsResult: permission failed")
                            return
                        }
                    }
                    Log.d(TAG, "onRequestPermissionsResult: permission granted")
                    mLocationPermissionsGranted = true
                    //initialize our map
                    initMap()
                }
            }
        }
    }

}
