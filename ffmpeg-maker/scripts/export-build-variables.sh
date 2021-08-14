#!/usr/bin/env bash

# 每个架构构建编译参数

function max() {
  [ $1 -ge $2 ] && echo "$1" || echo "$2"
}

# 当前ANDROID/IOS架构, 如armv7(iOS) armeabi-v7a(Android)
export PLATFORM_ABI=$1

EXTRA_C_FLAGS=
EXTRA_LD_FLAGS=
EXTRA_AS_FLAGS=

if [ ${COMPILE_BUILD_TARGET} == iOS ];
then
    # 配置iOS的参数
    # 配置最小iOS版本前缀
    EXTRA_C_FLAGS="-arch ${PLATFORM_ABI}"
    if [ $PLATFORM_ABI = "i386" ] || [ $PLATFORM_ABI = "x86_64" ] ; then
        export IOS_PLATFORM="iPhoneSimulator"
        export IOS_MIOS_VERSION="-mios-simulator-version-min=${API_LEVEL}"
        if [ $PLATFORM_ABI = "x86_64" ]
        then
            HOST=x86_64-apple-darwin
        else
            HOST=i386-apple-darwin
        fi
        EXTRA_C_FLAGS="$EXTRA_C_FLAGS ${IOS_MIOS_VERSION}"
    else
        export IOS_PLATFORM="iPhoneOS"
        export IOS_MIOS_VERSION="-mios-version-min=${API_LEVEL}"
        if [ $PLATFORM_ABI == "arm64" ];
        then
            HOST=aarch64-apple-darwin
        else
            HOST=arm-apple-darwin
        fi
        EXTRA_C_FLAGS="$EXTRA_C_FLAGS -fembed-bitcode"
        EXTRA_C_FLAGS="$EXTRA_C_FLAGS ${IOS_MIOS_VERSION}"
    fi
    EXTRA_C_FLAGS="$EXTRA_C_FLAGS"
    EXTRA_LD_FLAGS="$EXTRA_C_FLAGS"
    EXTRA_AS_FLAGS="$EXTRA_C_FLAGS"
    export IOS_PLATFORM_DEVELOPER="${XCODE_DEVELOPER_HOME}/Platforms/${IOS_PLATFORM}.platform/Developer"
    export IOS_SDK_HOME="${IOS_PLATFORM_DEVELOPER}/SDKs/${IOS_PLATFORM}${IOS_SDK_VERSION}.sdk"
    export TOOLCHAIN_PATH="${XCODE_DEVELOPER_HOME}/Toolchains/XcodeDefault.xctoolchain/usr"
    export SYSROOT_PATH=${IOS_SDK_HOME}
else
    # 配置Android的参数
    if [ $PLATFORM_ABI = "arm64-v8a" ] || [ $PLATFORM_ABI = "x86_64" ] ; then
        # For 64bit we use value not less than 21
        export ANDROID_PLATFORM=$(max ${DESIRED_ANDROID_API_LEVEL} 21)
    else
        export ANDROID_PLATFORM=${DESIRED_ANDROID_API_LEVEL}
    fi

    export TOOLCHAIN_PATH=${ANDROID_NDK_HOME}/toolchains/llvm/prebuilt/${HOST_TAG}
    export SYSROOT_PATH=${TOOLCHAIN_PATH}/sysroot
fi

export EXTRA_C_FLAGS=${EXTRA_C_FLAGS}
export EXTRA_LD_FLAGS=${EXTRA_LD_FLAGS}
export EXTRA_AS_FLAGS=${EXTRA_AS_FLAGS}

echo ${EXTRA_C_FLAGS}
echo ${EXTRA_LD_FLAGS}
echo ${EXTRA_AS_FLAGS}

TARGET_TRIPLE_MACHINE_CC=
CPU_FAMILY=


if [ ${COMPILE_BUILD_TARGET} == iOS ];
then
    export TARGET_TRIPLE_OS="iOS"
else
    export TARGET_TRIPLE_OS="android"
fi

case $PLATFORM_ABI in
  armeabi-v7a)
    #cc       armv7a-linux-androideabi16-clang
    #binutils arm   -linux-androideabi  -ld
    export TARGET_TRIPLE_MACHINE_BINUTILS=arm
    TARGET_TRIPLE_MACHINE_CC=armv7a
    export TARGET_TRIPLE_OS=androideabi
    ;;
  arm64-v8a)
    #cc       aarch64-linux-android21-clang
    #binutils aarch64-linux-android  -ld
    export TARGET_TRIPLE_MACHINE_BINUTILS=aarch64
    ;;
  x86)
    #cc       i686-linux-android16-clang
    #binutils i686-linux-android  -ld
    export TARGET_TRIPLE_MACHINE_BINUTILS=i686
    CPU_FAMILY=x86
    ;;
  x86_64)
    #cc       x86_64-linux-android21-clang
    #binutils x86_64-linux-android  -ld
    export TARGET_TRIPLE_MACHINE_BINUTILS=x86_64
    ;;
  # 再扩展iOS部分, 因为架构基本没重复的, 除了x86_64
  armv7 | armv7s | arm64 | i386)
    export TARGET_TRIPLE_MACHINE_BINUTILS=$PLATFORM_ABI
    ;;
esac

# If the cc-specific variable isn't set, we fallback to binutils version
[ -z "${TARGET_TRIPLE_MACHINE_CC}" ] && TARGET_TRIPLE_MACHINE_CC=${TARGET_TRIPLE_MACHINE_BINUTILS}
export TARGET_TRIPLE_MACHINE_CC=$TARGET_TRIPLE_MACHINE_CC

[ -z "${CPU_FAMILY}" ] && CPU_FAMILY=${TARGET_TRIPLE_MACHINE_BINUTILS}
export CPU_FAMILY=$CPU_FAMILY

# iOS没有前缀
if [ ${COMPILE_BUILD_TARGET} == iOS ];
then
    export CROSS_PREFIX=
else
    # Common prefix for ld, as, etc.
    if [ $DESIRED_BINUTILS = "gnu" ] ; then
      export CROSS_PREFIX=${TARGET_TRIPLE_MACHINE_BINUTILS}-linux-${TARGET_TRIPLE_OS}-
    else
      export CROSS_PREFIX=llvm-
    fi
fi

export CROSS_PREFIX_WITH_PATH=${TOOLCHAIN_PATH}/bin/${CROSS_PREFIX}

# Exporting Binutils paths, if passing just CROSS_PREFIX_WITH_PATH is not enough
# The FAM_ prefix is used to eliminate passing those values implicitly to build systems
export FAM_ADDR2LINE=${CROSS_PREFIX_WITH_PATH}addr2line # unused
export        FAM_AS=${CROSS_PREFIX_WITH_PATH}as
export        FAM_AR=${CROSS_PREFIX_WITH_PATH}ar
export        FAM_NM=${CROSS_PREFIX_WITH_PATH}nm
export   FAM_OBJCOPY=${CROSS_PREFIX_WITH_PATH}objcopy # unused
export   FAM_OBJDUMP=${CROSS_PREFIX_WITH_PATH}objdump # unused
export    FAM_RANLIB=${CROSS_PREFIX_WITH_PATH}ranlib # 都有使用
export   FAM_READELF=${CROSS_PREFIX_WITH_PATH}readelf # 检测TEXTREL用到
export      FAM_SIZE=${CROSS_PREFIX_WITH_PATH}size # unused
export   FAM_STRINGS=${CROSS_PREFIX_WITH_PATH}strings # unused
export     FAM_STRIP=${CROSS_PREFIX_WITH_PATH}strip # 三方及ffmpeg有使用

# 配置compiler
if [ ${COMPILE_BUILD_TARGET} == iOS ];
then
    export TARGET=$HOST
    export FAM_CC=${TOOLCHAIN_PATH}/bin/clang
    export FAM_READELF=$(which readelf)
else
    export TARGET=${TARGET_TRIPLE_MACHINE_CC}-linux-${TARGET_TRIPLE_OS}${ANDROID_PLATFORM}
    export FAM_CC=${TOOLCHAIN_PATH}/bin/${TARGET}-clang
fi

# The name for compiler is slightly different, so it is defined separatly.
export FAM_CXX=${FAM_CC}++
export FAM_LD=${FAM_CC}

# TODO consider abondaning this strategy of defining the name of the clang wrapper
# in favour of just passing -mstackrealign and -fno-addrsig depending on
# PLATFORM_ABI, ANDROID_PLATFORM and NDK's version

# iOS使用系统安装的
# Special variable for the yasm assembler
if [ ${COMPILE_BUILD_TARGET} == iOS ];
then
    export FAM_YASM=$(which yasm)
else
    export FAM_YASM=${TOOLCHAIN_PATH}/bin/yasm
fi


# A variable to which certain dependencies can add -l arguments during build.sh
export FFMPEG_EXTRA_LD_FLAGS=

export INSTALL_DIR=${BUILD_DIR_EXTERNAL}/${PLATFORM_ABI}

# Forcing FFmpeg and its dependencies to look for dependencies
# in a specific directory when pkg-config is used
export PKG_CONFIG_LIBDIR=${INSTALL_DIR}/lib/pkgconfig
