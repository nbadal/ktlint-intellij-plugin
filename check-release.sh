#!/usr/bin/env bash
set -euf -o pipefail

LATEST=$(curl -s 'https://data.services.jetbrains.com/products/releases?code=IIU&latest=true' | jq -r '.IIU[0].build' | cut -d . -f 1)
UNTIL="${LATEST}.*"

CURRENT=$(grep pluginUntilBuild gradle.properties | cut -d '=' -f 2 | xargs)

if [[ "$UNTIL" == "$CURRENT" ]]; then
    echo "Up to date."
    echo "::set-output name=updated::false"
    exit
fi

echo "::set-output name=updated::true"
echo "::set-output name=newUntil::$UNTIL"

sed -i "s|$CURRENT|$UNTIL|g" gradle.properties

