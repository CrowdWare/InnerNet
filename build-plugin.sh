#!/usr/bin/env bash
set -euo pipefail

# Build helper for the WordPress plugin. Increments the version in the plugin
# header and creates a zip package containing all plugin files.

PLUGIN_DIR="wordpress-plugin/innernet-webapp"
PLUGIN_FILE="${PLUGIN_DIR}/innernet-webapp.php"
OUTPUT_DIR="wordpress-plugin/build"
APP_RESOURCES_DIR="build/processedResources/js/main"
APP_BUNDLE_DIR="build/kotlin-webpack/js/developmentExecutable"
APP_TARGET_DIR="${PLUGIN_DIR}/app"
CONTENT_SOURCE_DIR="content"
CONTENT_TARGET_DIR="${PLUGIN_DIR}/content"

if [[ ! -f "${PLUGIN_FILE}" ]]; then
  echo "Plugin file not found at ${PLUGIN_FILE}" >&2
  exit 1
fi

current_version="$(awk -F': ' '/^[[:space:]]*\* Version:/ {print $2; exit}' "${PLUGIN_FILE}")"

if [[ ! "${current_version}" =~ ^([0-9]+)\.([0-9]+)\.([0-9]+)$ ]]; then
  echo "Konnte Versionsnummer nicht parsen: ${current_version}" >&2
  exit 1
fi

major="${BASH_REMATCH[1]}"
minor="${BASH_REMATCH[2]}"
patch="${BASH_REMATCH[3]}"
new_version="${major}.${minor}.$((patch + 1))"

echo "Aktualisiere Version: ${current_version} -> ${new_version}"
perl -pi -e "s/(\\* Version:\\s*)${current_version}/\${1}${new_version}/" "${PLUGIN_FILE}"

# Build aktuelle Web-Artefakte
echo "Baue JS-Bundle über gradlew (Development Webpack) …"
./gradlew jsBrowserDevelopmentWebpack

# Kopiere frische Dateien ins Plugin-App-Verzeichnis
if [[ ! -d "${APP_RESOURCES_DIR}" || ! -d "${APP_BUNDLE_DIR}" ]]; then
  echo "Build-Ausgabe nicht gefunden (Resources: ${APP_RESOURCES_DIR}, Bundle: ${APP_BUNDLE_DIR})" >&2
  exit 1
fi

echo "Synchronisiere Ressourcen -> ${APP_TARGET_DIR}/"
rsync -av --delete "${APP_RESOURCES_DIR}/" "${APP_TARGET_DIR}/"

echo "Kopiere Bundle (JS/WASM) -> ${APP_TARGET_DIR}/"
rsync -av "${APP_BUNDLE_DIR}/" "${APP_TARGET_DIR}/"

echo "Kopiere Content-Dateien -> ${CONTENT_TARGET_DIR}/"
mkdir -p "${CONTENT_TARGET_DIR}"
rsync -av --delete "${CONTENT_SOURCE_DIR}/" "${CONTENT_TARGET_DIR}/"

mkdir -p "${OUTPUT_DIR}"
zip_name="${OUTPUT_DIR}/innernet-webapp-${new_version}.zip"

echo "Erstelle Zip: ${zip_name}"
(
  cd "${PLUGIN_DIR}"
  zip -r "../build/innernet-webapp-${new_version}.zip" . -x "README.md"
)

echo "Fertig: ${zip_name}"
