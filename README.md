# HydroBud — Minimal Animated Water & Step Tracker (Android 9+)

HydroBud is a lightweight Jetpack Compose daily water intake and always-on step counter app (`minSdk = 28`, `targetSdk = 35`).

## Build Environment
- **JDK**: 17 (Temurin 17 recommended)
- **Gradle**: 8.9
- **Android Gradle Plugin**: 8.7.2
- **Kotlin**: 2.0.21
- **Android SDK**: `compileSdk = 35`, `build-tools = 35.0.0`

## How to Build the APK
1. Extract `HydroBud-Android-App-4.zip` and enter the `HydroBud/` folder.
2. Run:
   ```bash
   gradle assembleDebug
   ```
   *(Or open the `HydroBud/` folder in Android Studio and click **Build > Build Bundle(s) / APK(s) > Build APK(s)**.)*
3. Install the generated debug APK:
   ```bash
   adb install -r app/build/outputs/apk/debug/app-debug.apk
   ```
   *(For a smaller R8-minified APK, run `gradle assembleRelease` and install `app/build/outputs/apk/release/app-release.apk`.)*
