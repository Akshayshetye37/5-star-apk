package com.example

import android.os.Bundle
import android.Manifest
import android.content.pm.PackageManager
import androidx.core.app.ActivityCompat
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import com.example.ui.HotelApp
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.viewmodel.HotelViewModel
import com.example.util.AppPreferences

class MainActivity : ComponentActivity() {

    private val viewModel: HotelViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        if (android.os.Build.VERSION.SDK_INT >= 33 && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.POST_NOTIFICATIONS), 4101)
        }
        setContent {
            MyApplicationTheme(
                darkTheme = AppPreferences.darkMode(this@MainActivity),
                compactUi = AppPreferences.compactUi(this@MainActivity)
            ) {
                HotelApp(viewModel = viewModel)
            }
        }
    }
}
