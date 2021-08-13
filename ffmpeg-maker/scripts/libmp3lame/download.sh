#!/usr/bin/env bash

source ${SCRIPTS_DIR}/common-functions.sh

# 需要下载的版本号
# http://downloads.videolan.org/pub/contrib/lame
# lame-3.100.tar.gz
# lame-3.97.tar.gz
# lame-3.99.5.tar.gz
LAME_VERSION=3.100

downloadTarArchive \
  "libmp3lame" \
  "http://downloads.videolan.org/pub/contrib/lame/lame-${LAME_VERSION}.tar.gz"
