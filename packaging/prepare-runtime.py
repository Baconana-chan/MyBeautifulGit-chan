#!/usr/bin/env python3
"""Prepare a flat jpackage input directory from Amper's executable jar.

Amper's executable-jar packaging currently selects the empty
``org.jetbrains.compose.runtime`` / ``org.jetbrains.androidx.lifecycle`` metadata
wrappers when two artifacts have the same file name.  The real implementations
live in the corresponding ``androidx`` artifacts, so a Spring Boot launcher
cannot load classes such as ``androidx.compose.runtime.Composer``.

jpackage can launch a flat classpath directly.  Extract ``BOOT-INF/lib/*.jar``
from the executable jar, then replace the empty wrappers with the real jars from
Google Maven.  The application is launched through ``desktopApp-jvm.jar`` and
the module's main class, not through the Spring Boot launcher.

The versions below must match the Compose/lifecycle versions resolved by Amper.
If those versions change, update this list (including the SHA-256 values).
"""

from __future__ import annotations

import argparse
import hashlib
import shutil
import sys
import time
import urllib.request
import zipfile
from pathlib import Path

GOOGLE_MAVEN = "https://dl.google.com/dl/android/maven2/"


class Artifact:
    def __init__(self, filename: str, repository_path: str, sha256: str, class_prefix: str) -> None:
        self.filename = filename
        self.repository_path = repository_path
        self.sha256 = sha256
        self.class_prefix = class_prefix


# These are the real AndroidX implementations behind the JetBrains wrappers.
# The wrapper jars have the same file names but contain no classes.
ARTIFACTS = [
    Artifact(
        "runtime-desktop-1.12.1.jar",
        "androidx/compose/runtime/runtime-desktop/1.12.1/runtime-desktop-1.12.1.jar",
        "ed390d7bf7bdba951359d127f4c1ddd223f535f5a6a63078c5c5ac6b6924eb08",
        "androidx/compose/runtime/",
    ),
    Artifact(
        "runtime-saveable-desktop-1.12.1.jar",
        "androidx/compose/runtime/runtime-saveable-desktop/1.12.1/runtime-saveable-desktop-1.12.1.jar",
        "069949667409ab6de21053f9fdbff0869ae2a1f70a7212cdb89e7d409fd2552c",
        "androidx/compose/runtime/saveable/",
    ),
    Artifact(
        "lifecycle-common-jvm-2.11.0.jar",
        "androidx/lifecycle/lifecycle-common-jvm/2.11.0/lifecycle-common-jvm-2.11.0.jar",
        "e6a66e67b9f63a0fb6344f165c1d872376e5fe6d235bffb138af45122cf452a9",
        "androidx/lifecycle/",
    ),
    Artifact(
        "lifecycle-runtime-desktop-2.11.0.jar",
        "androidx/lifecycle/lifecycle-runtime-desktop/2.11.0/lifecycle-runtime-desktop-2.11.0.jar",
        "09f75a4a8d763cee5d6af36f422139a0205d3f6df2e2c5288cae42836f0b36ea",
        "androidx/lifecycle/",
    ),
    Artifact(
        "lifecycle-viewmodel-desktop-2.11.0.jar",
        "androidx/lifecycle/lifecycle-viewmodel-desktop/2.11.0/lifecycle-viewmodel-desktop-2.11.0.jar",
        "175c349266ccbb6a2036615725b7066ee3f3319665c98af42206ab3a582a43c3",
        "androidx/lifecycle/",
    ),
    Artifact(
        "lifecycle-runtime-compose-desktop-2.11.0.jar",
        "androidx/lifecycle/lifecycle-runtime-compose-desktop/2.11.0/lifecycle-runtime-compose-desktop-2.11.0.jar",
        "d5336753ea4d5881ccfb8b0613e034eb241f0d8b4349536df1cd0ec86cf63664",
        "androidx/lifecycle/compose/",
    ),
    Artifact(
        "lifecycle-viewmodel-compose-desktop-2.11.0.jar",
        "androidx/lifecycle/lifecycle-viewmodel-compose-desktop/2.11.0/lifecycle-viewmodel-compose-desktop-2.11.0.jar",
        "1b4c58125c7b3693444f40c8ab76881d17fa74726860c24872a3dec3e0fad34b",
        "androidx/lifecycle/viewmodel/compose/",
    ),
]


def file_sha256(path: Path) -> str:
    digest = hashlib.sha256()
    with path.open("rb") as stream:
        for chunk in iter(lambda: stream.read(1024 * 1024), b""):
            digest.update(chunk)
    return digest.hexdigest()


def has_classes(path: Path, prefix: str) -> bool:
    try:
        with zipfile.ZipFile(path) as archive:
            return any(
                name.startswith(prefix) and name.endswith(".class")
                for name in archive.namelist()
            )
    except (OSError, zipfile.BadZipFile):
        return False


def download(url: str, destination: Path) -> None:
    last_error: Exception | None = None
    for attempt in range(1, 4):
        try:
            with urllib.request.urlopen(url, timeout=60) as response:
                data = response.read()
            destination.write_bytes(data)
            return
        except Exception as error:  # noqa: BLE001 - retry any transient download failure
            last_error = error
            if attempt < 3:
                print(f"download failed (attempt {attempt}/3), retrying: {error}", file=sys.stderr)
                time.sleep(attempt * 2)
    raise RuntimeError(f"could not download {url}: {last_error}")


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--jar", required=True, type=Path, help="Amper executable jar")
    parser.add_argument("--output", required=True, type=Path, help="flat jpackage input directory")
    args = parser.parse_args()

    jar = args.jar.resolve()
    output = args.output.resolve()
    if not jar.is_file():
        print(f"executable jar not found: {jar}", file=sys.stderr)
        return 1

    if output.exists():
        shutil.rmtree(output)
    output.mkdir(parents=True)

    with zipfile.ZipFile(jar) as archive:
        nested = [
            name
            for name in archive.namelist()
            if name.startswith("BOOT-INF/lib/") and name.endswith(".jar")
        ]
        if not nested:
            print(f"no BOOT-INF/lib jars found in {jar}", file=sys.stderr)
            return 1
        for name in nested:
            target = output / Path(name).name
            target.write_bytes(archive.read(name))

    for required in ("desktopApp-jvm.jar", "shared-jvm.jar"):
        if not (output / required).is_file():
            print(f"expected {required} in the Amper executable jar", file=sys.stderr)
            return 1

    replaced = 0
    for artifact in ARTIFACTS:
        target = output / artifact.filename
        if not target.is_file():
            print(
                f"expected {artifact.filename} in the executable jar; "
                "update packaging/prepare-runtime.py for the new Compose/lifecycle version",
                file=sys.stderr,
            )
            return 1
        if has_classes(target, artifact.class_prefix):
            print(f"keeping real classes from packaged {artifact.filename}")
            continue

        url = GOOGLE_MAVEN + artifact.repository_path
        downloaded = output / (artifact.filename + ".download")
        print(f"replacing empty wrapper {artifact.filename}")
        try:
            download(url, downloaded)
        except RuntimeError as error:
            print(error, file=sys.stderr)
            return 1

        actual_hash = file_sha256(downloaded)
        if actual_hash != artifact.sha256:
            downloaded.unlink(missing_ok=True)
            print(
                f"SHA-256 mismatch for {artifact.filename}: "
                f"expected {artifact.sha256}, got {actual_hash}",
                file=sys.stderr,
            )
            return 1
        if not has_classes(downloaded, artifact.class_prefix):
            downloaded.unlink(missing_ok=True)
            print(f"downloaded {artifact.filename} contains no expected classes", file=sys.stderr)
            return 1
        downloaded.replace(target)
        replaced += 1

    print(f"prepared {output} ({len(nested)} jars, replaced {replaced} wrappers)")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
