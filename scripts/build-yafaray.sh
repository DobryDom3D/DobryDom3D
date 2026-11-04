#!/usr/bin/env bash
# Builds the YafaRay core + plugins and the DobryDom3D JNI library.
#
# Usage: scripts/build-yafaray.sh <libYafaRay checkout> <output dir>
#
# The libYafaRay checkout is DobryDom3D/libYafaRay (branch dobrydom3d). The
# output dir receives libyafaray_v3_core, libyafarayjni and yafaray-plugins/,
# i.e. the layout of lib/yafaray/<os>/<arch>/. Needs JAVA_HOME (for jni.h),
# cmake and a C++11 compiler. Implements Linux and macOS; on macOS set
# ARCH=arm64 or ARCH=x86_64 (default: the host architecture). Windows is built
# by a separate MSYS2 job.
set -euo pipefail

SRC="$(cd "${1:?libYafaRay checkout}" && pwd)"
OUT="$(mkdir -p "${2:?output dir}" && cd "$2" && pwd)"
ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
: "${JAVA_HOME:?JAVA_HOME must point to a JDK (jni.h)}"
BUILD="${BUILD_DIR:-$(mktemp -d)}"

CMAKE_PLATFORM_ARGS=()
case "$(uname -s)" in
  Linux)
    JNI_PLATFORM_INCLUDE=linux; EXT=so
    ;;
  Darwin)
    JNI_PLATFORM_INCLUDE=darwin; EXT=dylib
    ARCH="${ARCH:-$(uname -m)}"
    DEPLOYMENT_TARGET="${MACOSX_DEPLOYMENT_TARGET:-11.0}"
    # libYafaRay hardcodes the MacOSX10.11 SDK unless told otherwise
    CMAKE_PLATFORM_ARGS=(
      -DCMAKE_OSX_ARCHITECTURES="$ARCH"
      -DCMAKE_OSX_DEPLOYMENT_TARGET="$DEPLOYMENT_TARGET"
      -DYAF_CMAKE_OSX_SYSROOT="$(xcrun --show-sdk-path)"
    )
    ;;
  *) echo "Unsupported platform: $(uname -s)" >&2; exit 1 ;;
esac
CORE="libyafaray_v3_core.$EXT"

# Optional features off, as documented in YafarayRenderer.java. Python
# bindings are off too (the JNI library does not use them). The
# CMAKE_POLICY_VERSION_MINIMUM flag lets CMake >= 4 accept the old
# cmake_minimum_required in libYafaRay.
cmake -S "$SRC" -B "$BUILD" \
  -DCMAKE_BUILD_TYPE=Release \
  -DCMAKE_POLICY_VERSION_MINIMUM=3.5 \
  -DWITH_Freetype=OFF -DWITH_OpenEXR=OFF -DWITH_JPEG=OFF -DWITH_PNG=OFF \
  -DWITH_TIFF=OFF -DWITH_XMLImport=OFF -DWITH_XML_LOADER=OFF -DWITH_OpenCV=OFF \
  -DWITH_QT=OFF -DWITH_YAF_PY_BINDINGS=OFF -DWITH_YAF_RUBY_BINDINGS=OFF \
  ${CMAKE_PLATFORM_ARGS[@]+"${CMAKE_PLATFORM_ARGS[@]}"}
cmake --build "$BUILD" --parallel

echo "--- build tree"
find "$BUILD" -name "*.$EXT" | sort

core="$(find "$BUILD" -name "$CORE" | head -n 1)"
[ -n "$core" ] || { echo "$CORE not found in build tree" >&2; exit 1; }
rm -rf "$OUT"/*
cp "$core" "$OUT/"
mkdir -p "$OUT/yafaray-plugins"
# Plugins are built next to their sources (src/<module>/lib*.<ext>). The core and
# libyafaray_v3_plugin (the interface library used by script bindings) are not
# plugins of the JNI renderer.
find "$BUILD/src" -name "lib*.$EXT" ! -name "$CORE" ! -name "libyafaray_v3_plugin.$EXT" \
  -exec cp {} "$OUT/yafaray-plugins/" \;
[ -n "$(ls -A "$OUT/yafaray-plugins")" ] || { echo "no plugins found" >&2; exit 1; }

# CMake leaves absolute build-tree search paths (Linux: even empty entries,
# which mean the current directory); replace them with relative ones.
if [ "$EXT" = so ]; then
  command -v patchelf >/dev/null || { echo "patchelf is required to fix the RUNPATH" >&2; exit 1; }
  patchelf --set-rpath '$ORIGIN' "$OUT/$CORE"
  for f in "$OUT"/yafaray-plugins/*.so; do patchelf --set-rpath '$ORIGIN:$ORIGIN/..' "$f"; done
else
  reset_rpaths() {  # file rpath...
    local f="$1"; shift
    otool -l "$f" | awk '/cmd LC_RPATH/{r=1} r&&/ path /{print $2; r=0}' | while read -r p; do
      install_name_tool -delete_rpath "$p" "$f"
    done
    for p in "$@"; do install_name_tool -add_rpath "$p" "$f"; done
  }
  reset_rpaths "$OUT/$CORE" @loader_path
  for f in "$OUT"/yafaray-plugins/*.dylib; do reset_rpaths "$f" @loader_path @loader_path/..; done
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
JNI_SRC="$ROOT/src/com/eteks/sweethome3d/j3d/YafarayRenderer.cpp"
JNI_COMMON=(-std=c++11 -O2
  -I"$JAVA_HOME/include" -I"$JAVA_HOME/include/$JNI_PLATFORM_INCLUDE"
  -I"$SRC/include" -I"$JNI_GEN")
if [ "$EXT" = so ]; then
  g++ -fPIC "${JNI_COMMON[@]}" "$JNI_SRC" -shared -o "$OUT/libyafarayjni.so" \
    -L"$OUT" -lyafaray_v3_core -Wl,-rpath,'$ORIGIN'
else
  clang++ -arch "$ARCH" -mmacosx-version-min="$DEPLOYMENT_TARGET" "${JNI_COMMON[@]}" \
    "$JNI_SRC" -dynamiclib -o "$OUT/libyafarayjni.dylib" \
    -L"$OUT" -lyafaray_v3_core -Wl,-rpath,@loader_path
fi

echo "--- output"
ls -lR "$OUT" | head -80
