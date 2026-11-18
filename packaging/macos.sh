#!/usr/bin/env bash
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
APP_NAME="MyBeautifulGit-chan"
APP_VERSION="${APP_VERSION:-1.0.0}"
PACKAGE_VERSION="${PACKAGE_VERSION:-1.0.0}"
CHANNEL="${RELEASE_CHANNEL:-chan}"
ARCH="${PACKAGE_ARCH:-$(uname -m)}"
case "$ARCH" in
    arm64|aarch64) ARCH="arm64" ;;
    x64|x86_64|amd64) ARCH="x64" ;;
    *) echo "Unsupported macOS architecture: $ARCH" >&2; exit 1 ;;
esac

DIST="$ROOT/dist"
BUILD_DIR="$ROOT/build/packaging"
INPUT="$BUILD_DIR/input"
JAR="$ROOT/build/tasks/_desktopApp_executableJarJvm/desktopApp-jvm-executable.jar"
JPACKAGE="${JAVA_HOME:+$JAVA_HOME/bin/}jpackage"

PYTHON="${PYTHON:-}"
if [[ -z "$PYTHON" ]]; then
    PYTHON="$(command -v python3 || command -v python || true)"
fi
[[ -n "$PYTHON" ]] || { echo "Python 3 is required to prepare the jpackage input" >&2; exit 1; }

rm -rf "$DIST"
mkdir -p "$DIST"
bash "$ROOT/kotlin" package -m desktopApp -f executable-jar
test -f "$JAR" || { echo "Executable jar not found: $JAR" >&2; exit 1; }
"$PYTHON" "$ROOT/packaging/prepare-runtime.py" --jar "$JAR" --output "$INPUT"

JPACKAGE_VERSION="$("$JPACKAGE" --version | cut -d+ -f1)"
if [[ "$(printf '%s\n' "25.0" "$JPACKAGE_VERSION" | sort -V | head -1)" != "25.0" ]]; then
    echo "JDK 25+ is required for packaging (found jpackage $JPACKAGE_VERSION)" >&2
    exit 1
fi

MODULES="java.base,java.desktop,java.logging,java.management,java.naming,java.net.http,java.prefs,java.security.jgss,java.sql,java.xml,jdk.charsets,jdk.crypto.ec,jdk.localedata,jdk.unsupported,jdk.zipfs"
common=(
    --type app-image
    --name "$APP_NAME"
    --app-version "$PACKAGE_VERSION"
    --vendor "Baconana-chan"
    --description "$APP_NAME — terminal-flavored Git client"
    --copyright "GPL-2.0-only"
    --input "$INPUT"
    --main-jar "desktopApp-jvm.jar"
    --main-class "org.baconana.mybeautifulgit_chan.MainKt"
    --dest "$DIST"
    --java-options "-Dfile.encoding=UTF-8"
    --java-options "-Xmx2048m"
    --add-modules "$MODULES"
    --mac-package-identifier "org.baconana.mybeautifulgitchan"
    --mac-package-name "$APP_NAME"
)
if [[ -f "$ROOT/packaging/icon.icns" ]]; then
    common+=(--icon "$ROOT/packaging/icon.icns")
fi
"$JPACKAGE" "${common[@]}"

APP_BUNDLE="$DIST/$APP_NAME.app"
if [[ ! -d "$APP_BUNDLE" ]]; then
    APP_BUNDLE="$(find "$DIST" -maxdepth 1 -type d -name '*.app' -print -quit)"
fi
if [[ -z "$APP_BUNDLE" || ! -d "$APP_BUNDLE" ]]; then
    echo "jpackage did not produce a macOS .app bundle" >&2
    exit 1
fi

signed=false
notarized=false
IDENTITY=""
KEYCHAIN=""
if [[ -n "${MACOS_CERTIFICATE_P12:-}" && -n "${MACOS_CERTIFICATE_PASSWORD:-}" ]]; then
    KEYCHAIN="$BUILD_DIR/app-signing.keychain-db"
    KEYCHAIN_PASSWORD="${MACOS_KEYCHAIN_PASSWORD:-$(openssl rand -hex 16)}"
    CERT_PATH="$BUILD_DIR/app-signing.p12"
    security delete-keychain "$KEYCHAIN" 2>/dev/null || true
    printf '%s' "$MACOS_CERTIFICATE_P12" | base64 -D > "$CERT_PATH"
    security create-keychain -p "$KEYCHAIN_PASSWORD" "$KEYCHAIN"
    security set-keychain-settings -lut 21600 "$KEYCHAIN"
    security unlock-keychain -p "$KEYCHAIN_PASSWORD" "$KEYCHAIN"
    security import "$CERT_PATH" -P "$MACOS_CERTIFICATE_PASSWORD" -A -t cert \
        -f pkcs12 -k "$KEYCHAIN"
    security set-key-partition-list -S apple-tool:,apple: -k "$KEYCHAIN_PASSWORD" \
        "$KEYCHAIN" >/dev/null
    security list-keychains -d user -s "$KEYCHAIN" login.keychain-db
    security default-keychain -s "$KEYCHAIN"

    if [[ -n "${MACOS_SIGNING_IDENTITY:-}" ]]; then
        IDENTITY="$MACOS_SIGNING_IDENTITY"
    else
        IDENTITY="$(security find-identity -v -p codesigning "$KEYCHAIN" |
            sed -n 's/.*"\(Developer ID Application[^"]*\)".*/\1/p' | head -1)"
    fi
    if [[ -z "$IDENTITY" ]]; then
        echo "No Developer ID Application identity found in the imported keychain" >&2
        exit 1
    fi

    codesign --force --deep --options runtime --timestamp \
        --entitlements "$ROOT/packaging/macos/entitlements.plist" \
        --sign "$IDENTITY" "$APP_BUNDLE"
    codesign --verify --deep --strict --verbose=2 "$APP_BUNDLE"
    signed=true
else
    echo "::warning::macOS signing certificate is not configured; producing an unsigned DMG"
fi

DMG="$DIST/$APP_NAME-$APP_VERSION-macos-$ARCH.dmg"

# Stage outside the workspace: hdiutil writes the image through a temporary mount and
# reports "Resource busy" when something touches the source tree mid-write (Spotlight,
# antivirus, file watchers). The workspace is where that happens most often on CI.
DMG_ROOT="${RUNNER_TEMP:-${TMPDIR:-/tmp}}/mybeautifulgit-dmg-$$"
rm -rf "$DMG_ROOT" "$DMG"
mkdir -p "$DMG_ROOT"
trap 'rm -rf "$DMG_ROOT"' EXIT
ditto "$APP_BUNDLE" "$DMG_ROOT/$APP_NAME.app"
ln -s /Applications "$DMG_ROOT/Applications"

# A previous run can leave a volume with this name mounted, which also blocks create.
if [[ -d "/Volumes/$APP_NAME" ]]; then
    hdiutil detach "/Volumes/$APP_NAME" -force >/dev/null 2>&1 || true
    sleep 1
fi

created=false
for attempt in 1 2 3; do
    if hdiutil create -volname "$APP_NAME" -srcfolder "$DMG_ROOT" -ov -format UDZO "$DMG"; then
        created=true
        break
    fi
    echo "::warning::hdiutil create failed on attempt $attempt; retrying" >&2
    rm -f "$DMG"
    sleep 3
done
if [[ "$created" != true ]]; then
    echo "hdiutil could not create $DMG" >&2
    hdiutil info >&2 || true
    exit 1
fi

if [[ "$signed" == true ]]; then
    codesign --force --timestamp --sign "$IDENTITY" "$DMG"
fi

if [[ "$signed" == true && -n "${APPLE_ID:-}" && -n "${APPLE_APP_PASSWORD:-}" && -n "${APPLE_TEAM_ID:-}" ]]; then
    xcrun notarytool submit "$DMG" \
        --apple-id "$APPLE_ID" \
        --password "$APPLE_APP_PASSWORD" \
        --team-id "$APPLE_TEAM_ID" \
        --wait
    xcrun stapler staple "$DMG"
    xcrun stapler validate "$DMG"
    notarized=true
elif [[ "$signed" == true ]]; then
    echo "::warning::Apple notarization credentials are not configured; DMG is signed but not notarized"
fi

cat > "$DIST/PACKAGE-STATUS.txt" <<EOF
app: $APP_NAME
version: $APP_VERSION
package_version: $PACKAGE_VERSION
channel: $CHANNEL
platform: macOS $ARCH
signed: $signed
notarized: $notarized
EOF

(
    cd "$DIST"
    find . -maxdepth 1 -type f ! -name SHA256SUMS -print | sed 's#^./##' | sort |
        while IFS= read -r file; do
            printf '%s  %s\n' "$(shasum -a 256 "$file" | awk '{print $1}')" "$file"
        done > SHA256SUMS
)

echo "Built $APP_NAME $APP_VERSION ($CHANNEL), signed=$signed notarized=$notarized"
ls -lh "$DIST"
