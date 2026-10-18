# Sippo

A cute water tracker for Android, inspired by Waterllama. Pick a hydration buddy (Ottie the otter, Pingo the penguin or Axel the axolotl), hold to pour, and watch the bowl fill up. No account; everything is stored on the phone.

Built with Kotlin and Jetpack Compose. All mascots, water, splashes, confetti and the nav bar are drawn in code.

## Features

**Free**
- Get started screen: the three mascots drop into a full-screen pool that sloshes when you drag or tilt the phone
- Onboarding: pick a buddy, set a goal from weight and activity, choose ml or oz
- Today: a bowl that fills with layered waves and bubbles, with tilt and swipe to slosh. Hold to pour (about 150 ml/s, speeding up to 300 ml/s), or use Add drink for water, tea, coffee or juice
- Undo for 4.5 s after every drink, and tap any of today's drinks to edit or delete it
- Stats ring, a 7-day bar chart and totals by drink
- History calendar for the last 7 days, with each day's drinks
- Awards and a weekly challenge (5 goal days unlocks the Crown)
- Drink reminders with WorkManager, active 08:00–22:00, with a bubble-pop sound
- Wardrobe: Ottie with the Cap

**Sippo+ (one-time €2.99, Play Billing)**
- Pingo, Axel, and the Bow, Shades and Crown accessories
- Unlimited history with month navigation and CSV export
- Smart reminders that stay quiet when you're on pace or just had a drink
- More drinks: milk, soda, smoothie and sports drinks
- Home-screen widget with a one-tap 250 ml pour
- Themes: Lavender, Midnight (dark), Sunset and Ocean
- Streak freeze: one missed day per week is forgiven

## Project layout

```
app/src/main/java/com/sippo/app/
  MainActivity.kt, SippoApplication.kt, SippoViewModel.kt
  data/        Models, DataStore repository, pure progress maths (unit-tested)
  billing/     Play Billing wrapper for the lifetime product
  reminders/   Notification channel + WorkManager worker
  widget/      Home-screen widget (RemoteViews)
  ui/theme/    Colour themes, Plus Jakarta Sans, Material Symbols glyphs
  ui/mascot/   Ottie, Pingo and Axel drawn on Canvas
  ui/water/    Bowl and pool physics, bubbles, splash, tilt sensor
  ui/components, ui/screens
```

## Building

CI (`.github/workflows/android.yml`) runs on every push and pull request:

- unit tests, lint and `assembleDebug`, with the debug APK attached to the run as the `sippo-debug-apk` artifact
- an emulator smoke test (`app/src/androidTest/.../SmokeTest.kt`) on API 34. It walks through the whole app: onboarding, add, undo, pour, edit, every tab, the paywall, a theme switch and the wardrobe.

When a step fails, its compiler, lint or crash output also appears as an annotation on the run summary.

Locally you need JDK 17 and the Android SDK (API 35):

```
./gradlew assembleDebug        # app/build/outputs/apk/debug/
./gradlew testDebugUnitTest
```

Debug builds use the application ID `com.sippo.app.debug`, so they install alongside the release build.

## Play Store setup

1. **Create the app** in Play Console with the package name `com.sippo.app`.
2. **Upload signing:** generate a key with `keytool -genkeypair -v -keystore sippo-release.jks -alias sippo -keyalg RSA -keysize 2048 -validity 10000`. Then add these repository secrets: `SIPPO_KEYSTORE_BASE64` (the output of `base64 -w0 sippo-release.jks`), `SIPPO_KEYSTORE_PASSWORD`, `SIPPO_KEY_ALIAS` and `SIPPO_KEY_PASSWORD`.
3. **Build a bundle:** push a tag like `v1.0.0`, or run the workflow by hand. The `release` job uploads a signed `sippo-release-aab`, and the version code is the run number. To build locally instead, put `storeFile`, `storePassword`, `keyAlias` and `keyPassword` in an untracked `keystore.properties`.
4. **In-app product:** after the first upload to a testing track, create a one-time product with the ID **`sippo_plus_lifetime`** at €2.99 and activate it. Then add license testers to try the purchase.

Until the product exists, the debug build unlocks Sippo+ locally when you tap Unlock. Settings also shows a "reset purchase (debug)" link. Release builds only unlock through Google Play.

> Purchases are verified on the device only, because there's no server. That's normal for a small offline app, but a determined user could bypass it.

## Licences

- Plus Jakarta Sans: SIL Open Font License, see `licenses/`
- Material Symbols Rounded: Apache 2.0. The fonts are subset and instanced, so the app ships only the glyphs and weights it uses.
