#!/usr/bin/env bash

COMPILE_BUILD_TARGET="android"

for argument in "$@"; do
  case $argument in
  iOS | ios | IOS)
     COMPILE_BUILD_TARGET="iOS"
     ;;
  esac
  shift
done

echo "Compile Build Target : ${COMPILE_BUILD_TARGET}"

COMMON_COMPILE_BUILD_FLAGS=

if [ ${COMPILE_BUILD_TARGET} == android ];
then
    COMMON_COMPILE_BUILD_FLAGS="$COMMON_COMPILE_BUILD_FLAGS --target-abis=armeabi-v7a,arm64-v8a"
    COMMON_COMPILE_BUILD_FLAGS="$COMMON_COMPILE_BUILD_FLAGS --android-api-level=29"
    COMMON_COMPILE_BUILD_FLAGS="$COMMON_COMPILE_BUILD_FLAGS --enable-libmp3lame"
    COMMON_COMPILE_BUILD_FLAGS="$COMMON_COMPILE_BUILD_FLAGS --enable-libx264"
else
    COMMON_COMPILE_BUILD_FLAGS="$COMMON_COMPILE_BUILD_FLAGS --target-abis=armeabi-v7a,arm64-v8a"
fi

./ffmpeg-${COMPILE_BUILD_TARGET}-maker.sh \
            ${COMMON_COMPILE_BUILD_FLAGS}