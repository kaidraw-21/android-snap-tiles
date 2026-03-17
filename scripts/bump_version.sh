#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
BUILD_FILE="$SCRIPT_DIR/../app/build.gradle.kts"

usage() {
  echo "Usage: $0 <major|minor|patch>"
  exit 1
}

[[ $# -ne 1 ]] && usage

BUMP_TYPE="$1"
if [[ "$BUMP_TYPE" != "major" && "$BUMP_TYPE" != "minor" && "$BUMP_TYPE" != "patch" ]]; then
  usage
fi

# Extract current values
CURRENT_NAME=$(grep 'versionName' "$BUILD_FILE" | sed 's/.*"\(.*\)".*/\1/')
CURRENT_CODE=$(grep 'versionCode' "$BUILD_FILE" | sed 's/[^0-9]*//g')

if [[ -z "$CURRENT_NAME" || -z "$CURRENT_CODE" ]]; then
  echo "Error: Could not read version from $BUILD_FILE"
  exit 1
fi

# Parse semver (handle both "1.0" and "1.0.0" formats)
IFS='.' read -r MAJOR MINOR PATCH <<< "$CURRENT_NAME"
PATCH="${PATCH:-0}"

echo "Current: $MAJOR.$MINOR.$PATCH (code $CURRENT_CODE)"

case "$BUMP_TYPE" in
  major) MAJOR=$((MAJOR + 1)); MINOR=0; PATCH=0 ;;
  minor) MINOR=$((MINOR + 1)); PATCH=0 ;;
  patch) PATCH=$((PATCH + 1)) ;;
esac

NEW_NAME="$MAJOR.$MINOR.$PATCH"
NEW_CODE=$((CURRENT_CODE + 1))

echo "New:     $NEW_NAME (code $NEW_CODE)"

# Update build.gradle.kts
if [[ "$(uname)" == "Darwin" ]]; then
  sed -i '' "s/versionName = \"$CURRENT_NAME\"/versionName = \"$NEW_NAME\"/" "$BUILD_FILE"
  sed -i '' "s/versionCode = $CURRENT_CODE/versionCode = $NEW_CODE/" "$BUILD_FILE"
else
  sed -i "s/versionName = \"$CURRENT_NAME\"/versionName = \"$NEW_NAME\"/" "$BUILD_FILE"
  sed -i "s/versionCode = $CURRENT_CODE/versionCode = $NEW_CODE/" "$BUILD_FILE"
fi

echo ""
echo "==> Bumped to v$NEW_NAME (code $NEW_CODE)"
echo "    Run 'scripts/release.sh' when ready to tag and push."
