# iOS App

Xcode project for the RankThePlanet iOS target. Hosts the shared Compose UI
(`MainViewController()` from `composeApp/src/iosMain`) inside a SwiftUI shell.

## Prerequisites

- Full **Xcode** (not just Command Line Tools):
  `sudo xcode-select -s /Applications/Xcode.app/Contents/Developer`
- A JDK (Android Studio bundles one).
- **CocoaPods**: `brew install cocoapods` (or `sudo gem install cocoapods`).

The iOS build pulls the MapLibre Native framework as a CocoaPod, declared in
`composeApp/build.gradle.kts` via the Kotlin CocoaPods plugin.

## First-time setup

```
cd app
./gradlew :composeApp:podInstall   # generates iosApp/iosApp.xcworkspace
```

## Running

Open **`iosApp/iosApp.xcworkspace`** (the `.xcworkspace`, not the `.xcodeproj`)
in Xcode, pick a simulator, and Run. Android Studio with the KMP plugin can also
launch it directly.

The Kotlin framework is compiled by Gradle automatically as part of the build
(driven by the generated `composeApp` pod).
