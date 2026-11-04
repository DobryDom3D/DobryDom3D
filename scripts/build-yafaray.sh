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
find "$BUILD" -path '*yafaray-plugins*' -name "*.so" -exec cp {} "$OUT/yafaray-plugins/" \;
if [ -z "$(ls -A "$OUT/yafaray-plugins")" ]; then
  # fall back to any plugin libraries next to the core library
  find "$(dirname "$core")" -maxdepth 2 -name "lib*.so" ! -name "$CORE" \
    -exec cp {} "$OUT/yafaray-plugins/" \;
fi

# JNI glue, linked against the core library
g++ -fPIC -std=c++11 -O2 \
  -I"$JAVA_HOME/include" -I"$JAVA_HOME/include/$JNI_PLATFORM_INCLUDE" \
  -I"$SRC/include" \
  "$ROOT/src/com/eteks/sweethome3d/j3d/YafarayRenderer.cpp" \
  -shared -o "$OUT/libyafarayjni.so" \
  -L"$OUT" -lyafaray_v3_core -Wl,-rpath,'$ORIGIN'

echo "--- output"
ls -lR "$OUT" | head -80
