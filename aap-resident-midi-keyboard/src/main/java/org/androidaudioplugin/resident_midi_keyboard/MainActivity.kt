package org.androidaudioplugin.resident_midi_keyboard

import android.app.PendingIntent
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.core.app.NotificationCompat
import dev.jeziellago.compose.markdowntext.MarkdownText
import org.androidaudioplugin.resident_midi_keyboard.keyboard.MidiKeyboardMain
import org.androidaudioplugin.resident_midi_keyboard.ui.theme.ComposeAudioControlsTheme
import kotlin.system.exitProcess


class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)


        val serviceIntent = Intent(this, MidiKeyboardForegroundService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(serviceIntent)
        } else {
            startService(serviceIntent)
        }

        setContent {
            ComposeAudioControlsTheme {
                // A surface container using the 'background' color from the theme
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    MidiKeyboardManagerMain()
                }
            }
            var lastBackPressed by remember { mutableStateOf(System.currentTimeMillis()) }
            BackHandler {
                if (System.currentTimeMillis() - lastBackPressed < 2000) {
                    finish()
                    exitProcess(0)
                }
                else
                    Toast.makeText(this, "Tap once more to quit", Toast.LENGTH_SHORT).show()
                lastBackPressed = System.currentTimeMillis()
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun MidiKeyboardManagerMainPreview() {
    ComposeAudioControlsTheme {
        MidiKeyboardManagerMain()
    }
}

@Composable
fun MidiKeyboardManagerMain() {
    val context = LocalContext.current
    if (!Settings.canDrawOverlays(context)) {
        Toast.makeText(context, "Overlay permission is not enabled.", Toast.LENGTH_LONG).show()
    }

    Column {
        MarkdownText(markdown = """
There are three ways to use this MIDI keyboard:
- run main activity (this screen)
- via System Alert Window: you have to give UI overlay permission)
- via SurfaceControlViewHost: apps need to connect to it)
""")
        TextButton(onClick = {
            val intent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION)
            context.startActivity(intent)
        }) {
            Text("Launch overlay permission settings")
        }

        MidiKeyboardMain()
    }
}
