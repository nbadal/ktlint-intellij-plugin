#!/usr/bin/env bash
set -euf -o pipefail

MIN_VERSION="1.0.0"
IGNORED_VERSIONS=""

MAVEN_METADATA_URL="https://repo1.maven.org/maven2/com/pinterest/ktlint/ktlint-ruleset-standard/maven-metadata.xml"

tmp_mods=$(mktemp)
trap 'rm -f "$tmp_mods"' EXIT

version_ge() {
    local a=$1 b=$2
    awk -v a="$a" -v b="$b" 'BEGIN {
        n=split(a, A, "."); m=split(b, B, ".");
        for (i=1; i<=3; i++) {
            ai = (i<=n ? A[i]+0 : 0);
            bi = (i<=m ? B[i]+0 : 0);
            if (ai>bi) { print 1; exit }
            if (ai<bi) { print 0; exit }
        }
        print 1
    }'
}

ignored_list=$(echo "$IGNORED_VERSIONS" | tr ',' '\n' | sed '/^$/d' | sort -u)

published_versions=$(
    curl -s "$MAVEN_METADATA_URL" \
        | grep -oE '<version>[^<]+' \
        | sed 's/<version>//' \
        | grep -v '-' \
        | while read -r v; do
            if [[ $(version_ge "$v" "$MIN_VERSION") == "1" ]]; then
                echo "$v"
            fi
        done \
        | sort -u
)

published_versions=$(
    comm -23 <(echo "$published_versions") <(echo "$ignored_list")
)

module_versions=$(
    find ktlint-lib -maxdepth 1 -type d -name "ruleset-*" -print \
        | sed 's#.*/ruleset-##' \
        | sed 's/-/./g' \
        | sort -u
)

echo "$module_versions" > "$tmp_mods"

enum_lines=$(
    rg -n "^\s*V[0-9]+(_[0-9]+)+\(" ktlint-lib/src/main/kotlin/com/pinterest/ktlint/ruleset/standard/KtlintRulesetVersion.kt
)

enum_versions=$(
    echo "$enum_lines" | awk '{
        if (match($0, /V[0-9_]+/)) {
            v = substr($0, RSTART, RLENGTH)
            sub(/^V/, "", v)
            gsub(/_/, ".", v)
            print v
        }
    }' | sort -u
)

supported_versions=$(
    {
        echo "$module_versions"
        echo "$enum_versions"
    } | sort -u
)

unsupported_versions=$(comm -23 <(echo "$published_versions") <(echo "$supported_versions"))

modules_missing_in_enum=$(comm -23 <(echo "$module_versions") <(echo "$enum_versions"))

enum_missing_in_modules=$(
    echo "$enum_lines" | awk -v mods_file="$tmp_mods" '
        BEGIN {
            # Load module versions into a lookup map for fast membership checks.
            while ((getline line < mods_file) > 0) {
                if (line != "") {
                    modsMap[line] = 1
                }
            }
            close(mods_file)
        }
        {
            # Extract the enum entry version (e.g., V1_7_0 -> 1.7.0).
            if (match($0, /V[0-9_]+/)) {
                version = substr($0, RSTART, RLENGTH)
                sub(/^V/, "", version)
                gsub(/_/, ".", version)

                # Detect optional fallback version specified in the enum ctor, e.g. V1_7_0(..., V1_7_2)
                fallback = ""
                if (match($0, /, *V[0-9_]+/)) {
                    fallback = substr($0, RSTART, RLENGTH)
                    gsub(/[^V0-9_]/, "", fallback)
                    sub(/^V/, "", fallback)
                    gsub(/_/, ".", fallback)
                }

                # Only report when neither the enum version nor its fallback exists as a module.
                if (!(version in modsMap)) {
                    if (fallback == "" || !(fallback in modsMap)) {
                        print version
                    }
                }
            }
        }
    ' | sort -u
)

if [[ -z "$published_versions" ]]; then
    echo "Failed to resolve published ktlint ruleset versions from $MAVEN_METADATA_URL"
    exit 1
fi

if [[ -n "$unsupported_versions" ]]; then
    echo "Unsupported ktlint ruleset versions detected (published but not supported):"
    echo "$unsupported_versions"
    exit 1
fi

echo "All published ktlint ruleset versions are supported."

if [[ -n "$modules_missing_in_enum" ]]; then
    echo
    echo "Ruleset modules missing in KtlintRulesetVersion enum:"
    echo "$modules_missing_in_enum"
fi

if [[ -n "$enum_missing_in_modules" ]]; then
    echo
    echo "Ruleset versions in enum without a matching module (and no fallback):"
    echo "$enum_missing_in_modules"
fi
