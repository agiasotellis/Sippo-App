package com.sippo.app.data

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

class ProgressTest {

    private val today = LocalDate.of(2026, 10, 18).toEpochDay() // a Sunday

    private fun day(offset: Long, ml: Int, drink: Drink = Drink.WATER) =
        Entry(id = offset * 1000 + ml, day = today + offset, minute = 600, drink = drink, ml = ml)

    @Test fun goalFormulaMatchesPrototype() {
        assertEquals(2650, calcGoal(70, 1))
        assertEquals(1300, calcGoal(40, 0))
        assertEquals(5300, calcGoal(140, 2))
    }

    @Test fun hydrationUsesDrinkFactor() {
        val totals = Progress.dailyTotals(listOf(day(0, 1000, Drink.COFFEE), day(0, 500)))
        assertEquals(1300f, totals.getValue(today), .01f)
    }

    @Test fun streakCountsBackFromTodayOrYesterday() {
        val goal = 2000
        val met = (1..4).map { day(-it.toLong(), 2000) }
        assertEquals(4, Progress.streak(Progress.dailyTotals(met), goal, today))
        assertEquals(5, Progress.streak(Progress.dailyTotals(met + day(0, 2000)), goal, today))
        assertEquals(0, Progress.streak(Progress.dailyTotals(listOf(day(-2, 2000))), goal, today))
    }

    @Test fun streakFreezeForgivesOneMissPerWeek() {
        val goal = 2000
        // Met Mon–Wed and Fri–Sat of this week, missed Thursday.
        val entries = listOf(-6L, -5L, -4L, -2L, -1L).map { day(it, 2000) }
        val totals = Progress.dailyTotals(entries)
        assertEquals(2, Progress.streak(totals, goal, today))
        assertEquals(5, Progress.streak(totals, goal, today, freeze = true))
    }

    @Test fun bestStreakFindsLongestRun() {
        val entries = listOf(-10L, -9L, -8L, -5L, -4L).map { day(it, 2500) }
        assertEquals(3, Progress.bestStreak(Progress.dailyTotals(entries), 2000))
    }

    @Test fun goalDaysThisWeekStartsMonday() {
        // today is Sunday; Monday is -6, the Sunday before is -7.
        val entries = listOf(-7L, -6L, -3L, 0L).map { day(it, 2500) }
        assertEquals(3, Progress.goalDaysThisWeek(Progress.dailyTotals(entries), 2000, today))
    }

    @Test fun formatsUnits() {
        assertEquals("8 oz", formatAmount(250, Units.OZ))
    }
}
