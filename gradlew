#!/bin/sh
# Thin wrapper — delegates to the actual Gradle wrapper in app/
SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
cd "$SCRIPT_DIR/app"
exec ./gradlew "$@"
