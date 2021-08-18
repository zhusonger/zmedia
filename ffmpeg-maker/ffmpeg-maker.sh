#!/usr/bin/env bash

# Defining essential directories

# The root of the project
export BASE_DIR="$( cd "$( dirname "$0" )" && pwd )"
# Directory that contains source code for FFmpeg and its dependencies
# Each library has its own subdirectory
# Multiple versions of the same library can be stored inside librarie's directory
export SOURCES_DIR=${BASE_DIR}/sources
# Directory to place some statistics about the build.
# Currently - the info about Text Relocations
export STATS_DIR=${BASE_DIR}/stats
# Directory that contains helper scripts and
# scripts to download and build FFmpeg and each dependency separated by subdirectories
export SCRIPTS_DIR=${BASE_DIR}/scripts
# The directory to use by Android project
# All FFmpeg's libraries and headers are copied there
export OUTPUT_DIR=${BASE_DIR}/output


FFMPEG_LIB_SUFFIX=".so"
FFMPEG_BUILD_SH="build-android.sh"
# 配置下编译环境路径
if [ ${COMPILE_BUILD_TARGET} == iOS ];
then
    export XCODE_DEVELOPER_HOME=/Applications/Xcode.app/Contents/Developer
    export IOS_SDK_VERSION=14.5
    FFMPEG_BUILD_SH="build-iOS.sh"
    FFMPEG_LIB_SUFFIX=".a"
else
    # 配置自己电脑上的SDK&NDK路径, CMAKE用于生成so库, NDK交叉编译
    export ANDROID_SDK_HOME=~/Documents/Dev/android-sdk-macosx
    export ANDROID_NDK_HOME=$ANDROID_SDK_HOME/ndk/21.3.6528147
    export ANDROID_CMAKE_HOME=$ANDROID_SDK_HOME/cmake/3.10.2.4988404/bin
fi

# Check the host machine for proper setup and fail fast otherwise
${SCRIPTS_DIR}/check-host-machine.sh || exit 1

# Directory to use as a place to build/install FFmpeg and its dependencies
BUILD_DIR=${BASE_DIR}/build
# Separate directory to build FFmpeg to
export BUILD_DIR_FFMPEG=$BUILD_DIR/ffmpeg
# All external libraries are installed to a single root
# to make easier referencing them when FFmpeg is being built.
export BUILD_DIR_EXTERNAL=$BUILD_DIR/external


# Function that copies *.so files and headers of the current PLATFORM_ABI
# to the proper place inside OUTPUT_DIR
function prepareOutput() {
  OUTPUT_LIB=${OUTPUT_DIR}/lib/${PLATFORM_ABI}
  mkdir -p ${OUTPUT_LIB}
  cp ${BUILD_DIR_FFMPEG}/${PLATFORM_ABI}/lib/*${FFMPEG_LIB_SUFFIX} ${OUTPUT_LIB}

  OUTPUT_HEADERS=${OUTPUT_DIR}/include/${PLATFORM_ABI}
  mkdir -p ${OUTPUT_HEADERS}
  cp -r ${BUILD_DIR_FFMPEG}/${PLATFORM_ABI}/include/* ${OUTPUT_HEADERS}
}

# Saving stats about text relocation presence.
# If the result file doesn't have 'TEXTREL' at all, then we are good.
# Otherwise the whole script is interrupted
function checkTextRelocations() {
  TEXT_REL_STATS_FILE=${STATS_DIR}/text-relocations.txt
  # 根据平台修改库后缀
  ${FAM_READELF} --dynamic ${BUILD_DIR_FFMPEG}/${PLATFORM_ABI}/lib/*${FFMPEG_LIB_SUFFIX} | grep 'TEXTREL\|File' >> ${TEXT_REL_STATS_FILE}
  if grep -q TEXTREL ${TEXT_REL_STATS_FILE}; then
    echo "There are text relocations in output files:"
    cat ${TEXT_REL_STATS_FILE}
    exit 1
  fi
}

# Actual work of the script

# Clearing previously created binaries
rm -rf ${BUILD_DIR}
rm -rf ${STATS_DIR}
rm -rf ${OUTPUT_DIR}
mkdir -p ${STATS_DIR}
mkdir -p ${OUTPUT_DIR}

# Exporting more necessary variabls
source ${SCRIPTS_DIR}/export-host-variables.sh
source ${SCRIPTS_DIR}/parse-arguments.sh

# Treating FFmpeg as just a module to build after its dependencies
COMPONENTS_TO_BUILD=${EXTERNAL_LIBRARIES[@]}
if [ "$BUILD_FFMPEG" = true ] ; then
  echo "Append ffmpeg to COMPONENTS_TO_BUILD"
  COMPONENTS_TO_BUILD+=( "ffmpeg" )
fi

# Get the source code of component to build
for COMPONENT in ${COMPONENTS_TO_BUILD[@]}
do
  echo "Getting source code of the component: ${COMPONENT}"
  SOURCE_DIR_FOR_COMPONENT=${SOURCES_DIR}/${COMPONENT}

  mkdir -p ${SOURCE_DIR_FOR_COMPONENT}
  cd ${SOURCE_DIR_FOR_COMPONENT}

  # Executing the component-specific script for downloading the source code
  source ${SCRIPTS_DIR}/${COMPONENT}/download.sh

  # The download.sh script has to export SOURCES_DIR_$COMPONENT variable
  # with actual path of the source code. This is done for possiblity to switch
  # between different verions of a component.
  # If it isn't set, consider SOURCE_DIR_FOR_COMPONENT as the proper value
  COMPONENT_SOURCES_DIR_VARIABLE=SOURCES_DIR_${COMPONENT}
  if [[ -z "${!COMPONENT_SOURCES_DIR_VARIABLE}" ]]; then
     export SOURCES_DIR_${COMPONENT}=${SOURCE_DIR_FOR_COMPONENT}
  fi

  # Returning to the rood directory. Just in case.
  cd ${BASE_DIR}
done

# Main build loop
for ABI in ${FFMPEG_ABIS_TO_BUILD[@]}
do
  # Exporting variables for the current ABI
  source ${SCRIPTS_DIR}/export-build-variables.sh ${ABI}

  for COMPONENT in ${COMPONENTS_TO_BUILD[@]}
  do
    echo "Building the component: ${COMPONENT}"
    COMPONENT_SOURCES_DIR_VARIABLE=SOURCES_DIR_${COMPONENT}

    # Going to the actual source code directory of the current component
    cd ${!COMPONENT_SOURCES_DIR_VARIABLE}
    echo "Enter the ${COMPONENT} source dir: ${!COMPONENT_SOURCES_DIR_VARIABLE}"

    sh_name=build.sh
    if [ ${COMPONENT} == "ffmpeg" ];then
        sh_name=${FFMPEG_BUILD_SH}
        echo "change ${COMPONENT} build.sh => ${FFMPEG_BUILD_SH}"
    fi

    # and executing the component-specific build script
    source ${SCRIPTS_DIR}/${COMPONENT}/${sh_name} || exit 1

    # Returning to the root directory. Just in case.
    cd ${BASE_DIR}
  done

  # Android才检查
  if [ ${COMPILE_BUILD_TARGET} == Android ];then
      checkTextRelocations || exit 1
  fi
  prepareOutput
done
