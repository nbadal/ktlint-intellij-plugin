#!/bin/bash

GRADLE_PROPERTIES_FILE=gradle.properties

function getGradleProperty {
    PROPERTY_KEY=$1
    PROPERTY_VALUE=`cat $GRADLE_PROPERTIES_FILE | grep "$PROPERTY_KEY" | cut -d'=' -f2`
    echo $PROPERTY_VALUE
}

INSTRUMENTED_JAR_FILE_NAME=$(find .intellijPlatform/sandbox/ktlint-plugin/*/plugins -name ktlint-plugin*.jar)
if [ $(echo ${INSTRUMENTED_JAR_FILE_NAME} | wc -l | cut -w -f 2) -eq 1 ]; then
  PLUGINS_DIR=${INSTRUMENTED_JAR_FILE_NAME%/ktlint/lib/*}
  INSTRUMENTED_JAR_FILE_NAME_WITHOUT_EXTENSION=${INSTRUMENTED_JAR_FILE_NAME%*.jar}
  PLUGIN_VERSION=${INSTRUMENTED_JAR_FILE_NAME_WITHOUT_EXTENSION##*-plugin-}
  PLUGIN_ZIP_FILE_NAME=ktlint-plugin/build/ktlint-plugin-${PLUGIN_VERSION}.zip
  # Change to plugin directory so that during zip-operation the path relative to that directory are being stored.
  # Save the ZIP-file itself outside the ".intellijPlatform" directory as by default all directories with name starting with a "." are
  # being hidden on MacOS.
  (cd ${PLUGINS_DIR} && zip  -r ../../../../../${PLUGIN_ZIP_FILE_NAME} ktlint)
  echo "Created ZIP file ${PLUGIN_ZIP_FILE_NAME}"
else
  echo "Can not create valid ZIP file. Run clean build first"
fi
