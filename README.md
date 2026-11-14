# HydroBud — Pill-UI Animated Water Tracker (Android 9+)

HydroBud is a lightweight, hardware-accelerated Jetpack Compose water intake tracker designed to run smoothly at 60fps on both low-end and flagship Android devices (`minSdk = 28` / Android 9.0+).

## Features
- **Pill-Based UI**: Every button, card, progress bar, dialog, and floating bottom navigation bar uses rounded pill geometry.
- **3 Animated Characters**: Choose between **Droppy**, **Bubbels**, and **Cloudy**—each rendered on hardware-accelerated Compose `Canvas` with blinking eyes, mood states (Thirsty, Happy, Energized, Celebrating), tap-squish physics, and a dual-layer liquid wave tank.
- **Animated Loading Screen & Cute Logo**: Custom vector adaptive launcher icon and animated splash screen.
- **User Profile & Smart Goal**: Customize your name, body weight, activity level, wake/sleep hours, quick cup sizes, and auto-calculated daily limit.
- **Periodic Notifications**: WorkManager-powered hydration reminders during your active hours with a one-tap `+250 ml Sip` notification action.
- **Home Screen Widget**: Pill-shaped widget showing your progress bar, character status, and instant `+250 ml` / `+500 ml` logging buttons.

## How to Build the APK
1. Extract `HydroBud-Android-App.zip`.
2. Open the `HydroBud` folder in **Android Studio** (Ladybug / Koala or newer) with JDK 17.
3. Let Gradle sync the dependencies.
4. Click **Build > Build Bundle(s) / APK(s) > Build APK(s)** (or run `./gradlew assembleDebug` in Terminal if you have Gradle installed).
5. Install `app/build/outputs/apk/debug/app-debug.apk` on any Android 9+ device.
