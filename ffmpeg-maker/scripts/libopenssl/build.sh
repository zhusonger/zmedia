#!/usr/bin/env bash

if [ ${COMPILE_BUILD_TARGET} == Android ];
then
  CC=${FAM_CC}
  PATH=${TOOLCHAIN_PATH}/bin:$PATH
  case $PLATFORM_ABI in
  x86)
    ARCH=android-x86
    ;;
  x86_64)
    ARCH=android-x86_64
    ;;
  armeabi*)
    ARCH=android-arm
    ;;
  arm64*)
    ARCH=android-arm64
    ;;
  esac

  CC=${FAM_CC}
  AR=${FAM_AR}
  RANLIB=${FAM_RANLIB}

  ./Configure ${ARCH} \
  -D__ANDROID_API__=${DESIRED_ANDROID_API_LEVEL} \
  --prefix=${INSTALL_DIR} \
  --openssldir=${INSTALL_DIR} \
  no-asm \
  no-hw \
  no-pic \
  no-egd \
  no-zlib \
  no-afalgeng \
  no-autoalginit \
  no-autoerrinit \
  no-autoload-config \
  no-capieng \
  no-cms \
  no-comp \
  no-ct \
  no-dgram \
  no-dso \
  no-dynamic-engine \
  no-ec \
  no-ec2m \
  no-engine \
  no-err \
  no-filenames \
  no-gost \
  no-makedepend \
  no-multiblock \
  no-nextprotoneg \
  no-ocsp \
  no-hw-padlock \
  no-pic \
  no-pinshared \
  no-posix-io \
  no-psk \
  no-rdrand \
  no-rfc3779 \
  no-sock \
  no-srp \
  no-srtp \
  no-sse2 \
  no-ssl-trace \
  no-stdio \
  no-tests \
  no-ts \
  no-ui-console \
  no-tls \
  no-dtls \
  no-ssl \
  no-ssl2 \
  no-ssl3 \
  no-aria \
  no-bf \
  no-blake2 \
  no-camellia \
  no-cast \
  no-chacha \
  no-cmac \
  no-des \
  no-dh \
  no-dsa \
  no-ecdh \
  no-ecdsa \
  no-idea \
  no-md2 \
  no-md4 \
  no-mdc2 \
  no-ocb \
  no-poly1305 \
  no-rc2 \
  no-rc4 \
  no-rc5 \
  no-rmd160 \
  no-scrypt \
  no-seed \
  no-siphash \
  no-sm2 \
  no-sm3 \
  no-sm4 \
  no-whirlpool || exit 1

  ${MAKE_EXECUTABLE} clean
  ${MAKE_EXECUTABLE} -j${HOST_NPROC}
  ${MAKE_EXECUTABLE} install
fi
#
#X264_AS=${FAM_CC}
#
#X264_ADDITIONAL_FLAGS=
#
#case $PLATFORM_ABI in
#  x86)
#    # Disabling assembler optimizations due to text relocations
#    X264_ADDITIONAL_FLAGS=--disable-asm
#    ;;
#  x86_64)
#    X264_AS=${NASM_EXECUTABLE}
#    ;;
#esac
#
#CC=${FAM_CC} \
#AR=${FAM_AR} \
#AS=${X264_AS} \
#RANLIB=${FAM_RANLIB} \
#STRIP=${FAM_STRIP} \
#./Configure \
#    --prefix=${INSTALL_DIR} \
#    --host=${TARGET} \
#    --sysroot=${SYSROOT_PATH} \
#    --enable-pic \
#    --enable-static \
#    --disable-cli \
#    --disable-avs \
#    --disable-lavf \
#    --disable-cli \
#    --disable-ffms \
#    --disable-opencl \
#    --chroma-format=all \
#    --bit-depth=all \
#    --extra-cflags="${EXTRA_C_FLAGS}" \
#    --extra-asflags="${EXTRA_AS_FLAGS}" \
#    --extra-ldflags="${EXTRA_LD_FLAGS}" \
#    ${X264_ADDITIONAL_FLAGS}  || exit 1
#
#${MAKE_EXECUTABLE} clean
#${MAKE_EXECUTABLE} -j${HOST_NPROC}
#${MAKE_EXECUTABLE} install
