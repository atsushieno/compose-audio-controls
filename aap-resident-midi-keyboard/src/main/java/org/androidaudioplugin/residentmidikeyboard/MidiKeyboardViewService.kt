package org.androidaudioplugin.residentmidikeyboard

import android.content.Intent
import android.hardware.display.DisplayManager
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.Message
import android.os.Messenger
import android.view.SurfaceControlViewHost
import androidx.annotation.RequiresApi
import androidx.core.os.bundleOf

class MidiKeyboardViewService : MidiKeyboardService() {

    // SurfaceControlViewHost support
    companion object {
        // requests
        const val MESSAGE_KEY_OPCODE = "opcode"
        const val MESSAGE_KEY_HOST_TOKEN = "hostToken"
        const val MESSAGE_KEY_DISPLAY_ID = "displayId"
        const val MESSAGE_KEY_WIDTH = "width"
        const val MESSAGE_KEY_HEIGHT = "height"

        // replies
        const val MESSAGE_KEY_SURFACE_PACKAGE = "surfacePackage"

        const val OPCODE_CONNECT = 0
        const val OPCODE_DISCONNECT = 1
    }

    private lateinit var messenger: Messenger

    override fun onBind(intent: Intent): IBinder? {
        super.onBind(intent)
        messenger = Messenger(MidiKeyboardViewControllerHandler(Looper.myLooper()!!, this))
        return messenger.binder
    }

    private class MidiKeyboardViewControllerHandler(
        looper: Looper,
        private val owner: MidiKeyboardViewService
    ) : Handler(looper) {

        @RequiresApi(Build.VERSION_CODES.R)
        override fun handleMessage(msg: Message) {
            when (msg.data.getInt(MESSAGE_KEY_OPCODE)) {
                OPCODE_CONNECT -> {
                    owner.handleCreateRequest(msg)
                }

                OPCODE_DISCONNECT -> {
                    owner.handleDisposeRequest(msg)
                }

                else -> {}
            }
        }
    }

    private var controller: Controller? = null

    private fun handleCreateRequest(msg: Message) {
        val messenger = msg.replyTo
        val hostToken = msg.data.getBinder(MESSAGE_KEY_HOST_TOKEN)!!
        val displayId = msg.data.getInt(MESSAGE_KEY_DISPLAY_ID)
        val width = msg.data.getInt(MESSAGE_KEY_WIDTH)
        val height = msg.data.getInt(MESSAGE_KEY_HEIGHT)

        controller = Controller(this)
        controller!!.initialize(messenger, hostToken, displayId, width, height)
    }

    private fun handleDisposeRequest(msg: Message) {
        controller?.close()
    }

    class Controller(private val service: MidiKeyboardViewService) : AutoCloseable {
        private lateinit var viewHost: SurfaceControlViewHost

        fun initialize(messengerToSendReply: Messenger, hostToken: IBinder, displayId: Int, width: Int, height: Int) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                val display = service.getSystemService(DisplayManager::class.java)
                    .getDisplay(displayId)

                service.mainLooper.queue.addIdleHandler {
                    viewHost = SurfaceControlViewHost(service, display, hostToken).apply {


                        val view = service.createComposeView()

                        setView(view, width, height)

                        messengerToSendReply.send(Message.obtain().apply {
                            data = bundleOf(
                                MESSAGE_KEY_SURFACE_PACKAGE to surfacePackage
                            )
                        })
                    }
                    false
                }
            }
        }

        override fun close() {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                viewHost.release()
            }
        }
    }
}