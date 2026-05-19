APP_DIR  := app
PACKAGE  := com.saxonthune.ranktheplanet
ACTIVITY := $(PACKAGE)/$(PACKAGE).MainActivity
GRADLE   := cd $(APP_DIR) && ./gradlew

# --- iOS ---
SIM        ?= iPhone 16
WORKSPACE  := $(APP_DIR)/iosApp/iosApp.xcworkspace
DERIVED    := $(APP_DIR)/build/ios
APP_BUNDLE := $(DERIVED)/Build/Products/Debug-iphonesimulator/iosApp.app
# xcodebuild runs gradlew (via the CocoaPods sync phase); it needs a JDK.
JAVA_HOME  ?= /Applications/Android Studio.app/Contents/jbr/Contents/Home
export JAVA_HOME

.DEFAULT_GOAL := build

.PHONY: help build clean rebuild install run sync tasks stop adb-devices wrapper \
        ios-build ios-run ios-debug ios-logs ios-crash ios-pod-install ios-clean

help:
	@echo "Android targets:"
	@echo "  build         Assemble Android debug APK"
	@echo "  clean         Remove build outputs"
	@echo "  rebuild       clean + build"
	@echo "  install       Install debug APK on connected device/emulator"
	@echo "  run           install + launch MainActivity"
	@echo "  sync          Resolve dependencies and validate Gradle config"
	@echo "  tasks         List all Gradle tasks"
	@echo "  stop          Stop the Gradle daemon"
	@echo "  adb-devices   List attached Android devices/emulators"
	@echo "  wrapper       Print Gradle wrapper version"
	@echo ""
	@echo "iOS targets (override sim with: make ios-run SIM='iPhone 16 Pro'):"
	@echo "  ios-build       Build the app for the iOS simulator"
	@echo "  ios-run         Build, (re)install, and launch on the simulator"
	@echo "  ios-debug       ios-run, but attach the console (prints crash output)"
	@echo "  ios-logs        Stream the simulator system log for the app"
	@echo "  ios-crash       Print the most recent iOS crash report"
	@echo "  ios-pod-install Regenerate the Xcode workspace + Pods"
	@echo "  ios-clean       Remove the iOS derived-data dir"

build:
	$(GRADLE) :composeApp:assembleDebug

clean:
	$(GRADLE) clean

rebuild: clean build

install:
	$(GRADLE) :composeApp:installDebug

run: install
	adb shell am start -n $(ACTIVITY)

sync:
	$(GRADLE) help

tasks:
	$(GRADLE) tasks

stop:
	$(GRADLE) --stop

adb-devices:
	adb devices

wrapper:
	$(GRADLE) --version

# --- iOS ---

ios-pod-install:
	$(GRADLE) :composeApp:podInstall

ios-build:
	xcodebuild -workspace $(WORKSPACE) -scheme iosApp -configuration Debug \
		-destination 'platform=iOS Simulator,name=$(SIM)' \
		-derivedDataPath $(DERIVED) build

ios-run: ios-build
	xcrun simctl boot "$(SIM)" 2>/dev/null || true
	open -a Simulator
	xcrun simctl terminate "$(SIM)" $(PACKAGE) 2>/dev/null || true
	xcrun simctl install "$(SIM)" "$(APP_BUNDLE)"
	xcrun simctl launch "$(SIM)" $(PACKAGE)

# Like ios-run, but stays attached and prints stdout/stderr (including the
# "Uncaught Kotlin exception" + stack trace). Blocks until the app exits.
ios-debug: ios-build
	xcrun simctl boot "$(SIM)" 2>/dev/null || true
	open -a Simulator
	xcrun simctl terminate "$(SIM)" $(PACKAGE) 2>/dev/null || true
	xcrun simctl install "$(SIM)" "$(APP_BUNDLE)"
	xcrun simctl launch --console-pty "$(SIM)" $(PACKAGE)

# Stream the simulator system log for the app (native/MapLibre errors land here).
ios-logs:
	xcrun simctl spawn "$(SIM)" log stream --level debug \
		--predicate 'process == "iosApp"'

# Print the most recent iOS crash report.
ios-crash:
	@ls -t ~/Library/Logs/DiagnosticReports/iosApp-*.ips 2>/dev/null | head -1 \
		| xargs -I{} sh -c 'echo "==> {}"; cat "{}"' \
		|| echo "No iosApp crash reports found."

ios-clean:
	rm -rf $(DERIVED)
