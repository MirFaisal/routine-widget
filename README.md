# Routine (Android)

Faisal & Akash's daily routine as an Android app with a live home screen widget and reliable reminders.

## What's inside
- **App screen**: the routine web app, bundled offline (`app/src/main/assets/index.html`). Check-offs, streaks, week score, trading journal and the routine editor all work here.
- **Home screen widget** ("Routine now"): clock, current task, progress bar, time left, next task and streak. It updates exactly when each block starts and ends, and every 5 minutes while the screen is on.
- **Reminders**: 5 minutes before each block, even when the app is closed. Turn them on in the app under Settings → Reminders. They are set again after the phone restarts.

The widget and reminders follow whoever is selected in the app (Faisal or Akash) and any routine edits you save in Settings.

## Build and install
1. Install **Android Studio** (latest version).
2. File → Open → choose this `RoutineApp` folder. Wait for Gradle sync to finish (first time downloads a few hundred MB).
3. Build → Build App Bundle(s) / APK(s) → **Build APK(s)**.
4. The APK is at `app/build/outputs/apk/debug/app-debug.apk`. Send it to your phone and install it (allow "Install unknown apps" when asked).
   Or connect your phone with USB debugging on and press the green **Run** button.

## Add the widget
Long-press your home screen → Widgets → **Routine** → drag "Routine now" to the home screen. Resize it if you like.

## Phone settings for reliable reminders
Some phones (Xiaomi, Oppo, Vivo, Realme, Samsung) stop apps in the background to save battery. For Routine:
- Settings → Apps → Routine → Battery → **Unrestricted** (or "No restrictions").
- Xiaomi: also turn on **Autostart** for Routine.
- Allow notifications when the app asks.

## Change the routine
Easiest: edit it in the app (Settings → Edit routine). The widget and reminders update right away.

To change the built-in default routine, edit the `DEFAULTS` section in `app/src/main/assets/index.html`, then regenerate `app/src/main/assets/default_state.json` (or just open the app once; it sends the routine to the widget automatically).

## Files
- `MainActivity.kt` – app screen (WebView) and the JavaScript bridge (`window.RoutineNative`)
- `RoutineWidget.kt` – home screen widget
- `Scheduler.kt` – alarms for live widget updates and reminders
- `Routine.kt` – routine day/time logic and saved state
- `Receivers.kt` – alarm and phone-restart handlers
