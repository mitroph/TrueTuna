package com.truetuna.tuner

import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.SystemBarStyle
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.truetuna.tuner.ui.screen.TunerScreen
import com.truetuna.tuner.ui.theme.TrueTunaTheme
import com.truetuna.tuner.viewmodel.TunerViewModel

class MainActivity : ComponentActivity() {

    private val tunerViewModel: TunerViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // The app is always dark: force light (white) system-bar icons regardless of system theme.
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT)
        )
        // Keep screen on while tuning
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        setContent {
            TrueTunaTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    TunerScreen(viewModel = tunerViewModel)
                }
            }
        }
    }

    override fun onPause() {
        super.onPause()
        tunerViewModel.stopListening()
    }

    override fun onResume() {
        super.onResume()
        // Restart capture after returning from the background (onPause stops it) and
        // re-check the permission in case it was changed in system settings.
        tunerViewModel.checkPermission()
    }

}
