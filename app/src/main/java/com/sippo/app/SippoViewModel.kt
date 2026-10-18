package com.sippo.app

import android.app.Activity
import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.sippo.app.billing.BillingManager
import com.sippo.app.data.Accessory
import com.sippo.app.data.Drink
import com.sippo.app.data.Entry
import com.sippo.app.data.Mascot
import com.sippo.app.data.Progress
import com.sippo.app.data.ReminderSound
import com.sippo.app.data.Settings
import com.sippo.app.data.SippoRepository
import com.sippo.app.data.Stored
import com.sippo.app.data.Units
import com.sippo.app.data.calcGoal
import com.sippo.app.data.formatAmount
import com.sippo.app.reminders.Reminders
import com.sippo.app.ui.mascot.Mood
import com.sippo.app.widget.SippoWidget
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalTime
import java.time.YearMonth

enum class Screen { WELCOME, ONB, HOME, STATS, HIST, AWARDS, SETTINGS, WARD, PAYWALL }

data class EditState(val id: Long, val drink: Drink, val ml: Int, val time: String)

data class UiState(
    val loaded: Boolean = false,
    val settings: Settings = Settings(),
    val entries: List<Entry> = emptyList(),
    val today: Long = LocalDate.now().toEpochDay(),
    val screen: Screen = Screen.WELCOME,
    val nav: Int = 0,
    val step: Int = 0,
    val pwFrom: Screen = Screen.HOME,
    val sheet: Boolean = false,
    val edit: EditState? = null,
    val themeSheet: Boolean = false,
    val hoursSheet: Boolean = false,
    val soundSheet: Boolean = false,
    val snack: String? = null,
    val snackUndo: Boolean = false,
    val snackId: Int = 0,
    val promo: Boolean = false,
    val celebrate: Boolean = false,
    val mood: Mood = Mood.IDLE,
    val splash: Int = 0,
    val pouring: Boolean = false,
    val selDay: Long? = null,
    val dayN: Int = 0,
    val histMonth: YearMonth = YearMonth.now(),
    val wardMood: Mood = Mood.IDLE,
    val welcomeRun: Int = 0,
) {
    val todayEntries: List<Entry> get() = entries.filter { it.day == today }
    val todayTotal: Float get() = todayEntries.sumOf { it.hydration.toDouble() }.toFloat()
    fun fmt(ml: Number) = formatAmount(ml, settings.units)
}

class SippoViewModel(app: Application) : AndroidViewModel(app) {

    private val repo = SippoRepository.get(app)
    val billing: BillingManager = (app as SippoApplication).billing

    private val _ui = MutableStateFlow(UiState())
    val ui: StateFlow<UiState> = _ui.asStateFlow()

    val price: StateFlow<String?> = billing.price

    private var lastAddedId: Long? = null
    private var snackJob: Job? = null
    private var moodJob: Job? = null
    private var wardJob: Job? = null
    private var promoJob: Job? = null

    init {
        viewModelScope.launch {
            val stored = repo.data.first()
            _ui.update {
                it.copy(
                    loaded = true,
                    settings = stored.settings,
                    entries = stored.entries,
                    screen = if (stored.settings.onboarded) Screen.HOME else Screen.WELCOME,
                )
            }
            billing.owned.value?.let { applyOwnership(it) }
        }
        viewModelScope.launch {
            billing.owned.collect { owned -> if (owned != null && _ui.value.loaded) applyOwnership(owned) }
        }
        viewModelScope.launch {
            billing.events.collect { e ->
                when (e) {
                    BillingManager.Event.Purchased -> unlocked()
                    BillingManager.Event.Pending -> showSnack("Purchase pending — Sippo+ unlocks once it clears")
                    is BillingManager.Event.Failed -> showSnack(e.message)
                }
            }
        }
    }

    private val s get() = _ui.value.settings

    // ---- persistence -------------------------------------------------------------------------

    private fun setSettings(transform: (Settings) -> Settings) {
        val before = s
        _ui.update { it.copy(settings = transform(it.settings)) }
        val after = s
        viewModelScope.launch { repo.saveSettings(after); afterSave() }
        if (before.remind != after.remind || before.intervalMin != after.intervalMin || before.onboarded != after.onboarded) {
            Reminders.schedule(getApplication(), after)
        }
    }

    private fun setEntries(entries: List<Entry>) {
        _ui.update { it.copy(entries = entries) }
        viewModelScope.launch { repo.saveEntries(entries); afterSave() }
    }

    private fun afterSave() {
        val u = _ui.value
        SippoWidget.refresh(getApplication(), Stored(u.settings, u.entries))
    }

    /** Re-reads the store (the widget may have logged drinks) and rolls the day over. */
    fun onResume() {
        viewModelScope.launch {
            val stored = repo.data.first()
            val today = LocalDate.now().toEpochDay()
            _ui.update { u ->
                val dayChanged = u.today != today
                u.copy(
                    entries = stored.entries, today = today,
                    selDay = if (dayChanged) null else u.selDay,
                    histMonth = if (dayChanged) YearMonth.now() else u.histMonth,
                )
            }
            billing.refresh()
        }
    }

    // ---- navigation --------------------------------------------------------------------------

    fun go(screen: Screen) = _ui.update { it.copy(screen = screen, nav = it.nav + 1, sheet = false) }

    fun getStarted() = _ui.update { it.copy(screen = Screen.ONB, step = 0, nav = it.nav + 1) }

    fun onbNext() {
        val u = _ui.value
        if (u.step < 2) {
            _ui.update { it.copy(step = it.step + 1, nav = it.nav + 1) }
            return
        }
        setSettings { it.copy(onboarded = true) }
        go(Screen.HOME)
        if (!s.premium) {
            promoJob?.cancel()
            promoJob = viewModelScope.launch { delay(1400); _ui.update { it.copy(promo = true) } }
        }
    }

    fun onbBack() = _ui.update { it.copy(step = (it.step - 1).coerceAtLeast(0), nav = it.nav + 1) }

    fun replayOnboarding() = _ui.update { it.copy(screen = Screen.WELCOME, step = 0, nav = it.nav + 1, welcomeRun = it.welcomeRun + 1) }

    /** System back. Returns false when the app should close. */
    fun back(): Boolean {
        val u = _ui.value
        when {
            u.celebrate -> _ui.update { it.copy(celebrate = false) }
            u.promo -> _ui.update { it.copy(promo = false) }
            u.edit != null -> _ui.update { it.copy(edit = null) }
            u.sheet -> _ui.update { it.copy(sheet = false) }
            u.themeSheet -> _ui.update { it.copy(themeSheet = false) }
            u.hoursSheet -> closeHours()
            u.soundSheet -> closeSound()
            u.screen == Screen.PAYWALL -> closePaywall()
            u.screen == Screen.ONB && u.step > 0 -> onbBack()
            u.screen == Screen.ONB -> go(Screen.WELCOME)
            u.screen == Screen.WARD || (u.screen != Screen.HOME && u.screen != Screen.WELCOME && s.onboarded) -> go(Screen.HOME)
            else -> return false
        }
        return true
    }

    // ---- onboarding & profile ----------------------------------------------------------------

    fun pickMascot(m: Mascot) {
        if (m.premium && !s.premium) return openPaywall(_ui.value.screen)
        setSettings { it.copy(mascot = m) }
    }

    fun setWeight(kg: Int) = setSettings { it.copy(weightKg = kg, goalMl = calcGoal(kg, it.activity)) }
    fun setActivity(a: Int) = setSettings { it.copy(activity = a, goalMl = calcGoal(it.weightKg, a)) }
    fun setUnits(u: Units) = setSettings { it.copy(units = u) }
    fun goalStep(delta: Int) = setSettings { it.copy(goalMl = (it.goalMl + delta).coerceIn(800, 5000)) }

    fun pickAccessory(a: Accessory) {
        if (a.premium && !(s.premium || (a == Accessory.CROWN && s.crownEarned))) return openPaywall(Screen.WARD)
        setSettings { it.copy(accessory = a) }
        wardJob?.cancel()
        _ui.update { it.copy(wardMood = Mood.HAPPY) }
        wardJob = viewModelScope.launch { delay(1200); _ui.update { it.copy(wardMood = Mood.IDLE) } }
    }

    // ---- logging -----------------------------------------------------------------------------

    fun openSheet() = _ui.update { it.copy(sheet = true) }
    fun closeSheet() = _ui.update { it.copy(sheet = false) }
    fun sheetDrink(d: Drink) {
        if (d.premium && !s.premium) return openPaywall(Screen.HOME)
        setSettings { it.copy(lastDrink = d) }
    }
    fun sheetAmount(ml: Int) = setSettings { it.copy(sheetAmount = ml) }

    fun pourStart() {
        moodJob?.cancel()
        _ui.update { it.copy(pouring = true, mood = Mood.DRINK) }
    }

    fun pourEnd(ml: Int) {
        _ui.update { it.copy(pouring = false) }
        add(s.lastDrink, ml)
    }

    fun addFromSheet() = add(s.lastDrink, s.sheetAmount)

    private fun add(drink: Drink, ml: Int) {
        val u = _ui.value
        val before = u.todayTotal
        val now = LocalTime.now()
        val entry = Entry(System.currentTimeMillis(), u.today, now.hour * 60 + now.minute, drink, ml)
        lastAddedId = entry.id
        setEntries(u.entries + entry)
        _ui.update { it.copy(sheet = false, splash = it.splash + 1, mood = Mood.DRINK) }
        showSnack("Added ${u.fmt(ml)} ${drink.label.lowercase()}", undo = true, ms = 4500)

        val after = before + entry.hydration
        moodJob?.cancel()
        moodJob = viewModelScope.launch {
            delay(1500)
            _ui.update { it.copy(mood = if (it.todayTotal >= s.goalMl) Mood.HAPPY else Mood.IDLE) }
        }
        if (after >= s.goalMl && s.celebratedDay != u.today) {
            setSettings { it.copy(celebratedDay = u.today) }
            viewModelScope.launch { delay(900); _ui.update { it.copy(celebrate = true) } }
        }
        if (!s.crownEarned) {
            val totals = Progress.dailyTotals(_ui.value.entries)
            if (Progress.goalDaysThisWeek(totals, s.goalMl, u.today) >= 5) setSettings { it.copy(crownEarned = true) }
        }
    }

    fun undo() {
        snackJob?.cancel()
        val id = lastAddedId ?: return
        lastAddedId = null
        drain(_ui.value.entries.filterNot { it.id == id })
        _ui.update { it.copy(snack = null) }
    }

    /** Applies a smaller log, resetting the celebration and mood if the goal is no longer met. */
    private fun drain(entries: List<Entry>) {
        val u = _ui.value
        setEntries(entries)
        val total = entries.filter { it.day == u.today }.sumOf { it.hydration.toDouble() }
        if (total < s.goalMl) {
            if (s.celebratedDay == u.today) setSettings { it.copy(celebratedDay = -1) }
            _ui.update { it.copy(mood = Mood.IDLE) }
        }
    }

    fun openEdit(e: Entry) = _ui.update { it.copy(edit = EditState(e.id, e.drink, e.ml, e.timeLabel), snack = null) }
    fun closeEdit() = _ui.update { it.copy(edit = null) }
    fun editDrink(d: Drink) {
        if (d.premium && !s.premium) return
        _ui.update { u -> u.copy(edit = u.edit?.copy(drink = d)) }
    }
    fun editAmount(ml: Int) = _ui.update { u -> u.copy(edit = u.edit?.copy(ml = ml.coerceIn(10, 1000))) }

    fun saveEdit() {
        val e = _ui.value.edit ?: return
        drain(_ui.value.entries.map { if (it.id == e.id) it.copy(drink = e.drink, ml = e.ml) else it })
        _ui.update { it.copy(edit = null) }
    }

    fun deleteEdit() {
        val e = _ui.value.edit ?: return
        drain(_ui.value.entries.filterNot { it.id == e.id })
        _ui.update { it.copy(edit = null) }
    }

    // ---- history -----------------------------------------------------------------------------

    fun selectDay(day: Long) {
        val u = _ui.value
        if (day > u.today) return
        if (!s.premium && day < u.today - 6) return openPaywall(Screen.HIST)
        _ui.update { it.copy(selDay = day, dayN = it.dayN + 1) }
    }

    fun shiftMonth(delta: Int) {
        val next = _ui.value.histMonth.plusMonths(delta.toLong())
        if (next.isAfter(YearMonth.now())) return
        _ui.update { it.copy(histMonth = next) }
    }

    /** CSV of the full log, for Sippo+ export. */
    fun csv(): String = buildString {
        append("date,time,drink,amount_ml,hydration_ml\n")
        _ui.value.entries.sortedWith(compareBy({ it.day }, { it.minute })).forEach { e ->
            append("${LocalDate.ofEpochDay(e.day)},${e.timeLabel},${e.drink.key},${e.ml},${Math.round(e.hydration)}\n")
        }
    }

    // ---- reminders & settings ----------------------------------------------------------------

    fun toggleRemind() = setSettings { it.copy(remind = !it.remind) }
    fun setInterval(min: Int) = setSettings { it.copy(intervalMin = min) }

    fun openHours() = _ui.update { it.copy(hoursSheet = true) }
    fun closeHours() = _ui.update { it.copy(hoursSheet = false) }
    /** Start and end hour of the reminder window; the window always spans at least one hour. */
    fun setActiveStart(h: Int) = setSettings { it.copy(activeStart = h.coerceIn(0, it.activeEnd - 1)) }
    fun setActiveEnd(h: Int) = setSettings { it.copy(activeEnd = h.coerceIn(it.activeStart + 1, 24)) }

    fun openSound() = _ui.update { it.copy(soundSheet = true) }
    fun closeSound() = _ui.update { it.copy(soundSheet = false) }
    fun setSound(sound: ReminderSound) {
        setSettings { it.copy(sound = sound) }
        Reminders.createChannel(getApplication(), sound)
        Reminders.preview(getApplication(), sound)
    }

    /** Posts a reminder right now, so the user can check the sound and that notifications are allowed. */
    fun testReminder() {
        val app = getApplication<Application>()
        if (!Reminders.canNotify(app)) return showSnack("Notifications are off for Sippo in system settings")
        Reminders.notify(app, s.sound, "Time for a sip!", "${s.mascot.label} says hi. This is a test reminder.")
    }
    fun smartTap() = if (s.premium) setSettings { it.copy(smart = !it.smart) } else openPaywall(Screen.SETTINGS)
    fun themeTap() = if (s.premium) _ui.update { it.copy(themeSheet = true) } else openPaywall(Screen.SETTINGS)
    fun closeTheme() = _ui.update { it.copy(themeSheet = false) }
    fun setTheme(key: String) = setSettings { it.copy(theme = key) }

    // ---- Sippo+ ------------------------------------------------------------------------------

    fun openPaywall(from: Screen) = _ui.update { it.copy(screen = Screen.PAYWALL, pwFrom = from, nav = it.nav + 1, sheet = false, edit = null, promo = false) }
    fun closePaywall() = go(_ui.value.pwFrom)
    fun closePromo() = _ui.update { it.copy(promo = false) }
    fun closeCelebrate() = _ui.update { it.copy(celebrate = false) }

    fun buy(activity: Activity) {
        if (billing.launchPurchase(activity)) return
        if (BuildConfig.DEBUG) {
            // The product only exists once it is set up in Play Console; let debug builds try premium.
            applyOwnership(true)
            unlocked()
        } else {
            showSnack("Store isn't reachable right now. Try again in a moment.")
        }
    }

    fun restore() {
        billing.refresh()
        showSnack("Checking Google Play for your purchase…")
    }

    fun resetPremiumDebug() = applyOwnership(false)

    private fun unlocked() {
        if (!s.premium) applyOwnership(true)
        _ui.update { it.copy(screen = if (it.screen == Screen.PAYWALL) it.pwFrom else it.screen, nav = it.nav + 1) }
        showSnack("Sippo+ unlocked — enjoy!", ms = 3500)
    }

    private fun applyOwnership(owned: Boolean) {
        if (owned == s.premium) return
        setSettings {
            if (owned) it.copy(premium = true)
            else it.copy(
                premium = false,
                mascot = Mascot.OTTIE,
                accessory = if (it.accessory.premium && !(it.accessory == Accessory.CROWN && it.crownEarned)) Accessory.NONE else it.accessory,
                theme = "lavender",
                lastDrink = if (it.lastDrink.premium) Drink.WATER else it.lastDrink,
            )
        }
    }

    // ---- snackbar ----------------------------------------------------------------------------

    fun showSnack(text: String, undo: Boolean = false, ms: Long = 3500) {
        snackJob?.cancel()
        _ui.update { it.copy(snack = text, snackUndo = undo, snackId = it.snackId + 1) }
        snackJob = viewModelScope.launch { delay(ms); _ui.update { it.copy(snack = null) } }
    }
}
