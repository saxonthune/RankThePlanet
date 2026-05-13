# iOS App

The Xcode project for this target must be created on macOS.

After cloning the repo on a Mac:

1. Open Android Studio (Hedgehog or later)
2. File → Open → `<repo>/app`
3. Wait for Gradle sync to complete
4. Use Android Studio's KMP wizard (File → New → Kotlin Multiplatform → iOS Application) to generate the Xcode project, or use the KMP plugin to create it.

Alternatively, run `./gradlew linkReleaseFrameworkIosSimulatorArm64` to build the framework, then wire it into a new Xcode project manually.

The iOS framework entry point is `MainViewController()` in `composeApp/src/iosMain/kotlin/com/saxonthune/ranktheplanet/MainViewController.kt`.
