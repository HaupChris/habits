package de.habits.app.ui.preview

import android.content.res.Configuration
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import de.habits.app.data.AppState
import de.habits.app.ui.HabitDetailScreen
import de.habits.app.ui.HabitEditor
import de.habits.app.ui.HabitsScreen
import de.habits.app.ui.HabitsTheme
import de.habits.app.ui.ProgressScreen
import de.habits.app.ui.TodayScreen
import de.habits.app.ui.preview.SampleData.withAmount

/*
 * Compose previews with sample data (SampleData.kt). Open this file in Android Studio and use the "Split"
 * or "Design" view; "Interactive Mode" (▶ on a preview) lets you tap through the Today/Detail screens.
 * Debug source set only – nothing here ends up in release builds.
 */

@Preview(name = "Hell", showBackground = true, widthDp = 400, heightDp = 860)
@Preview(name = "Dunkel", showBackground = true, widthDp = 400, heightDp = 860, uiMode = Configuration.UI_MODE_NIGHT_YES)
annotation class PhonePreviews

@Composable
private fun PreviewSurface(content: @Composable () -> Unit) {
    HabitsTheme {
        Surface(color = MaterialTheme.colorScheme.background) {
            Box(Modifier.fillMaxSize()) { content() }
        }
    }
}

/** Keeps the sample state in memory so ticks in interactive mode show up. */
@Composable
private fun rememberSampleState() = remember { mutableStateOf(SampleData.state) }

@PhonePreviews
@Composable
fun TodayPreview() = PreviewSurface {
    var state by rememberSampleState()
    TodayScreen(
        state = state,
        onSetAmount = { id, amount -> state = state.withAmount(id, state.today, amount) },
        onOpen = {},
        onCreate = {},
    )
}

@Preview(name = "Heute – leer", showBackground = true, widthDp = 400, heightDp = 600)
@Composable
fun TodayEmptyPreview() = PreviewSurface {
    TodayScreen(state = SampleData.empty, onSetAmount = { _, _ -> }, onOpen = {}, onCreate = {})
}

@PhonePreviews
@Composable
fun HabitsPreview() = PreviewSurface { HabitsScreen(SampleData.state, onOpen = {}) }

@Composable
private fun DetailPreview(habitId: Long) = PreviewSurface {
    var state: AppState by rememberSampleState()
    HabitDetailScreen(
        history = state.history(habitId)!!,
        onBack = {},
        onEdit = {},
        onSetAmount = { date, amount -> state = state.withAmount(habitId, date, amount) },
        onPauseToggle = {},
        onArchiveToggle = {},
        onDelete = {},
    )
}

@Preview(name = "Detail – Zähneputzen (2× am Tag)", showBackground = true, widthDp = 400, heightDp = 1500)
@Composable
fun DetailTeethPreview() = DetailPreview(SampleData.teeth.id)

@Preview(name = "Detail – Challenge", showBackground = true, widthDp = 400, heightDp = 1500)
@Composable
fun DetailChallengePreview() = DetailPreview(SampleData.coldShower.id)

@Preview(name = "Detail – x-mal pro Woche", showBackground = true, widthDp = 400, heightDp = 1500)
@Composable
fun DetailWeeklyPreview() = DetailPreview(SampleData.sport.id)

@Preview(name = "Detail – Abgewöhnen", showBackground = true, widthDp = 400, heightDp = 1500)
@Composable
fun DetailQuitPreview() = DetailPreview(SampleData.noSugar.id)

@Preview(name = "Editor – neu", showBackground = true, widthDp = 400, heightDp = 1400)
@Composable
fun EditorNewPreview() = PreviewSurface { HabitEditor(initial = null, today = SampleData.today, onSave = {}, onCancel = {}) }

@Preview(name = "Editor – Challenge bearbeiten", showBackground = true, widthDp = 400, heightDp = 1400)
@Composable
fun EditorEditPreview() = PreviewSurface { HabitEditor(initial = SampleData.coldShower, today = SampleData.today, onSave = {}, onCancel = {}) }

@Preview(name = "Fortschritt", showBackground = true, widthDp = 400, heightDp = 1800)
@Preview(name = "Fortschritt – Dunkel", showBackground = true, widthDp = 400, heightDp = 1800, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
fun ProgressPreview() = PreviewSurface { ProgressScreen(SampleData.state) }
