package de.habits.app.ui.preview

import android.content.res.Configuration
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Slider
import androidx.compose.runtime.mutableIntStateOf
import de.habits.core.avatarPartAt
import de.habits.core.avatarPhase
import kotlin.math.roundToInt
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.dp
import de.habits.app.ui.Avatar
import de.habits.core.BodyShape
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
import de.habits.app.ui.ReviewScreen
import de.habits.app.ui.SettingsScreen
import de.habits.core.ReviewKind
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

@PhonePreviews
@Composable
fun ProgressPreview() = PreviewSurface { ProgressScreen(SampleData.state) }

@PhonePreviews
@Composable
fun WeekReviewPreview() = PreviewSurface { ReviewScreen(SampleData.state, ReviewKind.WEEK, onBack = {}) }

@Preview(name = "Monatsrückblick", showBackground = true, widthDp = 400, heightDp = 860)
@Composable
fun MonthReviewPreview() = PreviewSurface { ReviewScreen(SampleData.state, ReviewKind.MONTH, onBack = {}) }

@Preview(name = "Einstellungen", showBackground = true, widthDp = 400, heightDp = 600)
@Composable
fun SettingsPreview() = PreviewSurface {
    var shape by remember { mutableStateOf(BodyShape.NEUTRAL) }
    SettingsScreen(shape, level = 20, onBodyShape = { shape = it }, onBack = {})
}

@Preview(name = "Avatar – alle Stufen (neutral)", showBackground = true, widthDp = 400, heightDp = 820)
@Composable
fun AvatarStagesPreview() = PreviewSurface { AvatarGrid(BodyShape.NEUTRAL, (1..45).toList() + listOf(50, 55, 60)) }

@Preview(name = "Avatar – Körperformen je Phase", showBackground = true, widthDp = 400, heightDp = 560)
@Composable
fun AvatarShapesPreview() = PreviewSurface {
    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        listOf(14, 29, 45).forEach { lv ->
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                BodyShape.entries.forEach { Avatar(lv, it, Modifier.weight(1f), animate = false) }
            }
        }
    }
}

/** Playground for Interactive Mode: pick level and body shape, tap the figure to see its animation. */
@Preview(name = "Avatar – Spielwiese (Interactive Mode)", showBackground = true, widthDp = 400, heightDp = 640)
@Composable
fun AvatarPlaygroundPreview() = PreviewSurface {
    var level by remember { mutableIntStateOf(30) }
    var shape by remember { mutableStateOf(BodyShape.NEUTRAL) }
    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Level $level – ${avatarPhase(level).title(shape)}" + (avatarPartAt(level)?.let { ": ${it.label}" } ?: ""), style = MaterialTheme.typography.titleMedium)
        Avatar(level, shape, Modifier.height(380.dp).align(Alignment.CenterHorizontally))
        Slider(value = level.toFloat(), onValueChange = { level = it.roundToInt() }, valueRange = 1f..55f, steps = 53)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            BodyShape.entries.forEach { FilterChip(selected = shape == it, onClick = { shape = it }, label = { Text(it.label) }) }
        }
    }
}

@Composable
private fun AvatarGrid(shape: BodyShape, levels: List<Int>) {
    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        levels.chunked(8).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                row.forEach { lv ->
                    Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                        Avatar(lv, shape, animate = false)
                        Text("$lv", style = MaterialTheme.typography.labelSmall)
                    }
                }
            }
        }
    }
}
