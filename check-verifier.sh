#!/usr/bin/env bash
set -euf -o pipefail

LATEST_BUILD=$(curl -s 'https://data.services.jetbrains.com/products/releases?code=IIU&release.type=eap%2Crc&latest=true' | jq -r '.IIU[0].build')
LATEST=$(echo "$LATEST_BUILD" | cut -d . -f 1)

SINCE=$(grep pluginSinceBuild gradle.properties | cut -d '=' -f 2 | xargs | sed -e 's/\.\*//g')
UNTIL=$(grep pluginUntilBuild gradle.properties | cut -d '=' -f 2 | xargs | sed -e 's/\.\*//g')
CURRENT=$(grep pluginVerifierIdeVersions gradle.properties | cut -d '=' -f 2 | xargs)

TARGETS=$(curl -s 'https://data.services.jetbrains.com/products?code=IIU&fields=releases.build' | jq -r '.[0].releases | .[] | .build' | awk "!seen[substr(\$0, 1, 3)]++ && substr(\$0, 1, 3) >= ${SINCE} && substr(\$0, 1, 3) <= ${UNTIL}" | xargs | sed -e 's/ /, /g')

if [[ "$TARGETS" == "$CURRENT" ]]; then
    echo "Up to date."
    echo "::set-output name=updated::false"
    exit
fi

echo "::set-output name=updated::true"
echo "::set-output name=newTargets::$TARGETS"

sed -i -E "s|(pluginVerifierIdeVersions = )(.*)\$|\1$TARGETS|g" gradle.properties
