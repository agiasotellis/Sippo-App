package com.sippo.app

import android.content.Intent
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.By
import androidx.test.uiautomator.BySelector
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.UiObject2
import androidx.test.uiautomator.Until
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith
import java.util.regex.Pattern

/**
 * Walks the whole app on a fresh install the way a person would. UiAutomator is used instead of the
 * Compose test rule because the water and mascots animate forever, so Compose never goes idle.
 */
@RunWith(AndroidJUnit4::class)
class SmokeTest {

    private val instr = InstrumentationRegistry.getInstrumentation()
    private val device = UiDevice.getInstance(instr)
    private val pkg = instr.targetContext.packageName

    /** CI emulators sometimes show "isn't responding" over everything; the notification prompt can also lag. */
    private fun dismissSystemDialogs() {
        repeat(3) {
            val w = device.findObject(By.text("Wait")) ?: device.findObject(By.text("Close app"))
                ?: device.findObject(By.text("Allow")) ?: return
            w.click()
            device.waitForIdle(1_000)
        }
    }

    private fun find(sel: BySelector, what: String, timeout: Long = 8_000): UiObject2 =
        device.wait(Until.findObject(sel), timeout)
            ?: run { dismissSystemDialogs(); device.wait(Until.findObject(sel), timeout) }
            ?: error("Not on screen: $what. Visible: ${visibleTexts()}")

    private fun visibleTexts(): String = runCatching {
        "[${device.currentPackageName}] " + device.findObjects(By.text(Pattern.compile(".+"))).joinToString(" | ") { it.text }
    }.getOrElse { "?" }

    private fun text(t: String, timeout: Long = 8_000) = find(By.text(t), "\"$t\"", timeout)

    private fun tap(t: String) = text(t).click()

    private fun assertStillRunning() = assertEquals("App left the foreground (crash?)", pkg, device.currentPackageName)

    @Test
    fun walkThroughEverything() {
        dismissSystemDialogs()
        val launch = instr.targetContext.packageManager.getLaunchIntentForPackage(pkg)!!
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
        instr.targetContext.startActivity(launch)
        device.wait(Until.hasObject(By.pkg(pkg).depth(0)), 10_000)

        // Get started → onboarding.
        text("Hydration is better with friends", 15_000)
        tap("Get started")
        text("Pick your hydration buddy")
        tap("Pingo") // locked: opens the paywall
        text("Unlock Sippo+")
        find(By.text("Restore purchase"), "paywall")
        device.pressBack()
        text("Pick your hydration buddy")
        tap("Continue")
        text("Let's find your daily goal")
        tap("Athlete")
        tap("oz")
        tap("ml")
        tap("Set goal")
        tap("Start drinking")

        // Android 13+ asks for notifications here.
        device.wait(Until.findObject(By.text("Allow")), 10_000)?.click()

        // Home with the Sippo+ promo.
        text("Today")
        device.wait(Until.findObject(By.text("Maybe later")), 6_000)?.click()
        text("Hold to pour")

        // Add a drink from the sheet, then undo it.
        tap("Add drink")
        text("Add a drink")
        tap("Tea")
        tap("350 ml")
        tap("Add 350 ml")
        text("Added 350 ml tea")
        tap("Undo")
        assertStillRunning()

        // Hold to pour for ~1.5 s.
        val pour = text("Hold to pour").visibleCenter
        device.swipe(pour.x, pour.y, pour.x, pour.y, 300)
        find(By.textStartsWith("Added "), "pour snackbar")

        // Edit the poured entry (it pours the last drink picked in the sheet: tea).
        find(By.text("Tap to edit"), "drinks list")
        tap("Tea")
        text("Edit drink")
        tap("Save changes")
        assertStillRunning()

        // Every tab.
        tap("Stats"); text("This week")
        tap("History"); text("Free plan shows the last 7 days")
        tap("Awards"); text("WEEKLY CHALLENGE")
        tap("Reminders"); text("Remind me to drink")

        // Themes are Sippo+; debug builds unlock locally when Play has no product.
        tap("Themes")
        tap("Unlock Sippo+")
        text("Remind me to drink")
        tap("Themes")
        text("Theme")
        tap("Midnight")
        tap("Done")
        assertStillRunning()

        // Wardrobe with premium characters and accessories.
        tap("Today")
        tap("Wardrobe")
        text("CHARACTER")
        tap("Axel")
        tap("Crown")
        device.pressBack()
        text("Hold to pour")

        // History is unlimited now.
        tap("History")
        assertNotNull(device.wait(Until.findObject(By.text("Export history as CSV")), 8_000))
        assertStillRunning()
    }
}
