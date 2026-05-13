APP_DIR  := app
PACKAGE  := com.saxonthune.ranktheplanet
ACTIVITY := $(PACKAGE)/$(PACKAGE).MainActivity
GRADLE   := cd $(APP_DIR) && ./gradlew

.DEFAULT_GOAL := build

.PHONY: help build clean rebuild install run sync tasks stop adb-devices wrapper

help:
	@echo "Targets:"
	@echo "  build       Assemble Android debug APK"
	@echo "  clean       Remove build outputs"
	@echo "  rebuild     clean + build"
	@echo "  install     Install debug APK on connected device/emulator"
	@echo "  run         install + launch MainActivity"
	@echo "  sync        Resolve dependencies and validate Gradle config"
	@echo "  tasks       List all Gradle tasks"
	@echo "  stop        Stop the Gradle daemon"
	@echo "  adb-devices List attached Android devices/emulators"
	@echo "  wrapper     Print Gradle wrapper version"

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
