#!/usr/bin/env bash
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
APP_NAME="MyBeautifulGit-chan"
LINUX_PACKAGE_NAME="mybeautifulgit-chan"
APP_VERSION="${APP_VERSION:-0.1.0-dev}"
PACKAGE_VERSION="${PACKAGE_VERSION:-0.1.0}"
CHANNEL="${RELEASE_CHANNEL:-chan}"
ARCH="${PACKAGE_ARCH:-$(uname -m)}"
case "$ARCH" in
    x86_64|amd64) ARCH="x86_64" ;;
    aarch64|arm64) ARCH="aarch64" ;;
    *) echo "Unsupported Linux architecture: $ARCH" >&2; exit 1 ;;
esac

DIST="$ROOT/dist"
BUILD_DIR="$ROOT/build/packaging"
INPUT="$BUILD_DIR/input"
JAR="$ROOT/build/tasks/_desktopApp_executableJarJvm/desktopApp-jvm-executable.jar"
JPACKAGE="${JAVA_HOME:+$JAVA_HOME/bin/}jpackage"

for tool in dpkg-deb fakeroot rpmbuild sha256sum; do
    if ! command -v "$tool" >/dev/null 2>&1; then
        echo "Missing packaging tool: $tool" >&2
        exit 1
    fi
done

PYTHON="${PYTHON:-}"
if [[ -z "$PYTHON" ]]; then
    PYTHON="$(command -v python3 || command -v python || true)"
fi
[[ -n "$PYTHON" ]] || { echo "Python 3 is required to prepare the jpackage input" >&2; exit 1; }

rm -rf "$DIST"
mkdir -p "$DIST"
"$ROOT/kotlin" package -m desktopApp -f executable-jar
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
    --description "Terminal-flavored Git client"
    --copyright "GPL-2.0-only"
    --input "$INPUT"
    --main-jar "desktopApp-jvm.jar"
    --main-class "org.baconana.mybeautifulgit_chan.MainKt"
    --dest "$DIST"
    --java-options "-Dfile.encoding=UTF-8"
    --java-options "-Xmx2048m"
    --add-modules "$MODULES"
)
if [[ -f "$ROOT/packaging/icon.png" ]]; then
    common+=(--icon "$ROOT/packaging/icon.png")
fi
"$JPACKAGE" "${common[@]}"

APP_IMAGE="$DIST/$APP_NAME"
if [[ ! -d "$APP_IMAGE" ]]; then
    echo "jpackage did not produce a Linux app image" >&2
    exit 1
fi

linux_common=(
    --app-image "$APP_IMAGE"
    --name "$APP_NAME"
    --app-version "$PACKAGE_VERSION"
    --vendor "Baconana-chan"
    --description "Terminal-flavored Git client"
    --copyright "GPL-2.0-only"
    --dest "$DIST"
    --linux-package-name "$LINUX_PACKAGE_NAME"
    --linux-menu-group "Development"
    --linux-app-category "Development"
    --linux-shortcut
)
if [[ -f "$ROOT/packaging/icon.png" ]]; then
    linux_common+=(--icon "$ROOT/packaging/icon.png")
fi
for format in deb rpm; do
    "$JPACKAGE" --type "$format" "${linux_common[@]}"
done

APPDIR="$BUILD_DIR/AppDir"
rm -rf "$APPDIR"
mkdir -p "$APPDIR/usr"
cp -a "$APP_IMAGE/." "$APPDIR/usr/"
cp "$ROOT/packaging/icon.png" "$APPDIR/$LINUX_PACKAGE_NAME.png"
cp "$ROOT/packaging/icon.png" "$APPDIR/.DirIcon"
cat > "$APPDIR/AppRun" <<EOF
#!/bin/sh
HERE="\$(dirname "\$(readlink -f "\$0")")"
exec "\$HERE/usr/bin/$APP_NAME" "\$@"
EOF
chmod 755 "$APPDIR/AppRun"
cat > "$APPDIR/$LINUX_PACKAGE_NAME.desktop" <<EOF
[Desktop Entry]
Name=$APP_NAME
Comment=Terminal-flavored Git client
Exec=$APP_NAME
Icon=$LINUX_PACKAGE_NAME
Terminal=false
Type=Application
Categories=Development;Utility;
EOF
chmod -R a+rX "$APPDIR"

APPIMAGETOOL="${APPIMAGETOOL:-}"
if [[ -z "$APPIMAGETOOL" ]]; then
    APPIMAGETOOL="$BUILD_DIR/appimagetool-x86_64.AppImage"
    curl -fL --retry 3 \
        "https://github.com/AppImage/appimagetool/releases/download/continuous/appimagetool-x86_64.AppImage" \
        -o "$APPIMAGETOOL"
    chmod +x "$APPIMAGETOOL"
fi
APPIMAGE="$DIST/$APP_NAME-$APP_VERSION-linux-$ARCH.AppImage"
if [[ "$APPIMAGETOOL" == *.AppImage ]]; then
    ARCH="$ARCH" "$APPIMAGETOOL" --appimage-extract-and-run "$APPDIR" "$APPIMAGE"
else
    ARCH="$ARCH" "$APPIMAGETOOL" "$APPDIR" "$APPIMAGE"
fi

cat > "$DIST/PACKAGE-STATUS.txt" <<EOF
app: $APP_NAME
version: $APP_VERSION
package_version: $PACKAGE_VERSION
channel: $CHANNEL
platform: Linux $ARCH
formats: deb, rpm, AppImage
EOF

(
    cd "$DIST"
    find . -maxdepth 1 -type f ! -name SHA256SUMS -print | sed 's#^./##' | sort |
        while IFS= read -r file; do
            printf '%s  %s\n' "$(sha256sum "$file" | awk '{print $1}')" "$file"
        done > SHA256SUMS
)

echo "Built $APP_NAME $APP_VERSION ($CHANNEL) for Linux $ARCH"
ls -lh "$DIST"
