#!/bin/bash
# Build a fresh, unsigned local macOS app for the architecture of JAVA_HOME.
set -euo pipefail
project_dir="$(cd "$(dirname "$0")/.." && pwd)"
destination="${1:-$project_dir/build/distribution}"
: "${JAVA_HOME:?Set JAVA_HOME to a macOS JDK 17 installation}"
app_dir="$destination/자석의 모험 v3.app"
if [ -e "$app_dir" ]; then
  echo "Output already exists. Choose a fresh destination directory: $app_dir" >&2
  exit 1
fi
"$project_dir/gradlew" -p "$project_dir" :lwjgl3:jar
mkdir -p "$app_dir/Contents/MacOS" "$app_dir/Contents/Resources"
"$JAVA_HOME/bin/jlink" --add-modules java.base,java.desktop,jdk.unsupported --strip-debug --no-header-files --no-man-pages --compress=2 --output "$app_dir/Contents/Resources/runtime"
cp "$project_dir/packaging/Info.plist" "$app_dir/Contents/Info.plist"
cp "$project_dir/packaging/MagnetAdventure" "$app_dir/Contents/MacOS/MagnetAdventure"
cp "$project_dir/packaging/MagnetAdventure.icns" "$app_dir/Contents/Resources/MagnetAdventure.icns"
cp "$project_dir/lwjgl3/build/libs/Magnet-Adventure.jar" "$app_dir/Contents/Resources/Magnet-Adventure.jar"
chmod +x "$app_dir/Contents/MacOS/MagnetAdventure"
echo "Created: $app_dir"
