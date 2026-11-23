package ch.awakened.app.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import ch.awakened.app.domain.Exercise
import ch.awakened.app.domain.FitnessCalc
import ch.awakened.app.domain.FitnessLevel
import ch.awakened.app.domain.Goal
import ch.awakened.app.domain.QuestScaling
import ch.awakened.app.domain.Sex
import ch.awakened.app.ui.MainViewModel
import ch.awakened.app.ui.components.KeyValue
import ch.awakened.app.ui.components.SectionLabel
import ch.awakened.app.ui.components.SegmentedChoice
import ch.awakened.app.ui.components.SystemButton
import ch.awakened.app.ui.components.SystemText
import ch.awakened.app.ui.components.SystemTextField
import ch.awakened.app.ui.components.SystemWindow
import ch.awakened.app.ui.theme.Sys

fun String.toDoubleLenient(): Double? = replace(',', '.').trim().toDoubleOrNull()

@Composable
fun OnboardingScreen(vm: MainViewModel, onDone: () -> Unit) {
    var step by rememberSaveable { mutableIntStateOf(0) }
    var name by rememberSaveable { mutableStateOf("") }
    var age by rememberSaveable { mutableStateOf("") }
    var sex by rememberSaveable { mutableStateOf(Sex.MALE) }
    var height by rememberSaveable { mutableStateOf("") }
    var weight by rememberSaveable { mutableStateOf("") }
    var fitness by rememberSaveable { mutableStateOf(FitnessLevel.BEGINNER) }
    var goal by rememberSaveable { mutableStateOf(Goal.STRENGTH) }
    var busy by remember { mutableStateOf(false) }

    val ageI = age.trim().toIntOrNull()
    val heightD = height.toDoubleLenient()
    val weightD = weight.toDoubleLenient()
    val bodyValid = name.isNotBlank() && ageI in 10..100 && heightD != null && heightD in 100.0..250.0 && weightD != null && weightD in 30.0..300.0

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).imePadding().padding(20.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.height(32.dp))
        AnimatedContent(step, label = "onboarding") { st ->
            when (st) {
                0 -> SystemWindow(title = "BENACHRICHTIGUNG") {
                    SystemText(
                        "Du hast die geheimen Bedingungen erfüllt, um ein Spieler zu werden.\n\n" +
                            "Das System wird dich ab heute täglich prüfen. Erfüllst du deine Quests, wirst du stärker. " +
                            "Versagst du, erwartet dich die PENALTY ZONE.",
                        align = TextAlign.Center, modifier = Modifier.fillMaxWidth(),
                    )
                    Spacer(Modifier.height(18.dp))
                    Text(
                        "Möchtest du akzeptieren?", style = MaterialTheme.typography.titleLarge, color = Sys.Cyan,
                        textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth(),
                    )
                    Spacer(Modifier.height(16.dp))
                    SystemButton("Akzeptieren", { step = 1 }, Modifier.fillMaxWidth(), filled = true)
                }
                1 -> SystemWindow(title = "SPIELER-REGISTRIERUNG") {
                    SystemTextField(name, { name = it }, "Name")
                    Spacer(Modifier.height(10.dp))
                    SystemTextField(age, { age = it.filter(Char::isDigit).take(3) }, "Alter", numeric = true)
                    Spacer(Modifier.height(10.dp))
                    SectionLabel("Geschlecht")
                    SegmentedChoice(Sex.entries, sex, { it.label }, { sex = it })
                    Spacer(Modifier.height(10.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        SystemTextField(height, { height = it.take(5) }, "Grösse (cm)", Modifier.weight(1f), decimal = true)
                        SystemTextField(weight, { weight = it.take(5) }, "Gewicht (kg)", Modifier.weight(1f), decimal = true)
                    }
                    Spacer(Modifier.height(16.dp))
                    SystemButton("Weiter", { step = 2 }, Modifier.fillMaxWidth(), enabled = bodyValid, filled = true)
                    if (!bodyValid && (age.isNotEmpty() || height.isNotEmpty())) {
                        Spacer(Modifier.height(6.dp))
                        Text("Bitte alle Felder plausibel ausfüllen.", color = Sys.Muted, style = MaterialTheme.typography.bodySmall)
                    }
                }
                2 -> SystemWindow(title = "KLASSE WÄHLEN") {
                    SectionLabel("Fitnesslevel")
                    SegmentedChoice(FitnessLevel.entries, fitness, { it.label }, { fitness = it })
                    Spacer(Modifier.height(6.dp))
                    SystemText(
                        when (fitness) {
                            FitnessLevel.BEGINNER -> "Du trainierst selten oder fängst neu an."
                            FitnessLevel.INTERMEDIATE -> "Du trainierst ab und zu und bist recht fit."
                            FitnessLevel.ADVANCED -> "Du trainierst regelmässig und intensiv."
                        },
                        color = Sys.Muted,
                    )
                    Spacer(Modifier.height(12.dp))
                    SectionLabel("Ziel")
                    SegmentedChoice(Goal.entries, goal, { it.label }, { goal = it })
                    Spacer(Modifier.height(16.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        SystemButton("Zurück", { step = 1 }, Modifier.weight(1f))
                        SystemButton("Weiter", { step = 3 }, Modifier.weight(1f), filled = true)
                    }
                }
                else -> SystemWindow(title = "SPIELER-ANALYSE") {
                    val h = heightD ?: 175.0
                    val w = weightD ?: 70.0
                    val bmi = FitnessCalc.bmi(w, h)
                    KeyValue("Name", name)
                    KeyValue("BMI", "%.1f · %s".format(bmi, FitnessCalc.bmiCategory(bmi)))
                    KeyValue("Grundumsatz", "%.0f kcal/Tag".format(FitnessCalc.bmrMifflinStJeor(w, h, ageI ?: 25, sex)))
                    SectionLabel("Erste Daily Quest")
                    Exercise.DAILY.forEach { ex ->
                        val target = QuestScaling.dailyTarget(1, ex, fitness, goal)
                        KeyValue(ex.label, if (ex == Exercise.CARDIO) "$target min" else "$target ${ex.unit}")
                    }
                    Spacer(Modifier.height(8.dp))
                    SystemText("Die Ziele steigen langsam mit deinem Level.", color = Sys.Muted)
                    Spacer(Modifier.height(16.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        SystemButton("Zurück", { step = 2 }, Modifier.weight(1f))
                        SystemButton(
                            "Erwachen",
                            {
                                busy = true
                                vm.createProfile(name, ageI ?: 25, sex.name, h, w, fitness, goal, onDone)
                            },
                            Modifier.weight(1f), filled = true, enabled = !busy,
                        )
                    }
                }
            }
        }
        Spacer(Modifier.height(32.dp))
    }
}
