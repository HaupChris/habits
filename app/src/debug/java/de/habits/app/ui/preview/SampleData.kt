package de.habits.app.ui.preview

import de.habits.app.data.AppState
import de.habits.core.Challenge
import de.habits.core.Direction
import de.habits.core.Habit
import de.habits.core.Measure
import de.habits.core.Pause
import de.habits.core.Plan
import de.habits.core.Schedule
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime

/** Deterministic demo data for Compose previews: a mix of all habit types with a few weeks of history. */
object SampleData {
    /** A Thursday, so the current week is half done. */
    val today: LocalDate = LocalDate.of(2026, 10, 8)

    private fun daysAgo(n: Long) = today.minusDays(n)

    /** Pseudo-random but stable "did it on day n" pattern with roughly [percent] % hits. */
    private fun hit(n: Long, percent: Int, seed: Int) = ((n * 37 + seed * 11) % 100) < percent

    private fun habit(
        id: Long, name: String, emoji: String, color: Int, start: Long, target: Int,
        schedule: Schedule = Schedule.DAILY, direction: Direction = Direction.BUILD, measure: Measure = Measure.CHECK,
        unit: String = "", challenge: Challenge? = null, pauses: List<Pause> = emptyList(),
        reminders: List<LocalTime> = emptyList(), archived: Boolean = false,
    ) = Habit(id, name, emoji, color, direction, measure, unit, listOf(Plan(daysAgo(start), target, schedule)), challenge, pauses, archived, reminders)

    val teeth = habit(1, "Zähneputzen", "🪥", 0, start = 40, target = 2, reminders = listOf(LocalTime.of(7, 30), LocalTime.of(21, 30)))
    val pushups = habit(2, "Liegestütze", "💪", 3, start = 70, target = 20, measure = Measure.COUNT, unit = "Wdh.")
    val sport = habit(3, "Sport", "🏃", 2, start = 60, target = 1, schedule = Schedule.PerWeek(3))
    val noSugar = habit(4, "Kein Zucker", "🍬", 4, start = 30, target = 0, direction = Direction.QUIT)
    val coldShower = habit(
        5, "Kalt duschen", "🚿", 6, start = 12, target = 1,
        challenge = Challenge(daysAgo(12).plusDays(29), thresholdPercent = 80),
    )
    val floss = habit(
        6, "Zahnseide", "🧵", 1, start = 50, target = 1,
        schedule = Schedule.OnDays(setOf(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY, DayOfWeek.FRIDAY)),
        pauses = listOf(Pause(daysAgo(3), null)),
    )
    val reading = habit(
        7, "Lesen", "📖", 5, start = 60, target = 15, measure = Measure.COUNT, unit = "min",
        challenge = Challenge(daysAgo(60).plusDays(20), thresholdPercent = 70),
    )

    val habits = listOf(teeth, pushups, sport, noSugar, coldShower, floss, reading)

    val entries: Map<Long, Map<LocalDate, Int>> = mapOf(
        teeth.id to history(40) { n -> if (n == 0L) 1 else if (hit(n, 85, 1)) 2 else 1 },
        pushups.id to history(70) { n -> if (n == 0L) 12 else if (hit(n, 75, 2)) 20 + (n % 3).toInt() * 5 else if (hit(n, 50, 3)) 10 else 0 },
        sport.id to history(60) { n -> if (hit(n, 45, 4)) 1 else 0 },
        noSugar.id to history(30) { n -> if (n > 0 && !hit(n, 88, 5)) 1 else 0 },
        coldShower.id to history(12) { n -> if (n == 0L) 0 else if (hit(n, 90, 6)) 1 else 0 },
        floss.id to history(50) { n -> if (hit(n, 70, 7)) 1 else 0 },
        reading.id to history(60) { n -> if (n < 40) 0 else if (hit(n, 85, 8)) 20 else 5 },
    )

    private fun history(days: Long, amount: (Long) -> Int): Map<LocalDate, Int> =
        (0..days).associate { daysAgo(it) to amount(it) }.filterValues { it > 0 }

    val state = AppState(habits, entries, today)

    val empty = AppState(emptyList(), emptyMap(), today)

    /** All habits that need an action today are done (and no quit habit slipped). */
    fun allDoneToday(): AppState = state.histories.fold(state) { acc, h ->
        val habit = h.habit
        when {
            h.periodAt(today) == null -> acc
            habit.direction == Direction.QUIT -> acc.withAmount(habit.id, today, if (habit.needsAction(today)) 1 else 0)
            else -> acc.withAmount(habit.id, today, maxOf(h.amount(today), habit.planAt(today).target))
        }
    }

    /** For interactive previews: the state after changing one entry. */
    fun AppState.withAmount(habitId: Long, date: LocalDate, amount: Int): AppState {
        val forHabit = entries[habitId].orEmpty().let { if (amount <= 0) it - date else it + (date to amount) }
        return copy(entries = entries + (habitId to forHabit))
    }
}
