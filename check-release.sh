#!/usr/bin/env bash
set -euf -o pipefail

PLUGIN_UNTIL_BUILD=$(grep pluginUntilBuild gradle.properties | cut -d '=' -f 2 | xargs)
echo "Current plugin supports until build ${PLUGIN_UNTIL_BUILD}"

LATEST_IDEA_BUILD=$(curl -s 'https://data.services.jetbrains.com/products/releases?code=IIU&release.type=eap%2Crc&latest=true' | jq -r '.IIU[0].build')
echo "Latest IDEA build ${LATEST_IDEA_BUILD}"

# Check for exact match of entire build version
if [[ "$LATEST_IDEA_BUILD" == "$PLUGIN_UNTIL_BUILD" ]]; then
    echo "Plugin supports latest IDEA build (exact match)"
    echo "Up to date."
    echo "updated=false" >> $GITHUB_OUTPUT
    exit
fi

# Check for match on version with wildcard
if [[ "$PLUGIN_UNTIL_BUILD"  =~ ^.*\.\*$ ]]; then
    # pluginUntilBuild has format like "241.*"
    LATEST_IDEA_VERSION=$(echo "$LATEST_IDEA_BUILD" | cut -d . -f 1)
    UNTIL_VERSION="${LATEST_IDEA_VERSION}.*"
    if [[ "$UNTIL_VERSION" == "$PLUGIN_UNTIL_BUILD" ]]; then
        echo "Plugin supports latest IDEA build (wildcard match)"
        echo "updated=false" >> $GITHUB_OUTPUT
        exit
    fi
fi

echo "updated=true" >> $GITHUB_OUTPUT
echo "newUntil=$UNTIL_VERSION" >> $GITHUB_OUTPUT

sed -i -E "s|(pluginVerifierIdeVersions = )(.*)\$|\1\2, $LATEST_IDEA_BUILD|g" gradle.properties
sed -i "s|pluginUntilBuild = $PLUGIN_UNTIL_BUILD|pluginUntilBuild = $UNTIL_VERSION|g" gradle.properties
