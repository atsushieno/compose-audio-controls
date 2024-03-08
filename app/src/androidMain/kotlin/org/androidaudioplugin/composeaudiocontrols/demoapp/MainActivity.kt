package org.androidaudioplugin.composeaudiocontrols.demoapp

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import dev.atsushieno.ktmidi.AndroidMidi2Access
import org.androidaudioplugin.composeaudiocontrols.demoapp.MainContent
import org.androidaudioplugin.composeaudiocontrols.demoapp.theme.ComposeAudioControlsTheme
import kotlin.system.exitProcess

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        midiAccess = AndroidMidi2Access(this, true)
        setContent {
            ComposeAudioControlsTheme {
                // A surface container using the 'background' color from the theme
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    MainContent()
                }
            }
        }

        onBackPressedDispatcher.addCallback(object: OnBackPressedCallback(true) {
            private var lastBackPressed = System.currentTimeMillis()
            override fun handleOnBackPressed() {
                if (System.currentTimeMillis() - lastBackPressed < 2000) {
                    finish()
                    exitProcess(0)
                }
                else
                    Toast.makeText(this@MainActivity, "Tap once more to quit", Toast.LENGTH_SHORT).show()
                lastBackPressed = System.currentTimeMillis()
            }
        })
    }
}


