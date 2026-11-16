package com.example.shiftly

import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.example.shiftly.ui.navigation.MainAppScreen
import com.example.shiftly.ui.theme.ShiftlyTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT)
        )
        setContent {
            var isLightScreen by remember { mutableStateOf(true) }

            ShiftlyTheme {
                MainAppScreen(
                    onLightBackgroundChanged = { isLight ->
                        if (isLightScreen != isLight) {
                            isLightScreen = isLight
                            enableEdgeToEdge(
                                statusBarStyle = if (isLight) {
                                    SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT)
                                } else {
                                    SystemBarStyle.dark(Color.TRANSPARENT)
                                },
                                navigationBarStyle = SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT)
                            )
                        }
                    }
                )
            }
        }
    }
}
