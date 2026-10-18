package com.sippo.app.data

import com.sippo.app.ui.theme.Sym
import java.time.LocalDate

/** Pure calculations over the drink log, kept free of Android types so they are unit-testable. */
object Progress {

    fun dailyTotals(entries: List<Entry>): Map<Long, Float> =
        entries.groupBy { it.day }.mapValues { (_, list) -> list.sumOf { it.hydration.toDouble() }.toFloat() }

    private fun weekOf(day: Long): Long = Math.floorDiv(day + 3, 7L) // epoch day 0 was a Thursday; weeks start Monday

    /**
     * Consecutive goal days ending today (or yesterday, while today is still in progress).
     * With [freeze], one missed day per calendar week is forgiven instead of breaking the run.
     */
    fun streak(totals: Map<Long, Float>, goal: Int, today: Long, freeze: Boolean = false): Int {
        fun met(d: Long) = (totals[d] ?: 0f) >= goal
        var d = if (met(today)) today else today - 1
        var count = 0
        val frozenWeeks = HashSet<Long>()
        while (true) {
            if (met(d)) {
                count++
            } else if (freeze && count > 0 && met(d - 1) && frozenWeeks.add(weekOf(d))) {
                // forgiven: skip this day without counting it
            } else break
            d--
        }
        return count
    }

    fun bestStreak(totals: Map<Long, Float>, goal: Int): Int {
        var best = 0
        var run = 0
        var prev = Long.MIN_VALUE
        for (d in totals.keys.sorted()) {
            if ((totals[d] ?: 0f) < goal) { run = 0; prev = d; continue }
            run = if (prev == d - 1 && run > 0) run + 1 else 1
            best = maxOf(best, run)
            prev = d
        }
        return best
    }

    /** Average hydration over the last [window] days that have any drinks (today included). */
    fun dailyAverage(totals: Map<Long, Float>, today: Long, window: Int = 30): Float {
        val vals = (0 until window).mapNotNull { totals[today - it] }.filter { it > 0f }
        return if (vals.isEmpty()) 0f else vals.average().toFloat()
    }

    fun goalDaysInMonth(totals: Map<Long, Float>, goal: Int, month: LocalDate): Int {
        val first = month.withDayOfMonth(1).toEpochDay()
        val last = month.withDayOfMonth(month.lengthOfMonth()).toEpochDay()
        return totals.count { (d, v) -> d in first..last && v >= goal }
    }

    fun goalDaysThisWeek(totals: Map<Long, Float>, goal: Int, today: Long): Int {
        val week = weekOf(today)
        return totals.count { (d, v) -> weekOf(d) == week && d <= today && v >= goal }
    }

    data class Award(val icon: String, val name: String, val desc: String, val progress: Float)

    fun awards(entries: List<Entry>, totals: Map<Long, Float>, goal: Int): List<Award> {
        val best = bestStreak(totals, goal)
        val litres = entries.sumOf { it.hydration.toDouble() } / 1000.0
        val earlyDays = entries.filter { it.minute < 8 * 60 }.map { it.day }.distinct().size
        val teas = entries.count { it.drink == Drink.TEA }
        fun p(v: Double, of: Double) = (v / of).coerceIn(0.0, 1.0).toFloat()
        return listOf(
            Award(Sym.WATER_DROP, "First sip", "Log your first drink", p(entries.size.toDouble(), 1.0)),
            Award(Sym.LOCAL_FIRE_DEPARTMENT, "3-day streak", "Goal 3 days in a row", p(best.toDouble(), 3.0)),
            Award(Sym.BOLT, "Week warrior", "7-day streak", p(best.toDouble(), 7.0)),
            Award(Sym.WAVES, "Ocean 50 L", "Drink 50 litres total", p(litres, 50.0)),
            Award(Sym.WB_TWILIGHT, "Early bird", "Drink before 8 am ×5", p(earlyDays.toDouble(), 5.0)),
            Award(Sym.EMOJI_FOOD_BEVERAGE, "Tea time", "Log 10 teas", p(teas.toDouble(), 10.0)),
        )
    }
}
