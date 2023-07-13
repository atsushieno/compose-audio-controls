package org.androidaudioplugin.resident_midi_keyboard

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.view.View
import android.view.WindowManager
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.consumeAllChanges
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.NotificationChannelCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import org.androidaudioplugin.resident_midi_keyboard.keyboard.MidiKeyboardMain
import kotlin.math.roundToInt
import kotlin.system.exitProcess

class MidiKeyboardForegroundService : LifecycleService(), SavedStateRegistryOwner {
    @Composable
    fun OverlayDraggableContainer(modifier: Modifier = Modifier, content: @Composable BoxScope.() -> Unit) {
        var overlayOffset by remember { mutableStateOf(Offset.Zero) }
        Box(modifier = modifier.pointerInput(Unit) {
            detectDragGestures { change, delta ->
                change.consume()
                overlayOffset += delta
                wmLayoutParams.apply {
                    x = overlayOffset.x.roundToInt()
                    y = overlayOffset.y.roundToInt()
                }
                windowManager.updateViewLayout(compose, wmLayoutParams)
            }
        }, content = content)
    }

    private val savedStateRegistryController: SavedStateRegistryController by lazy {
        SavedStateRegistryController.create(this)
    }
    override val savedStateRegistry: SavedStateRegistry by lazy {
        savedStateRegistryController.savedStateRegistry
    }

    private lateinit var view: View
    private val compose: ComposeView by lazy {
        ComposeView(this).apply {
            setContent {
                Column(Modifier.background(Color.White)) {
                    OverlayDraggableContainer {
                        Row {
                            TextButton(onClick = {
                                windowManager.removeView(view)
                            }) {
                                Text("[X]")
                            }
                            Text(
                                "ResidentMIDIKeyboard",
                                fontSize = 20.sp,
                                modifier = Modifier.border(3.dp, Color.Black)
                                    .background(Color.White)
                                    .width(IntrinsicSize.Max)
                            )
                        }
                    }
                    MidiKeyboardMain()
                }
            }
        }
    }

    override fun onCreate() {
        super.onCreate()

        //view = MidiKeyboardSystemAlertView(this)
        //val compose = ComposeView(this).apply { setContent { MidiKeyboardManagerMain() } }
        view = compose
        view.setViewTreeLifecycleOwner(this)
        view.setViewTreeSavedStateRegistryOwner(this)

        savedStateRegistryController.performRestore(null)

        //view.addView(compose)
    }

    val INTENT_COMMAND_KEY = "Command"
    val ACTION_ALERT_WINDOW_SHOW = "SHOW"
    val ACTION_ALERT_WINDOW_HIDE = "HIDE"
    val ACTION_STOP = "STOP"

    private val wmLayoutParams = WindowManager.LayoutParams(
        WindowManager.LayoutParams.WRAP_CONTENT,
        WindowManager.LayoutParams.WRAP_CONTENT,
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY else WindowManager.LayoutParams.TYPE_PHONE,
        WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH or WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
        PixelFormat.TRANSLUCENT)
    private val windowManager by lazy {
        getSystemService(Context.WINDOW_SERVICE) as WindowManager
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)

        when (intent?.getStringExtra(INTENT_COMMAND_KEY)) {
            ACTION_ALERT_WINDOW_SHOW -> {
                windowManager.addView(view, wmLayoutParams)
            }
            ACTION_ALERT_WINDOW_HIDE -> {
                windowManager.removeView(view)
            }
            ACTION_STOP -> {
                stopSelf()
                exitProcess(0)
            }
            else -> {
                processStartForeground()
            }
        }
        return START_NOT_STICKY
    }

    private fun processStartForeground() {
        val notificationChannelId = javaClass.name

        val channel = NotificationChannelCompat.Builder(notificationChannelId, NotificationManagerCompat.IMPORTANCE_DEFAULT).apply {
            setName("ResidentMIDIKeyboard")
        }.build()
        val manager = NotificationManagerCompat.from(this)
        manager.createNotificationChannel(channel)

        val builder = NotificationCompat.Builder(this, notificationChannelId).apply {
            setContentTitle("ResidentMIDIKeyboard")
            setContentText("Resident MIDI keyboard")
            setSmallIcon(R.mipmap.ic_launcher)
            setCategory(NotificationCompat.CATEGORY_SERVICE)
            setVisibility(NotificationCompat.VISIBILITY_PUBLIC)

            val svc = this@MidiKeyboardForegroundService
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val showAction = Intent(svc, MidiKeyboardForegroundService::class.java)
                    .putExtra(INTENT_COMMAND_KEY, ACTION_ALERT_WINDOW_SHOW)
                addAction(NotificationCompat.Action.Builder(null, "Show",
                    PendingIntent.getForegroundService(svc, 1, showAction, 0)).build())
                val hideAction = Intent(svc, MidiKeyboardForegroundService::class.java)
                    .putExtra(INTENT_COMMAND_KEY, ACTION_ALERT_WINDOW_HIDE)
                addAction(NotificationCompat.Action.Builder(null, "Hide",
                    PendingIntent.getForegroundService(svc, 2, hideAction, 0)).build())
                addAction(NotificationCompat.Action.Builder(null, "Switch to App",
                    PendingIntent.getActivity(svc, 0, Intent(svc, MainActivity::class.java),
                        PendingIntent.FLAG_IMMUTABLE)).build())
                val stopAction = Intent(svc, MidiKeyboardForegroundService::class.java)
                    .putExtra(INTENT_COMMAND_KEY, ACTION_STOP)
                addAction(NotificationCompat.Action.Builder(null, "Stop",
                    PendingIntent.getForegroundService(svc, 3, stopAction, 0)).build())
            }
        }
        val notification = builder.build()

        startForeground(1, notification)
    }
}