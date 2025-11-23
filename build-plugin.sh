#!/usr/bin/env bash
set -euo pipefail

# Build helper for the WordPress plugin. Increments the version in the plugin
# header and creates a zip package containing all plugin files.

PLUGIN_DIR="wordpress-plugin/innernet-webapp"
PLUGIN_FILE="${PLUGIN_DIR}/innernet-webapp.php"
OUTPUT_DIR="wordpress-plugin/build"

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

mkdir -p "${OUTPUT_DIR}"
zip_name="${OUTPUT_DIR}/innernet-webapp-${new_version}.zip"

echo "Erstelle Zip: ${zip_name}"
(
  cd "${PLUGIN_DIR}"
  zip -r "../build/innernet-webapp-${new_version}.zip" .
)

echo "Fertig: ${zip_name}"
