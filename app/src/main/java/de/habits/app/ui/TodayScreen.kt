package de.habits.app.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import de.habits.app.data.AppState
import de.habits.core.Direction
import de.habits.core.HabitHistory
import de.habits.core.Lifecycle
import de.habits.core.Measure
import de.habits.core.Outcome
import de.habits.core.levelFor
import java.time.LocalDate

@Composable
fun TodayScreen(
    state: AppState,
    onSetAmount: (habitId: Long, amount: Int) -> Unit,
    onOpen: (Long) -> Unit,
    onCreate: () -> Unit,
) {
    val today = state.today
    val due = state.histories.filter { it.habit.lifecycle(today) == Lifecycle.ACTIVE && it.periodAt(today) != null }
    val (settled, open) = due.partition(::isSettled)
    val allDone = due.isNotEmpty() && open.isEmpty()
    val perfect = allDone && state.overview.isPerfectDay(today)
    val (done, total) = state.overview.dayScore(today)
    val level = levelFor(state.overview.totals.xp)
    var amountFor by remember { mutableStateOf<HabitHistory?>(null) }
    var doneExpanded by rememberSaveable { mutableStateOf(false) }

    fun quick(h: HabitHistory) {
        if (isSingleCheck(h.habit, today)) onSetAmount(h.habit.id, checkToggleValue(h, today)) else amountFor = h
    }

    LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp), modifier = Modifier.fillMaxSize()) {
        item(key = "header") {
            Text(today.format(dayFormat), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Heute", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                if (total > 0) Text("$done / $total", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Spacer(Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Avatar(level.number, state.bodyShape, Modifier.height(56.dp), animate = false)
                Text("Lv ${level.number}", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                LinearProgress(level.progress, Modifier.weight(1f))
                Text(level.title, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Spacer(Modifier.height(12.dp))
        }

        if (state.habits.isEmpty()) {
            item(key = "empty") { EmptyState(onCreate) }
        } else if (due.isEmpty()) {
            item(key = "free") {
                Text(
                    "Heute ist nichts fällig. Genieß den freien Tag! 🌿",
                    Modifier.padding(vertical = 32.dp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        item(key = "celebration") {
            AnimatedVisibility(visible = perfect, enter = fadeIn() + expandVertically(), exit = fadeOut() + shrinkVertically()) {
                CelebrationCard(today)
            }
            if (allDone && !perfect) {
                Text(
                    "Für heute ist nichts mehr offen.",
                    Modifier.padding(vertical = 16.dp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        // Open habits glide out of the list as they get done.
        items(open, key = { it.habit.id }) { h ->
            Box(Modifier.animateItem()) {
                TodayRow(h, onOpen = { onOpen(h.habit.id) }, onQuick = { quick(h) }, onSetAmount = { onSetAmount(h.habit.id, it) })
            }
        }

        if (settled.isNotEmpty()) {
            item(key = "done") {
                DoneSection(
                    settled,
                    expanded = doneExpanded,
                    onToggle = { doneExpanded = !doneExpanded },
                    modifier = Modifier.animateItem().padding(top = 12.dp),
                ) { h ->
                    TodayRow(h, onOpen = { onOpen(h.habit.id) }, onQuick = { quick(h) }, onSetAmount = { onSetAmount(h.habit.id, it) }, muted = true)
                }
            }
        }
        item(key = "fab-space") { Spacer(Modifier.height(72.dp)) }
    }

    amountFor?.let { h ->
        AmountDialog(h, today, onDismiss = { amountFor = null }, onSave = {
            onSetAmount(h.habit.id, it)
            amountFor = null
        })
    }
}

/**
 * Nothing left to do today: BUILD (or check-in) done or week complete. Quit habits that only log slips need no
 * action, so they're settled too – they stay reachable in the done section.
 */
private fun isSettled(h: HabitHistory): Boolean {
    val p = h.periodAt(h.today) ?: return true
    if (!h.habit.needsAction(h.today)) return true
    return p.outcome == Outcome.SUCCESS || h.habit.isOk(h.amount(h.today), h.habit.planAt(h.today))
}

/** Calm, collapsible container for everything done today; collapsed it only shows the habits' icons. */
@Composable
private fun DoneSection(
    settled: List<HabitHistory>,
    expanded: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier,
    row: @Composable (HabitHistory) -> Unit,
) {
    val rotation by animateFloatAsState(if (expanded) 180f else 0f, label = "chevron")
    Column(
        modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f), RoundedCornerShape(20.dp)),
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .clickable(onClick = onToggle)
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("✓", color = Status.success, fontWeight = FontWeight.Bold)
            Text(
                "  Erledigt · ${settled.size}",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f),
            )
            if (!expanded) StackedIcons(settled)
            Icon(Icons.Filled.KeyboardArrowDown, if (expanded) "Zuklappen" else "Aufklappen", Modifier.rotate(rotation), tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        AnimatedVisibility(visible = expanded, enter = fadeIn() + expandVertically(), exit = fadeOut() + shrinkVertically()) {
            Column(Modifier.padding(start = 12.dp, end = 12.dp, bottom = 8.dp)) {
                settled.forEach { row(it) }
            }
        }
    }
}

/** Overlapping little avatars – a quiet collection of what's done. */
@Composable
private fun StackedIcons(list: List<HabitHistory>) {
    val shown = list.take(5)
    Box(Modifier.padding(end = 8.dp)) {
        shown.forEachIndexed { i, h ->
            Box(
                Modifier
                    .offset(x = (i * 16).dp)
                    .size(24.dp)
                    .background(MaterialTheme.colorScheme.surface, CircleShape)
                    .border(1.dp, habitColor(h.habit.color).copy(alpha = 0.4f), CircleShape),
                contentAlignment = Alignment.Center,
            ) { Text(h.habit.emoji, style = MaterialTheme.typography.labelSmall) }
        }
        Spacer(Modifier.size(width = (24 + (shown.size - 1) * 16).dp, height = 24.dp))
    }
}

/** Shown once everything due today is done; tap for another message. */
@Composable
private fun CelebrationCard(today: LocalDate) {
    var offset by rememberSaveable(today) { mutableIntStateOf(0) }
    Column(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
            .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f), RoundedCornerShape(24.dp))
            .clickable { offset++ }
            .padding(horizontal = 24.dp, vertical = 28.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        AnimatedContent(targetState = encouragementFor(today, offset), transitionSpec = { fadeIn() togetherWith fadeOut() }, label = "encouragement") { e ->
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(e.emoji, style = MaterialTheme.typography.displaySmall)
                Text(
                    if (e.author != null) "„${e.text}“" else e.text,
                    style = MaterialTheme.typography.titleMedium,
                    fontStyle = if (e.author != null) FontStyle.Italic else FontStyle.Normal,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                )
                e.author?.let {
                    Text("— $it", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

@Composable
private fun TodayRow(
    h: HabitHistory,
    onOpen: () -> Unit,
    onQuick: () -> Unit,
    onSetAmount: (Int) -> Unit,
    muted: Boolean = false,
) {
    val habit = h.habit
    val today = h.today
    val period = h.periodAt(today)!!
    val amount = h.amount(today)
    val plan = habit.planAt(today)
    val okToday = habit.isOk(amount, plan)
    val checkIn = habit.direction == Direction.QUIT && habit.needsAction(today)
    val color = habitColor(habit.color)

    val subtitle = buildList {
        if (period.weekly) {
            add(if (habit.direction == Direction.BUILD) "${period.done}/${period.required} diese Woche" else "${period.done}/${period.required} Ausnahmen diese Woche")
        }
        if (habit.measure == Measure.COUNT) add("$amount / ${targetText(habit, plan.target)}")
        if (habit.direction == Direction.QUIT && !period.weekly && habit.measure == Measure.CHECK) {
            add(
                when {
                    checkIn -> if (okToday) "Heute clean" else "Clean geblieben? Abhaken"
                    okToday -> "Bisher clean"
                    else -> "Ausgerutscht"
                }
            )
        }
        if (h.streak.current > 0) add("🔥 ${streakText(h.streak)}")
        if (h.streak.atRisk && !okToday) add("⚠ nicht zweimal verpassen")
    }.joinToString(" · ")

    Row(
        Modifier
            .fillMaxWidth()
            .clickable(onClick = onOpen)
            .padding(vertical = if (muted) 6.dp else 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(Modifier.weight(1f).alpha(if (muted) 0.7f else 1f), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            HabitAvatar(habit, if (muted) 32.dp else 40.dp)
            Column {
                Text(
                    habit.name,
                    style = if (muted) MaterialTheme.typography.bodyLarge else MaterialTheme.typography.titleMedium,
                    fontWeight = if (muted) FontWeight.Normal else FontWeight.SemiBold,
                )
                if (subtitle.isNotEmpty()) Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        val circle = if (muted) 32.dp else 44.dp
        when {
            habit.direction == Direction.QUIT && habit.measure == Measure.CHECK && !checkIn ->
                OutlinedButton(onClick = onQuick) { Text(if (amount > 0) "Rückgängig" else "Ausrutscher") }
            isSingleCheck(habit, today) -> CheckCircle(done = okToday, color = color, size = circle, onClick = onQuick)
            habit.measure == Measure.CHECK -> {
                // One tick box per time of day; ticking fills them up from the left, unticking removes the last.
                val labels = checkLabels(habit, plan.target)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    for (i in 0 until plan.target) {
                        val done = amount > i
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            CheckCircle(done = done, color = color, size = if (muted) 28.dp else 36.dp, onClick = { onSetAmount(if (done) amount - 1 else amount + 1) })
                            if (!muted) labels?.let { Text(it[i], style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                        }
                    }
                }
            }
            else -> {
                val fraction = if (habit.direction == Direction.BUILD) {
                    if (plan.target == 0) 1f else amount.toFloat() / plan.target
                } else {
                    if (plan.target == 0) (if (amount > 0) 1f else 0f) else amount.toFloat() / plan.target
                }
                val ringColor = if (habit.direction == Direction.QUIT && !okToday) Status.fail else color
                ProgressRing(fraction, ringColor, Modifier.clickable(onClick = onQuick), size = circle) {
                    Text(if (habit.direction == Direction.BUILD && okToday) "✓" else "$amount", style = MaterialTheme.typography.labelLarge)
                }
            }
        }
    }
}

@Composable
private fun CheckCircle(done: Boolean, color: Color, size: Dp = 44.dp, onClick: () -> Unit) {
    Box(
        Modifier
            .size(size)
            .background(if (done) color else MaterialTheme.colorScheme.surfaceVariant, CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text("✓", color = if (done) Color.White else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f), style = MaterialTheme.typography.titleMedium)
    }
}

@Composable
private fun EmptyState(onCreate: () -> Unit) {
    Column(Modifier.fillMaxWidth().padding(vertical = 48.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("🌱", style = MaterialTheme.typography.displayMedium)
        Text("Noch keine Habits", style = MaterialTheme.typography.titleMedium)
        Text("Leg los mit etwas Kleinem – du kannst Vorlagen nutzen.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        Button(onClick = onCreate) { Text("Ersten Habit anlegen") }
    }
}
