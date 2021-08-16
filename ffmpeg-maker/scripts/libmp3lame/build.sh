#!/usr/bin/env bash


if [ ${COMPILE_BUILD_TARGET} == iOS ];
then
    echo "libmp3lame for iOS"
    ARCH=${PLATFORM_ABI}
    if [ "$ARCH" = "i386" -o "$ARCH" = "x86_64" ]
    then
        PLATFORM="iPhoneSimulator"
        if [ "$ARCH" = "x86_64" ]
        then
            SIMULATOR="-mios-simulator-version-min=${API_LEVEL}"
            TARGET=x86_64-apple-darwin
        else
            SIMULATOR="-mios-simulator-version-min=${API_LEVEL}"
            TARGET=i386-apple-darwin
        fi
    else
        PLATFORM="iPhoneOS"
        SIMULATOR=
        HOST=arm-apple-darwin
    fi

    XCRUN_SDK=`echo $PLATFORM | tr '[:upper:]' '[:lower:]'`
    CC="xcrun -sdk $XCRUN_SDK clang -arch $ARCH"
    #AS="$CWD/$SOURCE/extras/gas-preprocessor.pl $CC"
    CFLAGS="-arch $ARCH $SIMULATOR"
    if ! xcodebuild -version | grep "Xcode [1-6]\."
    then
        CFLAGS="$CFLAGS -fembed-bitcode"
    fi
    CXXFLAGS="$CFLAGS"
    LDFLAGS="$CFLAGS"

    CC=$CC ./configure \
        --prefix=${INSTALL_DIR} \
        --host=${TARGET} \
        --with-sysroot=${SYSROOT_PATH} \
        --disable-shared \
        --enable-static \
        --with-pic \
        --disable-fast-install \
        --disable-analyzer-hooks \
        --disable-gtktest \
        --disable-frontend \
        CC="$CC" CFLAGS="$CFLAGS" LDFLAGS="$LDFLAGS" || exit 1
else
    echo "libmp3lame for Android"
    ./configure \
    --prefix=${INSTALL_DIR} \
    --host=${TARGET} \
    --with-sysroot=${SYSROOT_PATH} \
    --disable-shared \
    --enable-static \
    --with-pic \
    --disable-fast-install \
    --disable-analyzer-hooks \
    --disable-gtktest \
    --disable-frontend \
    CC=${FAM_CC} \
    AR=${FAM_AR} \
    RANLIB=${FAM_RANLIB} || exit 1
fi

${MAKE_EXECUTABLE} clean
${MAKE_EXECUTABLE} -j${HOST_NPROC}
${MAKE_EXECUTABLE} install
