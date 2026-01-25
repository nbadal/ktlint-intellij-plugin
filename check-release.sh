#!/usr/bin/env bash
set -euf -o pipefail

if [[ "${GITHUB_ACTIONS:-}" != "true" ]]; then
    export GITHUB_OUTPUT="${GITHUB_OUTPUT:-/dev/stdout}"
fi

PLUGIN_UNTIL_BUILD=$(grep pluginUntilBuild gradle.properties | cut -d '=' -f 2 | xargs)
echo "Current plugin supports until build ${PLUGIN_UNTIL_BUILD}"

LATEST_IDEA_BUILD=$(curl -s 'https://data.services.jetbrains.com/products/releases?code=IIU&release.type=eap%2Crc&latest=true' | jq -r '.IIU[0].build')
echo "Latest IDEA build ${LATEST_IDEA_BUILD}"

if [[ "$LATEST_IDEA_BUILD" == "$PLUGIN_UNTIL_BUILD" ]]; then
    echo "Plugin supports latest IDEA build (exact match)"
    echo "Up to date."
    echo "updated=false" >> $GITHUB_OUTPUT
    exit
elif [[ "$PLUGIN_UNTIL_BUILD"  =~ ^.*\.\*$ ]]; then
    # Check for match on version with wildcard
    # pluginUntilBuild has format like "241.*"
    LATEST_IDEA_VERSION=$(echo "$LATEST_IDEA_BUILD" | cut -d . -f 1)
    UNTIL_VERSION="${LATEST_IDEA_VERSION}.*"
    if [[ "$UNTIL_VERSION" == "$PLUGIN_UNTIL_BUILD" ]]; then
        echo "Plugin supports latest IDEA build (wildcard match)"
        echo "updated=false" >> $GITHUB_OUTPUT
        exit
    fi
    echo "Major version of latest IDEA version ($LATEST_IDEA_VERSION) does not match with major version of pluginUntilBuild in 'gradle.properties' ($PLUGIN_UNTIL_BUILD)"
else
    echo "Latest IDEA version ($LATEST_IDEA_BUILD) does not match with pluginUntilBuild in 'gradle.properties' ($PLUGIN_UNTIL_BUILD)"
    UNTIL_VERSION=${LATEST_IDEA_BUILD}
fi

echo "updated=true" >> $GITHUB_OUTPUT
echo "newUntil=$UNTIL_VERSION" >> $GITHUB_OUTPUT

if [[ "$(uname -s)" == "Darwin" ]]; then
    sed -i '' "s|pluginUntilBuild = $PLUGIN_UNTIL_BUILD|pluginUntilBuild = $UNTIL_VERSION|g" gradle.properties
else
    sed -i "s|pluginUntilBuild = $PLUGIN_UNTIL_BUILD|pluginUntilBuild = $UNTIL_VERSION|g" gradle.properties
fi
