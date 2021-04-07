#!/usr/bin/env bash

NDK_PATH=~/Documents/Dev/android-sdk-macosx/ndk-bundle
HOST_PLATFORM_WIN=darwin-x86_64
HOST_PLATFORM=$HOST_PLATFORM_WIN
API=29

TOOLCHAINS="$NDK_PATH/toolchains/llvm/prebuilt/$HOST_PLATFORM"
SYSROOT="$NDK_PATH/toolchains/llvm/prebuilt/$HOST_PLATFORM/sysroot"
CFLAG="-D__ANDROID_API__=$API -Os -fPIC -DANDROID "
LDFLAG="-lc -lm -ldl -llog "

PREFIX=android-build

CONFIG_LOG_PATH=${PREFIX}/log
COMMON_OPTIONS=
CONFIGURATION=

build() {
  APP_ABI=$1
  echo "======== > Start build $APP_ABI"
  case ${APP_ABI} in
  armeabi-v7a)
    ARCH="arm"
    CPU="armv7-a"
    MARCH="armv7-a"
    TARGET=armv7a-linux-androideabi
    CC="$TOOLCHAINS/bin/$TARGET$API-clang"
    CXX="$TOOLCHAINS/bin/$TARGET$API-clang++"
    LD="$TOOLCHAINS/bin/$TARGET$API-clang"
    CROSS_PREFIX="$TOOLCHAINS/bin/arm-linux-androideabi-"
    EXTRA_CFLAGS="$CFLAG -mfloat-abi=softfp -mfpu=vfp -marm -march=$MARCH "
    EXTRA_LDFLAGS="$LDFLAG"
    EXTRA_OPTIONS="--enable-neon --cpu=$CPU "
    ;;
  arm64-v8a)
    ARCH="aarch64"
    TARGET=$ARCH-linux-android
    CC="$TOOLCHAINS/bin/$TARGET$API-clang"
    CXX="$TOOLCHAINS/bin/$TARGET$API-clang++"
    LD="$TOOLCHAINS/bin/$TARGET$API-clang"
    CROSS_PREFIX="$TOOLCHAINS/bin/$TARGET-"
    EXTRA_CFLAGS="$CFLAG"
    EXTRA_LDFLAGS="$LDFLAG"
    EXTRA_OPTIONS=""
    ;;
  x86)
    ARCH="x86"
    CPU="i686"
    MARCH="i686"
    TARGET=i686-linux-android
    CC="$TOOLCHAINS/bin/$TARGET$API-clang"
    CXX="$TOOLCHAINS/bin/$TARGET$API-clang++"
    LD="$TOOLCHAINS/bin/$TARGET$API-clang"
    CROSS_PREFIX="$TOOLCHAINS/bin/$TARGET-"
    EXTRA_CFLAGS="$CFLAG -march=$MARCH -mtune=intel -mssse3 -mfpmath=sse -m32"
    EXTRA_LDFLAGS="$LDFLAG"
    EXTRA_OPTIONS="--cpu=$CPU "
    ;;
  x86_64)
    ARCH="x86_64"
    CPU="x86-64"
    MARCH="x86_64"
    TARGET=$ARCH-linux-android
    CC="$TOOLCHAINS/bin/$TARGET$API-clang"
    CXX="$TOOLCHAINS/bin/$TARGET$API-clang++"
    LD="$TOOLCHAINS/bin/$TARGET$API-clang"
    CROSS_PREFIX="$TOOLCHAINS/bin/$TARGET-"
    EXTRA_CFLAGS="$CFLAG -march=$CPU -mtune=intel -msse4.2 -mpopcnt -m64"
    EXTRA_LDFLAGS="$LDFLAG"
    EXTRA_OPTIONS="--cpu=$CPU "
    ;;
  esac

  echo "-------- > Start clean workspace"
  make clean

  echo "-------- > Start build configuration"
  CONFIGURATION="$COMMON_OPTIONS"
  CONFIGURATION="$CONFIGURATION --logfile=$CONFIG_LOG_PATH/config_$APP_ABI.log"
  CONFIGURATION="$CONFIGURATION --prefix=$PREFIX"
  CONFIGURATION="$CONFIGURATION --libdir=$PREFIX/libs/$APP_ABI"
  CONFIGURATION="$CONFIGURATION --incdir=$PREFIX/includes/$APP_ABI"
  CONFIGURATION="$CONFIGURATION --pkgconfigdir=$PREFIX/pkgconfig/$APP_ABI"
  CONFIGURATION="$CONFIGURATION --cross-prefix=$CROSS_PREFIX"
  CONFIGURATION="$CONFIGURATION --arch=$ARCH"
  CONFIGURATION="$CONFIGURATION --sysroot=$SYSROOT"
  CONFIGURATION="$CONFIGURATION --cc=$CC"
  CONFIGURATION="$CONFIGURATION --cxx=$CXX"
  CONFIGURATION="$CONFIGURATION --ld=$LD"
  CONFIGURATION="$CONFIGURATION $EXTRA_OPTIONS"

  echo "-------- > Start config makefile with $CONFIGURATION --extra-cflags=${EXTRA_CFLAGS} --extra-ldflags=${EXTRA_LDFLAGS}"
  ./configure ${CONFIGURATION} \
  --extra-cflags="$EXTRA_CFLAGS" \
  --extra-ldflags="$EXTRA_LDFLAGS"

  echo "-------- > Start make $APP_ABI with -j8"
  make -j10

  echo "-------- > Start install $APP_ABI"
  make install
  echo "++++++++ > make and install $APP_ABI complete."

}

build_all() {

  COMMON_OPTIONS="$COMMON_OPTIONS --target-os=android"
  # --disable-static 与 --enable-shared成对使用
  # static 表示生产.a静态库 shared表示生成.so动态库
  COMMON_OPTIONS="$COMMON_OPTIONS --disable-static"
  COMMON_OPTIONS="$COMMON_OPTIONS --enable-shared"
  # COMMON_OPTIONS="$COMMON_OPTIONS --enable-protocols"
  # COMMON_OPTIONS="$COMMON_OPTIONS --disable-protocols"
  COMMON_OPTIONS="$COMMON_OPTIONS --enable-cross-compile"
  COMMON_OPTIONS="$COMMON_OPTIONS --enable-optimizations"
  COMMON_OPTIONS="$COMMON_OPTIONS --disable-debug"
  COMMON_OPTIONS="$COMMON_OPTIONS --enable-small"
  COMMON_OPTIONS="$COMMON_OPTIONS --disable-doc"
  COMMON_OPTIONS="$COMMON_OPTIONS --disable-programs"
  COMMON_OPTIONS="$COMMON_OPTIONS --disable-ffmpeg"
  COMMON_OPTIONS="$COMMON_OPTIONS --disable-ffplay"
  COMMON_OPTIONS="$COMMON_OPTIONS --disable-ffprobe"
  COMMON_OPTIONS="$COMMON_OPTIONS --disable-symver"
  COMMON_OPTIONS="$COMMON_OPTIONS --disable-network"
  COMMON_OPTIONS="$COMMON_OPTIONS --disable-x86asm"
  COMMON_OPTIONS="$COMMON_OPTIONS --disable-asm"

  COMMON_OPTIONS="$COMMON_OPTIONS --disable-avdevice"
  COMMON_OPTIONS="$COMMON_OPTIONS --disable-avcodec"
  COMMON_OPTIONS="$COMMON_OPTIONS --disable-avformat"
  COMMON_OPTIONS="$COMMON_OPTIONS --disable-swscale"
  COMMON_OPTIONS="$COMMON_OPTIONS --disable-avfilter"
  COMMON_OPTIONS="$COMMON_OPTIONS --enable-fast-unaligned"

  COMMON_OPTIONS="$COMMON_OPTIONS --enable-pthreads"
  # COMMON_OPTIONS="$COMMON_OPTIONS --enable-mediacodec"
  COMMON_OPTIONS="$COMMON_OPTIONS --enable-jni"
  COMMON_OPTIONS="$COMMON_OPTIONS --enable-zlib"
  COMMON_OPTIONS="$COMMON_OPTIONS --enable-pic"
  # COMMON_OPTIONS="$COMMON_OPTIONS --enable-avresample"
  # COMMON_OPTIONS="$COMMON_OPTIONS --enable-decoder=h264"
  # COMMON_OPTIONS="$COMMON_OPTIONS --enable-decoder=mpeg4"
  # COMMON_OPTIONS="$COMMON_OPTIONS --enable-decoder=mjpeg"
  # COMMON_OPTIONS="$COMMON_OPTIONS --enable-decoder=png"
  # COMMON_OPTIONS="$COMMON_OPTIONS --enable-decoder=vorbis"
  # COMMON_OPTIONS="$COMMON_OPTIONS --enable-decoder=opus"
  # COMMON_OPTIONS="$COMMON_OPTIONS --enable-decoder=flac"

  echo "COMMON_OPTIONS=$COMMON_OPTIONS"
  echo "PREFIX=$PREFIX"
  echo "CONFIG_LOG_PATH=$CONFIG_LOG_PATH"

  rm -rf ${PREFIX}
  mkdir -p ${CONFIG_LOG_PATH}

  #    build $app_abi
  build "armeabi-v7a"
  build "arm64-v8a"
  # build "x86"
  # build "x86_64"
}

echo "-------- Start --------"

build_all

echo "-------- End --------"
