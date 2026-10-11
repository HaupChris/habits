package de.habits.core

import java.time.LocalDate
import java.time.YearMonth

/** Weekly (Mon–Sun) or monthly performance review over all habits. */
enum class ReviewKind { WEEK, MONTH }

fun ReviewKind.rangeOf(day: LocalDate): ClosedRange<LocalDate> = when (this) {
    ReviewKind.WEEK -> day.weekStart().let { it..it.plusDays(6) }
    ReviewKind.MONTH -> YearMonth.from(day).let { it.atDay(1)..it.atEndOfMonth() }
}

/** The same day [n] weeks or months later (negative = earlier). */
fun ReviewKind.shift(day: LocalDate, n: Long): LocalDate = when (this) {
    ReviewKind.WEEK -> day.plusWeeks(n)
    ReviewKind.MONTH -> day.plusMonths(n)
}

/** One habit within a review: its success rate in the period and in the one before. */
class HabitReview(val history: HabitHistory, val rate: Float, val previousRate: Float?)

class Review(
    val kind: ReviewKind,
    val range: ClosedRange<LocalDate>,
    /** False while the period is still running. */
    val finished: Boolean,
    /** Average success rate over the habits (each habit weighs the same), null without decided periods. */
    val rate: Float?,
    val previousRate: Float?,
    /** Successful and decided periods (days or weeks) across all habits. */
    val done: Int,
    val decided: Int,
    val perfectDays: Int,
    val xp: Int,
    /** Best first. */
    val habits: List<HabitReview>,
    /** Average rates of the last periods, oldest first, this one last. */
    val trend: List<Float?>,
) {
    /** Change against the previous period in percentage points, null if either has no data. */
    val change: Float? get() = if (rate != null && previousRate != null) rate - previousRate else null
}

fun Overview.review(kind: ReviewKind, anyDay: LocalDate, trendLength: Int = if (kind == ReviewKind.WEEK) 8 else 6): Review {
    val range = kind.rangeOf(anyDay)
    val previous = kind.rangeOf(kind.shift(range.start, -1))
    fun rateIn(r: ClosedRange<LocalDate>) = histories.mapNotNull { it.rate(r.start, r.endInclusive) }.takeIf { it.isNotEmpty() }?.average()?.toFloat()

    val decided = histories.flatMap { h -> h.periods.filter { it.end in range && it.outcome != Outcome.PENDING } }
    val lastDay = minOf(range.endInclusive, today)
    val perfectDays = generateSequence(range.start) { it.plusDays(1) }.takeWhile { !it.isAfter(lastDay) }.count(::isPerfectDay)
    val habits = histories.mapNotNull { h ->
        h.rate(range.start, range.endInclusive)?.let { HabitReview(h, it, h.rate(previous.start, previous.endInclusive)) }
    }.sortedByDescending { it.rate }

    return Review(
        kind = kind,
        range = range,
        finished = range.endInclusive.isBefore(today),
        rate = rateIn(range),
        previousRate = rateIn(previous),
        done = decided.count { it.outcome == Outcome.SUCCESS },
        decided = decided.size,
        perfectDays = perfectDays,
        xp = xpBetween(range.start, range.endInclusive),
        habits = habits,
        trend = (trendLength - 1 downTo 0).map { rateIn(kind.rangeOf(kind.shift(range.start, -it.toLong()))) },
    )
}
