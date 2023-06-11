package com.example.locationmapsplaces

import android.app.Dialog
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.Toast
import androidx.activity.ComponentActivity
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GoogleApiAvailability

class MainActivity : ComponentActivity() {

    private final val TAG:String = "MainActivity";
    private final val ERROR_DIALOG_REQUEST:Int = 9001;


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_display_message)
        if(isServicesOK()) {
            init();

        }
    }

    private fun init() {
        val btnMap = findViewById<Button>(R.id.btnMap)
        btnMap.setOnClickListener {
            val intent = Intent(this@MainActivity, MapActivity::class.java)
            startActivity(intent)
        }
    }

    private fun isServicesOK(): Boolean {
        Log.d(TAG, "isServicesOK: checking google services version")

        val available = GoogleApiAvailability.getInstance()
            .isGooglePlayServicesAvailable(this@MainActivity)

        if (available == ConnectionResult.SUCCESS) {
            // Everything is fine and the user can make map requests
            Log.d(TAG, "isServicesOK: Google Play Services is working")
            return true
        } else if (GoogleApiAvailability.getInstance().isUserResolvableError(available)) {
            // An error occurred but we can resolve it
            Log.d(TAG, "isServicesOK: an error occurred but we can fix it")
            val dialog: Dialog? = GoogleApiAvailability.getInstance()
                .getErrorDialog(this@MainActivity, available, ERROR_DIALOG_REQUEST).also {
                    it?.show()
                }
        } else {
            Toast.makeText(this, "You can't make map requests", Toast.LENGTH_SHORT).show()
        }
        return false
    }
}
