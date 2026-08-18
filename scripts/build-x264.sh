#!/usr/bin/env bash
set -euo pipefail

# ── Defaults ────────────────────────────────────────────────────────────────

PLATFORM="${PLATFORM:-android}"
ARCH="${ARCH:-aarch64}"
API_LEVEL="${API_LEVEL:-24}"
MIN_IOS="${MIN_IOS:-15.0}"

JOBS="${JOBS:-$(sysctl -n hw.ncpu 2>/dev/null || nproc 2>/dev/null || echo 4)}"

: "${TOOLCHAIN:?TOOLCHAIN env var must be set}"

# ── Platform configuration ─────────────────────────────────────────────────

case "$PLATFORM" in

    android)
        TRIPLE="${ARCH}-linux-android${API_LEVEL}"
        export CC="$TOOLCHAIN/${TRIPLE}-clang"

        case "$ARCH" in
            aarch64)
                ANDROID_ABI="arm64-v8a"
                HOST="aarch64-linux-android"
                ;;
            x86_64)
                ANDROID_ABI="x86_64"
                HOST="x86_64-linux-android"
                ;;
            *)
                echo "Unsupported Android architecture: $ARCH"
                exit 1
                ;;
        esac

        PREFIX="$PWD/native/x264/android/$ANDROID_ABI"

        CONFIGURE_EXTRA=(
            --sysroot=""
            # FIX: cross-prefix is correct for Android (llvm-ar, llvm-ranlib, etc.)
            --cross-prefix="$TOOLCHAIN/llvm-"
        )
        ;;

    darwin)
        : "${SDK:?SDK must be set for Darwin (iphoneos or iphonesimulator)}"
        SDK_PATH="$(xcrun --sdk "$SDK" --show-sdk-path)"

        case "$ARCH" in
            arm64)  HOST="aarch64-apple-darwin"  ;;
            x86_64) HOST="x86_64-apple-darwin"   ;;
            *)
                echo "Unsupported Darwin architecture: $ARCH"
                exit 1
                ;;
        esac

        export CC="$(xcrun --sdk "$SDK" --find clang)"
        export AS="$(xcrun --sdk "$SDK" --find clang)"

        PREFIX="$PWD/native/x264/darwin/${SDK}/${ARCH}"

        if [[ "$SDK" == "iphoneos" ]]; then
            MIN_FLAG="-mios-version-min=${MIN_IOS}"
        else
            MIN_FLAG="-mios-simulator-version-min=${MIN_IOS}"
        fi

        BASE_FLAGS="-arch $ARCH -isysroot $SDK_PATH $MIN_FLAG"
        export ASFLAGS="$BASE_FLAGS"

        CONFIGURE_EXTRA=(
            --sysroot="$SDK_PATH"
            --extra-cflags="$BASE_FLAGS"
            --extra-ldflags="$BASE_FLAGS"
            --extra-asflags="$BASE_FLAGS -arch $ARCH"
            --as="$AS"
            --disable-svink

            # FIX: Do NOT set --cross-prefix for Darwin — xcrun tools are used
            # directly via CC/AS exports above. Setting --cross-prefix causes
            # x264's configure to look for "${cross-prefix}pkg-config" (e.g.
            # "$TOOLCHAIN/llvm-pkg-config") which doesn't exist, producing:
            #   ERROR: x264 not found using pkg-config
            # Instead, point --pkg-config at the real binary explicitly.
            --pkg-config="$(command -v pkg-config)"
            --pkg-config-flags="--static"
        )
        ;;

    *)
        echo "Unsupported PLATFORM: $PLATFORM"
        echo "Supported platforms: android, darwin"
        exit 1
        ;;
esac

# ── Clean previous build ────────────────────────────────────────────────────

rm -rf "$PREFIX"

# ── Configure ────────────────────────────────────────────────────────────────

cd third-party/x264

make distclean || true

./configure \
    --prefix="$PREFIX" \
    --host="$HOST" \
    --enable-static \
    --enable-pic \
    --disable-cli \
    "${CONFIGURE_EXTRA[@]}"

# ── Build ────────────────────────────────────────────────────────────────────

make -j"$JOBS"
make install

rm -rf "$PREFIX/share"

echo
echo "========================================"
echo "x264 build completed"
echo "Platform : $PLATFORM"
echo "Arch     : $ARCH"
echo "Output   : $PREFIX"
echo "========================================"