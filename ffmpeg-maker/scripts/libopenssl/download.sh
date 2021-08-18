
#!/usr/bin/env bash

source ${SCRIPTS_DIR}/common-functions.sh

# 需要下载的版本号
# https://www.openssl.org/source/
# 1.1.1k支持到2023.9.11
OPENSSL_VERSION=1.1.1k

downloadTarArchive \
  "libopenssl" \
  "https://www.openssl.org/source/openssl-${OPENSSL_VERSION}.tar.gz"