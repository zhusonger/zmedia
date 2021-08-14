#!/usr/bin/env bash

function checkVariablePresence() {
  VARIABLE_NAME=$1
  if [[ -z "${!VARIABLE_NAME}" ]]; then
    echo "The ${VARIABLE_NAME} environment variable isn't defined"
    echo $2
    exit 1
  fi
}

# 根据编译平台来检测环境变量
if [ ${COMPILE_BUILD_TARGET} == iOS ];
then
    if [ ! `which yasm` ]
    then
        echo 'Yasm not found'
        if [ ! `which brew` ]
        then
            echo 'Homebrew not found. Trying to install...'
                        ruby -e "$(curl -fsSL https://raw.githubusercontent.com/Homebrew/install/master/install)" \
                || exit 1
        fi
        echo 'Trying to install Yasm...'
        brew install yasm || exit 1
    fi

#     if [ ! `which readelf` ]
#     then
#         echo 'readelf not found'
#         echo 'Trying to install binutils...'
#         brew install binutils || exit 1
#         echo 'export PATH="/usr/local/opt/binutils/bin:$PATH"' >> ~/.bash_profile
#     fi

    if [ ! `which gas-preprocessor.pl` ]
	then
		echo 'gas-preprocessor.pl not found. Trying to install...'
		(curl -L https://github.com/libav/gas-preprocessor/raw/master/gas-preprocessor.pl \
			-o /usr/local/bin/gas-preprocessor.pl \
			&& chmod +x /usr/local/bin/gas-preprocessor.pl) \
			|| exit 1
	fi

	checkVariablePresence "XCODE_DEVELOPER_HOME" \
              "The variable should be set to the actual Xcode Developer path, use xcrun --show-sdk-path see it." || exit 1

    checkVariablePresence "IOS_SDK_VERSION" \
              "The variable should be set to the actual IOS_SDK_VERSION. check it in SDKs." || exit 1
else
    checkVariablePresence "ANDROID_SDK_HOME" \
          "The variable should be set to the actual Android SDK path" || exit 1

        checkVariablePresence "ANDROID_NDK_HOME" \
          "The variable should be set to the actual Android NDK path" || exit 1

        checkVariablePresence "ANDROID_CMAKE_HOME" \
          "The variable should be set to the actual Android CMake path" || exit 1
fi

