#!/usr/bin/env bash
set -euo pipefail

raw="${INPUT_VERSION:-${GITHUB_REF_NAME:-0.1.0-dev}}"
raw="${raw#v}"
raw="${raw//[[:space:]]/}"
if [[ ! "$raw" =~ ^[0-9] ]]; then
    raw="0.1.0-dev"
fi

base="${raw%%-*}"
base="${base%%+*}"
if [[ ! "$base" =~ ^[0-9]+(\.[0-9]+){0,2}$ ]]; then
    base="0.1.0"
fi
case "$base" in
    *.*.*) ;;
    *.*) base="$base.0" ;;
    *) base="$base.0.0" ;;
esac

channel="chan"
case "$raw" in
    *-kun*|*-beta*) channel="kun" ;;
esac

if [[ -n "${GITHUB_ENV:-}" ]]; then
    {
        echo "APP_VERSION=$raw"
        echo "PACKAGE_VERSION=$base"
        echo "RELEASE_CHANNEL=$channel"
    } >> "$GITHUB_ENV"
else
    printf 'APP_VERSION=%s\nPACKAGE_VERSION=%s\nRELEASE_CHANNEL=%s\n' \
        "$raw" "$base" "$channel"
fi
