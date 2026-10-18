package com.sippo.app.data

import androidx.compose.ui.graphics.Color
import com.sippo.app.ui.theme.Sym

/** A drink and how much of it counts toward hydration. */
enum class Drink(
    val key: String,
    val label: String,
    val color: Color,
    val tint: Color,
    val factor: Float,
    val icon: String,
    val premium: Boolean = false,
) {
    WATER("water", "Water", Color(0xFF4A8DFF), Color(0xFFE3EFFF), 1f, Sym.WATER_DROP),
    TEA("tea", "Tea", Color(0xFF4FA85A), Color(0xFFE4F4E4), .9f, Sym.EMOJI_FOOD_BEVERAGE),
    COFFEE("coffee", "Coffee", Color(0xFFA8714A), Color(0xFFF3E7DD), .8f, Sym.COFFEE),
    JUICE("juice", "Juice", Color(0xFFF28A1E), Color(0xFFFFF0DC), .85f, Sym.LOCAL_DRINK),
    MILK("milk", "Milk", Color(0xFF8A94B8), Color(0xFFEEF0F8), .9f, Sym.GLASS_CUP, premium = true),
    SODA("soda", "Soda", Color(0xFFE0457B), Color(0xFFFFE3EC), .8f, Sym.LOCAL_BAR, premium = true),
    SMOOTHIE("smoothie", "Smoothie", Color(0xFF9B5DE5), Color(0xFFF0E6FF), .85f, Sym.BLENDER, premium = true),
    SPORTS("sports", "Sports", Color(0xFF12A3A8), Color(0xFFDCF0F0), .95f, Sym.WATER_BOTTLE, premium = true);

    companion object {
        val free = entries.filter { !it.premium }
        fun of(key: String): Drink = entries.firstOrNull { it.key == key } ?: WATER
    }
}

/** One logged drink. [day] is an epoch day, [minute] is minutes since local midnight. */
data class Entry(
    val id: Long,
    val day: Long,
    val minute: Int,
    val drink: Drink,
    val ml: Int,
) {
    val hydration: Float get() = ml * drink.factor
    val timeLabel: String get() = "%02d:%02d".format(minute / 60, minute % 60)
}

enum class Mascot(val key: String, val label: String, val species: String, val tint: Color, val blurb: String) {
    OTTIE("ottie", "Ottie", "Sea otter", Color(0xFFFBE9DA), "Floats on its back as your water rises. Claps its paws every sip."),
    PINGO("pingo", "Pingo", "Penguin", Color(0xFFE3E6FB), "A tuxedoed little drama queen. Flaps wildly when you hit your goal."),
    AXEL("axel", "Axel", "Axolotl", Color(0xFFFFE3EA), "Lives in water anyway. Gills wiggle faster the more you drink.");

    val premium: Boolean get() = this != OTTIE

    companion object {
        fun of(key: String): Mascot = entries.firstOrNull { it.key == key } ?: OTTIE
    }
}

enum class Accessory(val key: String, val label: String, val icon: String, val premium: Boolean) {
    NONE("none", "None", Sym.BLOCK, false),
    CAP("cap", "Cap", Sym.SPORTS_BASEBALL, false),
    BOW("bow", "Bow", Sym.REDEEM, true),
    SHADES("shades", "Shades", Sym.EYEGLASSES, true),
    CROWN("crown", "Crown", Sym.WORKSPACE_PREMIUM, true);

    companion object {
        fun of(key: String): Accessory = entries.firstOrNull { it.key == key } ?: NONE
    }
}

enum class Units { ML, OZ }

/** Reminder sound. Each maps to its own notification channel, because a channel's sound is fixed once created. */
enum class ReminderSound(val key: String, val label: String) {
    POP("pop", "Bubble pop"),
    DEFAULT("default", "Phone default"),
    SILENT("silent", "Silent");

    companion object {
        fun of(key: String): ReminderSound = entries.firstOrNull { it.key == key } ?: POP
    }
}

data class Settings(
    val onboarded: Boolean = false,
    val mascot: Mascot = Mascot.OTTIE,
    val accessory: Accessory = Accessory.NONE,
    val weightKg: Int = 70,
    val activity: Int = 1,
    val units: Units = Units.ML,
    val goalMl: Int = calcGoal(70, 1),
    val remind: Boolean = true,
    val intervalMin: Int = 90,
    val smart: Boolean = true,
    val activeStart: Int = 8,
    val activeEnd: Int = 22,
    val sound: ReminderSound = ReminderSound.POP,
    val theme: String = "lavender",
    val premium: Boolean = false,
    val lastDrink: Drink = Drink.WATER,
    val sheetAmount: Int = 250,
    val celebratedDay: Long = -1,
    val crownEarned: Boolean = false,
)

/** Daily goal from body weight (33 ml/kg) plus activity, rounded to 50 ml. */
fun calcGoal(weightKg: Int, activity: Int): Int =
    Math.round((weightKg * 33 + activity * 350) / 50f) * 50

fun formatAmount(ml: Number, units: Units): String {
    val v = ml.toFloat()
    return if (units == Units.OZ) "${Math.round(v / 29.574f)} oz"
    else "%,d ml".format(Math.round(v))
}
