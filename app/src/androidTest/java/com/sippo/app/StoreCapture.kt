package com.sippo.app

import android.content.Intent
import android.util.Log
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.By
import androidx.test.uiautomator.BySelector
import androidx.test.uiautomator.Configurator
import androidx.test.uiautomator.StaleObjectException
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.UiObject2
import androidx.test.uiautomator.Until
import com.sippo.app.data.Accessory
import com.sippo.app.data.Drink
import com.sippo.app.data.Entry
import com.sippo.app.data.Mascot
import com.sippo.app.data.Settings
import com.sippo.app.data.SippoRepository
import kotlinx.coroutines.runBlocking
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.time.LocalDate
import java.util.Random

/**
 * Not part of the regular test run: the "Store assets" workflow runs these to capture Play Store
 * screenshots ([screenshots]) and the promo screen recording ([tour]) on seeded demo data.
 */
@RunWith(AndroidJUnit4::class)
class StoreCapture {

    private val instr = InstrumentationRegistry.getInstrumentation()
    private val device = UiDevice.getInstance(instr)
    private val ctx = instr.targetContext
    private val pkg = ctx.packageName
    private val repo = SippoRepository.get(ctx)
    private val out = File(ctx.getExternalFilesDir(null), "store").apply { mkdirs() }
    private val timeline = StringBuilder()
    private val t0 = System.currentTimeMillis()

    init {
        Configurator.getInstance().waitForIdleTimeout = 200
        Configurator.getInstance().actionAcknowledgmentTimeout = 500
    }

    // ---- driving -------------------------------------------------------------------------------

    private fun dismissSystemDialogs() {
        repeat(3) {
            val w = device.findObject(By.text("Wait")) ?: device.findObject(By.text("Close app"))
                ?: device.findObject(By.text("Allow")) ?: return
            try { w.click() } catch (_: StaleObjectException) { }
            device.waitForIdle(1_000)
        }
    }

    private fun find(sel: BySelector, what: String, timeout: Long = 10_000): UiObject2 =
        device.wait(Until.findObject(sel), timeout)
            ?: run { dismissSystemDialogs(); device.wait(Until.findObject(sel), timeout) }
            ?: run {
                device.takeScreenshot(File(out, "fail.png"))
                val seen = runCatching { device.findObjects(By.text(java.util.regex.Pattern.compile(".+"))).joinToString(" | ") { it.text } }.getOrDefault("?")
                error("Not on screen: $what. Visible: $seen")
            }

    private fun <T> retry(block: () -> T): T {
        repeat(4) { attempt ->
            try { return block() } catch (e: StaleObjectException) { if (attempt == 3) throw e; Thread.sleep(300) }
        }
        error("unreachable")
    }

    private fun text(t: String) = find(By.text(t), "\"$t\"")
    private fun tap(t: String) = retry { closeCelebrationIfShown(); text(t).click() }
    private fun tapDesc(d: String) = retry { closeCelebrationIfShown(); find(By.desc(d), "tab $d").click() }

    private fun closeCelebrationIfShown() {
        device.findObject(By.text("Yay!"))?.let { runCatching { it.click() }; Thread.sleep(800) }
    }
    private fun pause(ms: Long) = Thread.sleep(ms)

    /** A long emulator hold can cross the goal early; close the celebration if it shows up. */
    private fun clearCelebration() {
        device.wait(Until.findObject(By.text("Yay!")), 1_500)?.let { runCatching { it.click() }; pause(800) }
    }

    private fun shot(name: String) {
        pause(900)
        device.takeScreenshot(File(out, "$name.png"))
    }

    /** Marks the start of a feature in the tour so captions can be synced to the recording. */
    private fun mark(label: String) {
        timeline.append(System.currentTimeMillis()).append('\t').append(label).append('\n')
        File(out, "timeline.tsv").writeText("start\t$t0\n$timeline")
        Log.i("StoreCapture", "mark $label")
    }

    // ---- demo data -----------------------------------------------------------------------------

    private val goal = 2500

    private fun demoEntries(todayMl: Int): List<Entry> {
        val rnd = Random(7)
        val today = LocalDate.now().toEpochDay()
        val types = listOf(Drink.WATER, Drink.WATER, Drink.WATER, Drink.TEA, Drink.COFFEE, Drink.JUICE, Drink.WATER)
        val list = mutableListOf<Entry>()
        var id = 1L
        for (back in 24 downTo 1) {
            val day = today - back
            val target = goal * (if (back == 9 || back == 16) .62f else .92f + rnd.nextFloat() * .3f)
            val n = 5 + rnd.nextInt(3)
            var acc = 0f
            for (i in 0 until n) {
                val d = types[rnd.nextInt(types.size)]
                val ml = ((target / n / d.factor) / 10).toInt() * 10
                acc += ml * d.factor
                list += Entry(id++, day, 7 * 60 + 30 + i * (13 * 60 / n) + rnd.nextInt(40), d, ml)
            }
        }
        // Today so far.
        val todayDrinks = listOf(Drink.WATER to 350, Drink.COFFEE to 200, Drink.WATER to 400, Drink.TEA to 300, Drink.WATER to 450)
        var left = todayMl
        todayDrinks.forEachIndexed { i, (d, ml) ->
            if (left <= 0) return@forEachIndexed
            list += Entry(id++, today, listOf(7 * 60 + 40, 9 * 60 + 15, 11 * 60 + 5, 13 * 60 + 30, 15 * 60 + 10)[i], d, ml)
            left -= ml
        }
        return list
    }

    private fun seed(
        onboarded: Boolean = true,
        premium: Boolean = true,
        mascot: Mascot = Mascot.OTTIE,
        acc: Accessory = Accessory.CROWN,
        theme: String = "lavender",
        todayMl: Int = 1700,
        entries: Boolean = true,
    ) = runBlocking {
        val today = LocalDate.now().toEpochDay()
        repo.saveSettings(
            Settings(
                onboarded = onboarded, mascot = mascot, accessory = acc, weightKg = 64, activity = 1,
                goalMl = goal, premium = premium, theme = theme, celebratedDay = if (todayMl >= goal) today else -1,
                crownEarned = true,
            )
        )
        repo.saveEntries(if (entries) demoEntries(todayMl) else emptyList())
    }

    private fun launch(screenWait: String) {
        val intent = ctx.packageManager.getLaunchIntentForPackage(pkg)!!
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
        ctx.startActivity(intent)
        device.wait(Until.hasObject(By.pkg(pkg).depth(0)), 15_000)
        text(screenWait)
        dismissSystemDialogs()
    }

    // ---- Play Store screenshots -----------------------------------------------------------------

    @Test
    fun screenshots() {
        dismissSystemDialogs()

        seed(onboarded = false, premium = false, entries = false)
        launch("Hydration is better with friends")
        pause(5_000) // let all three mascots drop in and settle
        shot("01_pool")

        seed()
        launch("Hold to pour")
        pause(2_500)
        shot("02_today")

        // Mid-pour: hold the button on another thread and capture while the stream runs.
        val pour = retry { text("Hold to pour").visibleCenter }
        // Emulator holds run ~50 ms per swipe step.
        val holder = Thread { device.swipe(pour.x, pour.y, pour.x, pour.y, 20) }
        holder.start()
        pause(700)
        device.takeScreenshot(File(out, "03_pour.png"))
        holder.join()
        clearCelebration()
        device.wait(Until.findObject(By.text("Undo")), 3_000)?.let { runCatching { it.click() } }
        pause(1_500)

        tapDesc("Stats"); text("This week"); pause(1_200); shot("04_stats")

        tapDesc("History"); find(By.desc("Previous month"), "history")
        retry { find(By.text("${LocalDate.now().minusDays(2).dayOfMonth}"), "day").click() }
        pause(800); shot("05_history")

        tapDesc("Awards"); text("WEEKLY CHALLENGE"); shot("06_awards")

        tapDesc("Today"); tap("Wardrobe"); text("CHARACTER")
        tap("Pingo"); tap("Shades"); pause(1_500); shot("07_wardrobe")

        seed(mascot = Mascot.AXEL, acc = Accessory.BOW, theme = "midnight", todayMl = 2100)
        launch("Hold to pour"); pause(2_500); shot("08_midnight")

        tapDesc("Reminders"); text("Remind me to drink"); shot("09_reminders")

        seed(premium = false, acc = Accessory.CAP)
        launch("Hold to pour")
        tapDesc("Reminders"); tap("Get Sippo+"); text("Unlock Sippo+"); pause(1_500); shot("10_paywall")

        seed(theme = "sunset", mascot = Mascot.PINGO, acc = Accessory.CROWN, todayMl = 2400)
        launch("Hold to pour")
        // Cross the goal with the sheet to trigger the celebration.
        tap("Add drink"); text("Add a drink"); tap("Water"); tap("250 ml"); tap("Add 250 ml")
        find(By.text("Goal reached!"), "celebration")
        pause(1_400); shot("11_goal")
    }

    // ---- promo video tour ------------------------------------------------------------------------

    @Test
    fun tour() {
        dismissSystemDialogs()
        seed(onboarded = false, premium = false, entries = false)
        // Past days so stats and history look lived-in after onboarding.
        runBlocking { repo.saveEntries(demoEntries(0)) }
        launch("Hydration is better with friends")
        mark("pool")
        pause(5_500)

        mark("onboarding")
        tap("Get started"); text("Pick your hydration buddy"); pause(1_800)
        tap("Continue"); text("Let's find your daily goal"); pause(1_200)
        tap("Active"); pause(600); tap("Set goal"); text("Start drinking"); pause(1_800)
        tap("Start drinking")
        dismissSystemDialogs()
        find(By.text("Maybe later"), "promo"); pause(1_800); tap("Maybe later")
        text("Hold to pour"); pause(1_200)

        mark("pour")
        val pour = retry { text("Hold to pour").visibleCenter }
        device.swipe(pour.x, pour.y, pour.x, pour.y, 40)
        pause(2_500)
        clearCelebration()

        mark("add")
        tap("Add drink"); text("Add a drink"); pause(900)
        tap("Tea"); pause(500); tap("350 ml"); pause(700); tap("Add 350 ml"); pause(1_800)

        mark("undo")
        retry { device.wait(Until.findObject(By.text("Undo")), 3_000)?.click() }
        pause(1_800)

        mark("edit")
        retry { find(By.text("Water"), "row").click() }
        text("Edit drink"); pause(900); tap("Coffee"); pause(700); tap("Save changes"); pause(1_500)

        mark("stats")
        tapDesc("Stats"); text("This week"); pause(2_800)

        mark("history")
        tapDesc("History"); text("Free plan shows the last 7 days"); pause(1_000)
        retry { find(By.text("${LocalDate.now().minusDays(3).dayOfMonth}"), "day").click() }
        pause(2_200)

        mark("awards")
        tapDesc("Awards"); text("WEEKLY CHALLENGE"); pause(2_400)

        mark("reminders")
        tapDesc("Reminders"); text("Remind me to drink"); pause(1_200)
        tap("08:00 – 22:00"); text("From"); pause(1_000)
        pause(500); tap("Done"); pause(800)

        mark("paywall")
        tap("Get Sippo+"); text("Unlock Sippo+"); pause(3_000)
        tap("Unlock Sippo+"); text("Remind me to drink"); pause(1_500)

        mark("smart")
        tap("Smart reminders"); pause(800); tap("Smart reminders"); pause(800)

        mark("themes")
        tap("Themes"); text("Theme"); pause(900)
        tap("Midnight"); pause(1_400); tap("Ocean"); pause(1_200); tap("Sunset"); pause(1_200); tap("Midnight"); pause(900)
        tap("Done"); pause(800)

        mark("wardrobe")
        tapDesc("Today"); text("Hold to pour"); pause(800)
        tap("Wardrobe"); text("CHARACTER"); pause(900)
        tap("Pingo"); pause(1_000); tap("Shades"); pause(1_400)
        tap("Axel"); pause(1_000); tap("Bow"); pause(1_400)
        tap("Ottie"); pause(800); tap("Crown"); pause(1_600)
        device.pressBack(); text("Hold to pour")

        mark("drinks")
        tap("Add drink"); text("Add a drink"); pause(1_000)
        tap("Smoothie"); pause(600); tap("500 ml"); pause(600); tap("Add 500 ml"); pause(1_600)

        mark("goal")
        var tries = 0
        while (!device.hasObject(By.text("Goal reached!")) && tries++ < 8) {
            tap("Add drink"); text("Add a drink"); tap("Water"); tap("500 ml"); tap("Add 500 ml"); pause(1_800)
        }
        find(By.text("Goal reached!"), "celebration"); pause(3_500)
        tap("Yay!"); pause(1_000)

        mark("history-premium")
        tapDesc("History"); text("Goal met"); pause(1_000)
        retry { find(By.desc("Previous month") , "prev").click() }
        pause(2_000)
        mark("end")
    }
}
