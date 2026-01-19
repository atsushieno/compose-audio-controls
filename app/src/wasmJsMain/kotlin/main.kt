import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.window.CanvasBasedWindow
import androidx.compose.ui.window.ComposeViewport
import dev.atsushieno.ktmidi.WebMidiAccess
import org.androidaudioplugin.composeaudiocontrols.demoapp.MainContent
import org.androidaudioplugin.composeaudiocontrols.demoapp.midiAccess

@OptIn(ExperimentalComposeUiApi::class)
fun main() {
    midiAccess = WebMidiAccess()
    ComposeViewport(content = { MainContent() })
}