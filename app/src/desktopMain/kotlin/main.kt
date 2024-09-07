import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.runtime.Composable
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import dev.atsushieno.ktmidi.JvmMidiAccess
import dev.atsushieno.ktmidi.LibreMidiAccess
import dev.atsushieno.ktmidi.MergedMidiAccess
import dev.atsushieno.ktmidi.MidiTransportProtocol
import org.androidaudioplugin.composeaudiocontrols.demoapp.MainContent
import org.androidaudioplugin.composeaudiocontrols.demoapp.midiAccess

fun main(args: Array<String>) {
    midiAccess = when {
        System.getProperty("os.name").startsWith("Windows", true) -> JvmMidiAccess()
        else -> LibreMidiAccess.create(MidiTransportProtocol.UMP)
    }
    application {
        Window(
            onCloseRequest = ::exitApplication,
            state = rememberWindowState(),
            title = "ComposeAudioControls Demo"
        ) {
            MainContent()
        }
    }
}


@Preview
@Composable
fun AppDesktopPreview() {
    MainContent()
}