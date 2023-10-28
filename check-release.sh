#!/usr/bin/env bash
set -euf -o pipefail

LATEST_BUILD=$(curl -s 'https://data.services.jetbrains.com/products/releases?code=IIU&release.type=eap%2Crc&latest=true' | jq -r '.IIU[0].build')
LATEST=$(echo "$LATEST_BUILD" | cut -d . -f 1)
UNTIL="${LATEST}.*"

CURRENT=$(grep pluginUntilBuild gradle.properties | cut -d '=' -f 2 | xargs)

if [[ "$UNTIL" == "$CURRENT" ]]; then
    echo "Up to date."
    echo "updated=false" >> $GITHUB_OUTPUT
    exit
fi

echo "updated=true" >> $GITHUB_OUTPUT
echo "newUntil=$UNTIL" >> $GITHUB_OUTPUT

sed -i -E "s|(pluginVerifierIdeVersions = )(.*)\$|\1\2, $LATEST_BUILD|g" gradle.properties
sed -i "s|pluginUntilBuild = $CURRENT|pluginUntilBuild = $UNTIL|g" gradle.properties
