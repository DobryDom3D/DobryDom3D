#!/usr/bin/env bash
# Builds the YafaRay core + plugins and the DobryDom3D JNI library.
#
# Usage: scripts/build-yafaray.sh <libYafaRay checkout> <output dir>
#
# The libYafaRay checkout is DobryDom3D/libYafaRay (branch dobrydom3d). The
# output dir receives libyafaray_v3_core, libyafarayjni and yafaray-plugins/,
# i.e. the layout of lib/yafaray/<os>/<arch>/. Needs JAVA_HOME (for jni.h),
# cmake and a C++11 compiler. Currently implements Linux only.
set -euo pipefail

SRC="$(cd "${1:?libYafaRay checkout}" && pwd)"
OUT="$(mkdir -p "${2:?output dir}" && cd "$2" && pwd)"
ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
: "${JAVA_HOME:?JAVA_HOME must point to a JDK (jni.h)}"
BUILD="${BUILD_DIR:-$(mktemp -d)}"

case "$(uname -s)" in
  Linux) PLATFORM=linux; JNI_PLATFORM_INCLUDE=linux; CORE=libyafaray_v3_core.so ;;
  *) echo "Unsupported platform: $(uname -s)" >&2; exit 1 ;;
esac

# Optional features off, as documented in YafarayRenderer.java. Python
# bindings are off too (the JNI library does not use them). The
# CMAKE_POLICY_VERSION_MINIMUM flag lets CMake >= 4 accept the old
# cmake_minimum_required in libYafaRay.
cmake -S "$SRC" -B "$BUILD" \
  -DCMAKE_BUILD_TYPE=Release \
  -DCMAKE_POLICY_VERSION_MINIMUM=3.5 \
  -DWITH_Freetype=OFF -DWITH_OpenEXR=OFF -DWITH_JPEG=OFF -DWITH_PNG=OFF \
  -DWITH_TIFF=OFF -DWITH_XMLImport=OFF -DWITH_XML_LOADER=OFF -DWITH_OpenCV=OFF \
  -DWITH_QT=OFF -DWITH_YAF_PY_BINDINGS=OFF -DWITH_YAF_RUBY_BINDINGS=OFF
cmake --build "$BUILD" --parallel

echo "--- build tree"
find "$BUILD" \( -name "*.so" -o -name "*.dylib" -o -name "*.dll" \) | sort

core="$(find "$BUILD" -name "$CORE" | head -n 1)"
[ -n "$core" ] || { echo "$CORE not found in build tree" >&2; exit 1; }
rm -rf "$OUT"/*
cp "$core" "$OUT/"
mkdir -p "$OUT/yafaray-plugins"
# Plugins are built next to their sources (src/<module>/lib*.so). The core and
# libyafaray_v3_plugin (the interface library used by script bindings) are not
# plugins of the JNI renderer.
find "$BUILD/src" -name "lib*.so" ! -name "$CORE" ! -name "libyafaray_v3_plugin.so" \
  -exec cp {} "$OUT/yafaray-plugins/" \;
[ -n "$(ls -A "$OUT/yafaray-plugins")" ] || { echo "no plugins found" >&2; exit 1; }
# CMake leaves a RUNPATH of empty entries (which means the current directory);
# replace it with the same relative search path as the committed libraries.
if command -v patchelf >/dev/null 2>&1; then
  patchelf --set-rpath '$ORIGIN' "$OUT/$CORE"
  for f in "$OUT"/yafaray-plugins/*.so; do patchelf --set-rpath '$ORIGIN:$ORIGIN/..' "$f"; done
else
  echo "patchelf is required to fix the RUNPATH" >&2; exit 1
fi

# Generate the JNI header (YafarayRenderer.h is not checked in). javac -h
# replaces the old javah; the source path pulls in the classes it depends on.
JNI_GEN="$(mktemp -d)"
(cd "$ROOT" && "$JAVA_HOME/bin/javac" -proc:none -encoding ISO-8859-1 \
  -h "$JNI_GEN" -d "$JNI_GEN/classes" -sourcepath src \
  -cp "lib/*:lib/java3d-1.6/*:libtest/*" \
  src/com/eteks/sweethome3d/j3d/YafarayRenderer.java)
test -f "$JNI_GEN/com_eteks_sweethome3d_j3d_YafarayRenderer.h"
cp "$JNI_GEN/com_eteks_sweethome3d_j3d_YafarayRenderer.h" "$JNI_GEN/YafarayRenderer.h"

# JNI glue, linked against the core library
g++ -fPIC -std=c++11 -O2 \
  -I"$JAVA_HOME/include" -I"$JAVA_HOME/include/$JNI_PLATFORM_INCLUDE" \
  -I"$SRC/include" -I"$JNI_GEN" \
  "$ROOT/src/com/eteks/sweethome3d/j3d/YafarayRenderer.cpp" \
  -shared -o "$OUT/libyafarayjni.so" \
  -L"$OUT" -lyafaray_v3_core -Wl,-rpath,'$ORIGIN'

echo "--- output"
ls -lR "$OUT" | head -80
