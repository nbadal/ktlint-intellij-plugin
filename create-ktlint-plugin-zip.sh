#!/bin/bash

GRADLE_PROPERTIES_FILE=gradle.properties

function getGradleProperty {
    PROPERTY_KEY=$1
    PROPERTY_VALUE=`cat $GRADLE_PROPERTIES_FILE | grep "$PROPERTY_KEY" | cut -d'=' -f2`
    echo $PROPERTY_VALUE
}

if [ $(find plugin/build/idea-sandbox/plugins -name instrumented*.jar | wc -l | cut -w -f 2) -eq 1 ]; then
  PLUGIN_VERSION=$(getGradleProperty "pluginVersion")
  PLUGIN_ZIP_FILE_NAME=ktlint-plugin-${PLUGIN_VERSION}.zip
  (cd plugin/build/idea-sandbox/plugins && zip  -r ../../${PLUGIN_ZIP_FILE_NAME} ktlint)
  echo "Created ZIP file ./plugin/build/${PLUGIN_ZIP_FILE_NAME}"
else
  echo "Can not create valid ZIP file. Run clean build first"
fi
