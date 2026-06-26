# Elder Launcher

Elder Launcher is an Android launcher designed for seniors, focused on simplicity, legibility, and accessibility. It allows pinning favorite apps and contacts to the homescreen for quick and easy access.

<span>
<img src="fastlane/metadata/android/en-US/images/phoneScreenshots/1.png" height="350" alt="Homescreen with favourite apps">
<img src="fastlane/metadata/android/en-US/images/phoneScreenshots/2.png" height="350" alt="Homescreen with favourite contacts">
<img src="fastlane/metadata/android/en-US/images/phoneScreenshots/3.png" height="350" alt="App Drawer">
</span>

## Tech Stack

Elder Launcher is built as a **pure native Android application** using modern Jetpack libraries and guidelines:

* **Programming Language**: [Kotlin](https://kotlinlang.org/)
* **UI Framework**: [Jetpack Compose](https://developer.android.com/compose) with [Material Design 3 (M3)](https://m3.material.io/)
* **Architecture**: MVVM (Model-View-ViewModel) using clean, repository-based data layers
* **Asynchronous Operations**: Kotlin Coroutines & reactive Flows
* **Local Settings & Storage**: [Jetpack DataStore Preferences](https://developer.android.com/topic/libraries/architecture/datastore) (replacing legacy blocking SharedPreferences)
* **Image Loading**: [Coil](https://coil-kt.github.io/coil/) (for asynchronous, thread-safe icon and avatar loading)

## Modern Features & Optimizations

* **Smooth 120Hz Scrolling**: Optimized for high-refresh-rate screens using stable data models annotated with `@Immutable` and unique keys on all lazy lists to prevent redundant recompositions.
* **Dynamic Material You Coloring**: Integrates native Material You dynamic color schemes (Android 12+) with high-contrast accessibility fallback options for light and dark themes.
* **Native Icon Zoom Transitions**: Configures intent `sourceBounds` dynamically from Compose coordinates, allowing the OS to play native zoom-out scale animations on app launch and zoom-in scale animations on return-to-home.
* **Gesture & Lifecycle Integrity**: Intercepts the home gesture (`onNewIntent`) and back gesture (`BackHandler`) to smoothly update the active instance navigation state, preventing activity reconstruction lag and reload delays.
* **Accessibility & Font Scaling**: All UI text uses scale-independent pixels (`sp`) and is styled using oversized fonts in `Theme.kt`, satisfying Android's display size, font size, bold text, and color inversion accessibility features.
* **Native Per-App Language Selection**: Fully supports Android 13's built-in multi-lingual preferences, providing native translations for 11 locales: English, Czech, German, Spanish, French, Hindi, Italian, Dutch, Polish, Portuguese, and Russian.

## Build and Run

To build the project locally, ensure you have the Android SDK (API 34) and Gradle installed:

1. Open the `android` folder in **Android Studio** or your preferred development environment.
2. Build the debug APK using Gradle:
   ```bash
   cd android
   gradle assembleDebug
   ```
3. The compiled APK will be located under `build/app/outputs/apk/debug/app-debug.apk`.

## More Screenshots

<span>
<img src="fastlane/metadata/android/en-US/images/phoneScreenshots/4.png" height="350" alt="Homescreen call shortcut dialog">
<img src="fastlane/metadata/android/en-US/images/phoneScreenshots/5.png" height="350" alt="Favourite App selection screen">
<img src="fastlane/metadata/android/en-US/images/phoneScreenshots/6.png" height="350" alt="Favourite Contacts reorder screen">
</span>

<br/>

### Dark Mode (Android 10+)
Can be enabled from system settings.

<span>
<img src="fastlane/metadata/android/en-US/images/phoneScreenshots/7.png" height="350" alt="Dark Mode: Homescreen with favourite apps">
<img src="fastlane/metadata/android/en-US/images/phoneScreenshots/8.png" height="350" alt="Dark Mode: Homescreen with favourite contacts">
<img src="fastlane/metadata/android/en-US/images/phoneScreenshots/9.png" height="350" alt="Dark Mode: App Drawer">
</span>

## License

This project is licensed under the [MIT License](LICENSE.md).
