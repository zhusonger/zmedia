#!/usr/bin/env bash

case $PLATFORM_ABI in
  i386 | x86_64)
    # Disabling assembler optimizations, because they have text relocations
    EXTRA_BUILD_CONFIGURATION_FLAGS="$EXTRA_BUILD_CONFIGURATION_FLAGS --disable-asm"
    EXTRA_BUILD_CONFIGURATION_FLAGS="$EXTRA_BUILD_CONFIGURATION_FLAGS --disable-mmx"
    EXTRA_BUILD_CONFIGURATION_FLAGS="$EXTRA_BUILD_CONFIGURATION_FLAGS --assert-level=2"
    ;;
    *)
    EXTRA_BUILD_CONFIGURATION_FLAGS="$EXTRA_BUILD_CONFIGURATION_FLAGS --disable-asm"
    EXTRA_BUILD_CONFIGURATION_FLAGS="$EXTRA_BUILD_CONFIGURATION_FLAGS --disable-debug"
    EXTRA_BUILD_CONFIGURATION_FLAGS="$EXTRA_BUILD_CONFIGURATION_FLAGS --enable-pic"
    EXTRA_BUILD_CONFIGURATION_FLAGS="$EXTRA_BUILD_CONFIGURATION_FLAGS --enable-neon"
    EXTRA_BUILD_CONFIGURATION_FLAGS="$EXTRA_BUILD_CONFIGURATION_FLAGS --enable-optimizations"
    if [ "$PLATFORM_ABI" = "arm64" ]
    then
        EXPORT="GASPP_FIX_XCODE5=1"
    fi
    ;;
esac

if [ "$FFMPEG_GPL_ENABLED" = true ] ; then
    EXTRA_BUILD_CONFIGURATION_FLAGS="$EXTRA_BUILD_CONFIGURATION_FLAGS --enable-gpl --enable-version3"
fi

# Preparing flags for enabling requested libraries
ADDITIONAL_COMPONENTS=
for LIBARY_NAME in ${FFMPEG_EXTERNAL_LIBRARIES[@]}
do
  ADDITIONAL_COMPONENTS+=" --enable-$LIBARY_NAME"
done

# Referencing dependencies without pkgconfig
DEP_CFLAGS="$EXTRA_C_FLAGS -I${BUILD_DIR_EXTERNAL}/${PLATFORM_ABI}/include"
DEP_LD_FLAGS="$EXTRA_LD_FLAGS -L${BUILD_DIR_EXTERNAL}/${PLATFORM_ABI}/lib $FFMPEG_EXTRA_LD_FLAGS"

# 配置选项:
# disable 关闭 enable开启
# --disable-static 与 --enable-shared成对使用
# static 表示生产.a静态库 shared表示生成.so动态库
COMMON_OPTIONS="$COMMON_OPTIONS --enable-static"
COMMON_OPTIONS="$COMMON_OPTIONS --disable-shared"

# 程序选项
COMMON_OPTIONS="$COMMON_OPTIONS --disable-programs"
COMMON_OPTIONS="$COMMON_OPTIONS --disable-ffmpeg"
COMMON_OPTIONS="$COMMON_OPTIONS --disable-ffplay"
COMMON_OPTIONS="$COMMON_OPTIONS --disable-ffprobe"

# 文档选项
COMMON_OPTIONS="$COMMON_OPTIONS --disable-doc"
COMMON_OPTIONS="$COMMON_OPTIONS --disable-htmlpages"
COMMON_OPTIONS="$COMMON_OPTIONS --disable-manpages"
COMMON_OPTIONS="$COMMON_OPTIONS --disable-podpages"
COMMON_OPTIONS="$COMMON_OPTIONS --disable-txtpages"

# 组件选项：
COMMON_OPTIONS="$COMMON_OPTIONS --disable-avdevice"
COMMON_OPTIONS="$COMMON_OPTIONS --enable-avcodec"
COMMON_OPTIONS="$COMMON_OPTIONS --enable-avformat"
COMMON_OPTIONS="$COMMON_OPTIONS --enable-swresample"
COMMON_OPTIONS="$COMMON_OPTIONS --enable-swscale"
COMMON_OPTIONS="$COMMON_OPTIONS --disable-postproc"
COMMON_OPTIONS="$COMMON_OPTIONS --disable-avfilter"
COMMON_OPTIONS="$COMMON_OPTIONS --enable-pthreads"
COMMON_OPTIONS="$COMMON_OPTIONS --disable-w32threads"
COMMON_OPTIONS="$COMMON_OPTIONS --disable-os2threads"
COMMON_OPTIONS="$COMMON_OPTIONS --disable-network"
COMMON_OPTIONS="$COMMON_OPTIONS --disable-dct"
COMMON_OPTIONS="$COMMON_OPTIONS --disable-dwt"
COMMON_OPTIONS="$COMMON_OPTIONS --disable-error-resilience"
COMMON_OPTIONS="$COMMON_OPTIONS --disable-lsp"
COMMON_OPTIONS="$COMMON_OPTIONS --disable-lzo"
COMMON_OPTIONS="$COMMON_OPTIONS --disable-mdct"
COMMON_OPTIONS="$COMMON_OPTIONS --disable-rdft"
COMMON_OPTIONS="$COMMON_OPTIONS --disable-fft"
COMMON_OPTIONS="$COMMON_OPTIONS --disable-faan"
COMMON_OPTIONS="$COMMON_OPTIONS --disable-pixelutils"

# 单个组件选项:
COMMON_OPTIONS="$COMMON_OPTIONS --disable-everything"
COMMON_OPTIONS="$COMMON_OPTIONS --disable-encoders" # 编码器
COMMON_OPTIONS="$COMMON_OPTIONS --disable-decoders" # 解码器
COMMON_OPTIONS="$COMMON_OPTIONS --disable-hwaccels" # 硬件加速
COMMON_OPTIONS="$COMMON_OPTIONS --disable-muxers" # 合成器
COMMON_OPTIONS="$COMMON_OPTIONS --disable-parsers" # 解析器, 一般跟解码器结合使用
COMMON_OPTIONS="$COMMON_OPTIONS --disable-demuxers" # 分解器
COMMON_OPTIONS="$COMMON_OPTIONS --disable-bsfs" # 位流过滤器, 处理某些格式变体调整保证解析器正常工作
COMMON_OPTIONS="$COMMON_OPTIONS --disable-devices"
COMMON_OPTIONS="$COMMON_OPTIONS --disable-filters"
COMMON_OPTIONS="$COMMON_OPTIONS --disable-protocols"
## 指定具体开启的功能, 精简库大小
COMMON_OPTIONS="$COMMON_OPTIONS --enable-protocol=file"

# aac依赖
# COMMON_OPTIONS="$COMMON_OPTIONS --enable-mdct"
# COMMON_OPTIONS="$COMMON_OPTIONS --enable-fft"
# mpegaudio依赖
# COMMON_OPTIONS="$COMMON_OPTIONS --enable-dct"
# COMMON_OPTIONS="$COMMON_OPTIONS --enable-rdft"
# 解码器
## =====> 音频
## aac解码器
COMMON_OPTIONS="$COMMON_OPTIONS --enable-decoder=aac"
COMMON_OPTIONS="$COMMON_OPTIONS --enable-decoder=pcm_s16le"
## =====> 视频
COMMON_OPTIONS="$COMMON_OPTIONS --enable-decoder=png"
COMMON_OPTIONS="$COMMON_OPTIONS --enable-decoder=h264"
COMMON_OPTIONS="$COMMON_OPTIONS --enable-decoder=mp3"
COMMON_OPTIONS="$COMMON_OPTIONS --enable-decoder=mjpeg"
COMMON_OPTIONS="$COMMON_OPTIONS --enable-decoder=mpeg4"
COMMON_OPTIONS="$COMMON_OPTIONS --enable-decoder=gif"
COMMON_OPTIONS="$COMMON_OPTIONS --enable-decoder=hevc"

## 支持Anroid硬解码部分
# COMMON_OPTIONS="$COMMON_OPTIONS --enable-mediacodec"
# COMMON_OPTIONS="$COMMON_OPTIONS --enable-decoder=h264_mediacodec"
# COMMON_OPTIONS="$COMMON_OPTIONS --enable-decoder=mpeg4_mediacodec"
# COMMON_OPTIONS="$COMMON_OPTIONS --enable-decoder=vp9_mediacodec"
# COMMON_OPTIONS="$COMMON_OPTIONS --enable-decoder=vp8_mediacodec"
# COMMON_OPTIONS="$COMMON_OPTIONS --enable-decoder=hevc_mediacodec"

# 编码器
## =====> 音频
# aac编码器
COMMON_OPTIONS="$COMMON_OPTIONS --enable-encoder=aac"
# # =====> 视频
COMMON_OPTIONS="$COMMON_OPTIONS --enable-encoder=libx264"
COMMON_OPTIONS="$COMMON_OPTIONS --enable-encoder=png"
COMMON_OPTIONS="$COMMON_OPTIONS --enable-encoder=mjpeg"
COMMON_OPTIONS="$COMMON_OPTIONS --enable-encoder=gif"

# 合成器
COMMON_OPTIONS="$COMMON_OPTIONS --enable-muxer=mov"
COMMON_OPTIONS="$COMMON_OPTIONS --enable-muxer=mp4"
COMMON_OPTIONS="$COMMON_OPTIONS --enable-muxer=image2"
COMMON_OPTIONS="$COMMON_OPTIONS --enable-muxer=mp3"
COMMON_OPTIONS="$COMMON_OPTIONS --enable-muxer=ipod"
COMMON_OPTIONS="$COMMON_OPTIONS --enable-muxer=gif"

# 分解器
COMMON_OPTIONS="$COMMON_OPTIONS --enable-demuxer=mov"
COMMON_OPTIONS="$COMMON_OPTIONS --enable-demuxer=mp3"
COMMON_OPTIONS="$COMMON_OPTIONS --enable-demuxer=image2"
COMMON_OPTIONS="$COMMON_OPTIONS --enable-demuxer=gif"
COMMON_OPTIONS="$COMMON_OPTIONS --enable-demuxer=wav"

# 外部库支持:
# COMMON_OPTIONS="$COMMON_OPTIONS --enable-jni"

# Toolchain options:
COMMON_OPTIONS="$COMMON_OPTIONS --enable-cross-compile"
COMMON_OPTIONS="$COMMON_OPTIONS --enable-pic"
COMMON_OPTIONS="$COMMON_OPTIONS --enable-zlib"
# 高级选项(仅限专家):
COMMON_OPTIONS="$COMMON_OPTIONS --disable-symver"

# 优化选项:
COMMON_OPTIONS="$COMMON_OPTIONS --enable-fast-unaligned"

# 优化大小
COMMON_OPTIONS="$COMMON_OPTIONS --enable-small"
COMMON_OPTIONS="$COMMON_OPTIONS --disable-runtime-cpudetect"
COMMON_OPTIONS="$COMMON_OPTIONS --disable-autodetect"

./configure \
  --prefix=${BUILD_DIR_FFMPEG}/${PLATFORM_ABI} \
  --target-os=darwin \
  --arch=${TARGET_TRIPLE_MACHINE_BINUTILS} \
  --sysroot=${SYSROOT_PATH} \
  --cc=${FAM_CC} \
  --cxx=${FAM_CXX} \
  --ld=${FAM_LD} \
  --ar=${FAM_AR} \
  --as=${FAM_CC} \
  --nm=${FAM_NM} \
  --ranlib=${FAM_RANLIB} \
  --strip=${FAM_STRIP} \
  --extra-cflags="-O3 -fPIC $DEP_CFLAGS" \
  --extra-ldflags="$DEP_LD_FLAGS" \
  ${COMMON_OPTIONS} \
  --pkg-config=${PKG_CONFIG_EXECUTABLE} \
  ${EXTRA_BUILD_CONFIGURATION_FLAGS} \
  $ADDITIONAL_COMPONENTS || exit 1

${MAKE_EXECUTABLE} clean
${MAKE_EXECUTABLE} -j${HOST_NPROC}
${MAKE_EXECUTABLE} install $EXPORT