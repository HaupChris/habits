package de.habits.app.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import de.habits.app.data.AppState
import de.habits.core.AchievementProgress
import de.habits.core.ReviewKind
import de.habits.core.achievementProgress
import de.habits.core.avatarPhase
import de.habits.core.levelFor
import de.habits.core.nextAvatarPart
import de.habits.core.nextGoals
import de.habits.core.review

/** Avatar and level, the entry points to the reviews, and the next achievements – details live in the reviews. */
@Composable
fun ProgressScreen(state: AppState, onOpenReview: (ReviewKind) -> Unit = {}, onOpenSettings: () -> Unit = {}) {
    var showAllAchievements by rememberSaveable { mutableStateOf(false) }
    val totals = state.overview.totals
    val level = levelFor(totals.xp)

    Column(
        Modifier.verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Fortschritt", Modifier.weight(1f), style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.SemiBold)
            IconButton(onClick = onOpenSettings) { Icon(Icons.Filled.Settings, "Einstellungen") }
        }

        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Avatar(level.number, state.bodyShape, Modifier.height(160.dp))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(avatarPhase(level.number).title(state.bodyShape), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Text("Level ${level.number} · ${level.title}", color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(4.dp))
                LinearProgress(level.progress)
                Text(
                    "Noch ${level.xpForNext - level.xpInLevel} XP" + (nextAvatarPart(level.number)?.takeIf { it.unlockLevel == level.number + 1 }?.let { " bis: ${it.label}" } ?: ""),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        HorizontalDivider()
        SectionTitle("Rückblick")
        ReviewKind.entries.forEach { kind ->
            val r = state.overview.review(kind, state.today)
            Row(
                Modifier.fillMaxWidth().clickable { onOpenReview(kind) }.padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f)) {
                    Text(if (kind == ReviewKind.WEEK) "Diese Woche" else "Dieser Monat", style = MaterialTheme.typography.bodyLarge)
                    changeText(r.change, kind)?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                }
                Text(percent(r.rate), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
                Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }

        HorizontalDivider()
        val all = totals.achievementProgress()
        Row(verticalAlignment = Alignment.CenterVertically) {
            SectionTitle("Erfolge · ${all.count { it.unlocked }}/${all.size}", Modifier.weight(1f))
            TextButton(onClick = { showAllAchievements = !showAllAchievements }) { Text(if (showAllAchievements) "Weniger" else "Alle") }
        }
        val shown = if (showAllAchievements) all.sortedByDescending { it.unlocked } else totals.nextGoals(3)
        shown.forEach { AchievementRow(it) }
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun AchievementRow(p: AchievementProgress) {
    val a = p.achievement
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.padding(vertical = 4.dp)) {
        Text(if (p.unlocked) a.emoji else "🔒", style = MaterialTheme.typography.titleLarge)
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(a.title, style = MaterialTheme.typography.bodyLarge, fontWeight = if (p.unlocked) FontWeight.SemiBold else FontWeight.Normal)
            Text(a.description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            if (!p.unlocked) {
                LinearProgress(p.progress)
                Text("${p.value} / ${a.target}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}
