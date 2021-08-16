#!/usr/bin/env bash

COMPILE_BUILD_TARGET="Android"
COMPILE_LIB_FLAGS="--disable-shared --enable-static"
for argument in "$@"; do
  case $argument in
  iOS | ios | IOS)
     COMPILE_BUILD_TARGET="iOS"
     COMPILE_LIB_FLAGS="--enable-shared --disable-static"
     ;;
  esac
  shift
done

echo "Compile Build Target : ${COMPILE_BUILD_TARGET}"

COMMON_COMPILE_BUILD_FLAGS=

if [ ${COMPILE_BUILD_TARGET} == iOS ];
then
    COMMON_COMPILE_BUILD_FLAGS="$COMMON_COMPILE_BUILD_FLAGS --target-abis=arm64"
    COMMON_COMPILE_BUILD_FLAGS="$COMMON_COMPILE_BUILD_FLAGS --iOS-api-level=9.0"
else
    COMMON_COMPILE_BUILD_FLAGS="$COMMON_COMPILE_BUILD_FLAGS --target-abis=armeabi-v7a,arm64-v8a"
    COMMON_COMPILE_BUILD_FLAGS="$COMMON_COMPILE_BUILD_FLAGS --android-api-level=29"
fi

COMMON_COMPILE_BUILD_FLAGS="$COMMON_COMPILE_BUILD_FLAGS --enable-libmp3lame"
COMMON_COMPILE_BUILD_FLAGS="$COMMON_COMPILE_BUILD_FLAGS --enable-libx264"

export COMPILE_BUILD_TARGET=${COMPILE_BUILD_TARGET}
export COMPILE_LIB_FLAGS=${COMPILE_LIB_FLAGS}

./ffmpeg-maker.sh \
            ${COMMON_COMPILE_BUILD_FLAGS}