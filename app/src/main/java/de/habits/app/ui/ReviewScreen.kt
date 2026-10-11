package de.habits.app.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import de.habits.app.data.AppState
import de.habits.core.Review
import de.habits.core.ReviewKind
import de.habits.core.rangeOf
import de.habits.core.review
import de.habits.core.shift
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.IsoFields
import kotlin.math.roundToInt

private val dayMonth = DateTimeFormatter.ofPattern("d. MMM", GERMAN)
private val monthYear = DateTimeFormatter.ofPattern("MMMM yyyy", GERMAN)
private val shortMonth = DateTimeFormatter.ofPattern("MMM", GERMAN)

/** "▲ 12 Punkte ggü. Vorwoche" – arrows instead of red/green, so it reads without colour. */
fun changeText(change: Float?, kind: ReviewKind): String? {
    change ?: return null
    val points = (change * 100).roundToInt()
    val before = if (kind == ReviewKind.WEEK) "Vorwoche" else "Vormonat"
    return when {
        points > 0 -> "▲ $points Punkte ggü. $before"
        points < 0 -> "▼ ${-points} Punkte ggü. $before"
        else -> if (kind == ReviewKind.WEEK) "wie in der Vorwoche" else "wie im Vormonat"
    }
}

private fun title(kind: ReviewKind, start: LocalDate) = when (kind) {
    ReviewKind.WEEK -> "KW ${start.get(IsoFields.WEEK_OF_WEEK_BASED_YEAR)} · ${start.format(dayMonth)} – ${start.plusDays(6).format(dayMonth)}"
    ReviewKind.MONTH -> start.format(monthYear)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReviewScreen(state: AppState, initialKind: ReviewKind, onBack: () -> Unit) {
    var kind by rememberSaveable { mutableStateOf(initialKind) }
    var offset by rememberSaveable { mutableStateOf(0L) } // 0 = current period, -1 = the one before, …
    val today = state.today
    val review = state.overview.review(kind, kind.shift(today, offset))

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Rückblick") },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Zurück") } },
            )
        },
    ) { padding ->
        Column(
            Modifier.padding(padding).verticalScroll(rememberScrollState()).padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(selected = kind == ReviewKind.WEEK, onClick = { kind = ReviewKind.WEEK; offset = 0 }, label = { Text("Woche") })
                FilterChip(selected = kind == ReviewKind.MONTH, onClick = { kind = ReviewKind.MONTH; offset = 0 }, label = { Text("Monat") })
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = { offset-- }) { Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, "Vorher") }
                Text(title(kind, review.range.start), Modifier.weight(1f), textAlign = TextAlign.Center, style = MaterialTheme.typography.titleMedium)
                IconButton(onClick = { offset++ }, enabled = offset < 0) { Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, "Danach") }
            }
            ReviewContent(review)
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun ReviewContent(r: Review) {
    if (r.rate == null) {
        Text("Für diesen Zeitraum gibt es noch keine Einträge.", Modifier.padding(vertical = 24.dp), color = MaterialTheme.colorScheme.onSurfaceVariant)
        return
    }
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
        Text(percent(r.rate), style = MaterialTheme.typography.displayMedium, fontWeight = FontWeight.SemiBold)
        val note = listOfNotNull(changeText(r.change, r.kind), "läuft noch".takeIf { !r.finished }).joinToString(" · ")
        if (note.isNotEmpty()) Text(note, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
    Row(Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
        Stat("${r.done}/${r.decided}", "erfüllt")
        Stat("${r.perfectDays}", "perfekte Tage")
        Stat("+${r.xp}", "XP")
    }

    SectionTitle("Verlauf")
    RateBars(
        values = r.trend,
        labels = r.trend.indices.map { i ->
            val start = r.kind.rangeOf(r.kind.shift(r.range.start, (i - r.trend.size + 1).toLong())).start
            if (r.kind == ReviewKind.WEEK) "KW${start.get(IsoFields.WEEK_OF_WEEK_BASED_YEAR)}" else start.format(shortMonth)
        },
        color = MaterialTheme.colorScheme.primary,
        height = 96.dp,
    )

    SectionTitle("Pro Habit")
    r.habits.forEach { h ->
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.padding(vertical = 4.dp)) {
            HabitAvatar(h.history.habit, 32.dp)
            Column(Modifier.weight(1f)) {
                Text(h.history.habit.name, style = MaterialTheme.typography.bodyLarge, maxLines = 1)
                changeText(h.previousRate?.let { h.rate - it }, r.kind)?.let {
                    Text(it, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            Text(percent(h.rate), Modifier.width(48.dp), style = MaterialTheme.typography.labelLarge, textAlign = TextAlign.End)
        }
    }
}
