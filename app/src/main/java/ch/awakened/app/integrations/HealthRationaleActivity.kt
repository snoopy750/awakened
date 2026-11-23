package ch.awakened.app.integrations

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import ch.awakened.app.ui.components.SystemBackground
import ch.awakened.app.ui.components.SystemButton
import ch.awakened.app.ui.components.SystemText
import ch.awakened.app.ui.components.SystemWindow
import ch.awakened.app.ui.theme.AwakenedTheme

/** Erklärt, wofür AWAKENED Health-Connect-Daten liest (von Health Connect verlangt). */
class HealthRationaleActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            AwakenedTheme {
                SystemBackground {
                    SystemWindow(Modifier.statusBarsPadding().padding(16.dp), title = "DATENSCHUTZ") {
                        SystemText(
                            "AWAKENED liest aus Health Connect nur Schritte, Trainingseinheiten, Distanz und verbrannte Kalorien. " +
                                "Daraus werden XP und der Fortschritt deiner Ausdauer-Quests berechnet.\n\n" +
                                "Die Daten bleiben ausschliesslich auf deinem Gerät. Nichts wird hochgeladen, geteilt oder verkauft. " +
                                "Du kannst den Zugriff jederzeit in Health Connect widerrufen.",
                        )
                        Spacer(Modifier.height(16.dp))
                        SystemButton("Verstanden", { finish() }, Modifier.fillMaxWidth(), filled = true)
                    }
                }
            }
        }
    }
}
