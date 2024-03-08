import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.runtime.Composable
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import dev.atsushieno.ktmidi.JvmMidiAccess
import org.androidaudioplugin.composeaudiocontrols.demoapp.MainContent
import org.androidaudioplugin.composeaudiocontrols.demoapp.midiAccess

fun main(args: Array<String>) = application {
    midiAccess = JvmMidiAccess()
    Window(onCloseRequest = ::exitApplication,
        state = rememberWindowState(),
        title = "ComposeAudioControls Demo") {
        MainContent()
    }
}

@Preview
@Composable
fun AppDesktopPreview() {
    MainContent()
}