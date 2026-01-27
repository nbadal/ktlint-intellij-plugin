#!/usr/bin/env bash
set -euf -o pipefail

# Minimum ktlint ruleset version to consider. Ignores versions below this.
MIN_VERSION="1.0.0"

# Comma-separated list of published versions to ignore (e.g., "8.8.8,9.9.9")
IGNORED_VERSIONS=""

MAVEN_METADATA_URL="https://repo1.maven.org/maven2/com/pinterest/ktlint/ktlint-ruleset-standard/maven-metadata.xml"

# Check for required binaries.
missing_cmds=0
for cmd in curl rg awk sort comm find sed; do
    if ! command -v "$cmd" &> /dev/null; then
        echo "Missing required command: $cmd"
        missing_cmds=$((missing_cmds + 1))
    fi
done

if [[ $missing_cmds -gt 0 ]]; then
    exit 1
fi

# Temp file to pass module versions into awk without embedding newlines in vars.
tmp_mods=$(mktemp)
trap 'rm -f "$tmp_mods"' EXIT

# Compares two semantic versions. Returns 1 if left >= right, else 0.
version_ge() {
    local left=$1 right=$2
    local left_major=0 left_minor=0 left_patch=0
    local right_major=0 right_minor=0 right_patch=0

    IFS='.' read -r left_major left_minor left_patch <<< "$left"
    IFS='.' read -r right_major right_minor right_patch <<< "$right"

    if [[ ${left_major:-0} -gt ${right_major:-0} ]]; then
        echo 1
        return
    elif [[ ${left_major:-0} -lt ${right_major:-0} ]]; then
        echo 0
        return
    fi

    if [[ ${left_minor:-0} -gt ${right_minor:-0} ]]; then
        echo 1
        return
    elif [[ ${left_minor:-0} -lt ${right_minor:-0} ]]; then
        echo 0
        return
    fi

    if [[ ${left_patch:-0} -ge ${right_patch:-0} ]]; then
        echo 1
        return
    fi

    echo 0
}

# Normalize ignored versions into a sorted, newline-delimited list.
ignored_list=$(echo "$IGNORED_VERSIONS" | tr ',' '\n' | sed '/^$/d' | sort -u)

# Fetch published versions from Maven metadata, drop pre-release tags, and apply MIN_VERSION.
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

# Remove explicitly ignored versions from the published list.
published_versions=$(
    comm -23 <(echo "$published_versions") <(echo "$ignored_list")
)

# Read supported versions from module folder names (ruleset-1-2-3 -> 1.2.3).
module_versions=$(
    find ktlint-lib -maxdepth 1 -type d -name "ruleset-*" -print \
        | sed 's#.*/ruleset-##' \
        | sed 's/-/./g' \
        | sort -u
)

echo "$module_versions" > "$tmp_mods"

# Capture enum entry lines (e.g., V1_7_0(...)) for parsing and fallback detection.
enum_lines=$(
    rg -n "^\s*V[0-9]+(_[0-9]+)+\(" ktlint-lib/src/main/kotlin/com/pinterest/ktlint/ruleset/standard/KtlintRulesetVersion.kt
)

# Extract enum versions from the lines above (V1_7_0 -> 1.7.0).
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

# Union of supported versions from modules and enum.
supported_versions=$(
    {
        echo "$module_versions"
        echo "$enum_versions"
    } | sort -u
)

# Versions published in Maven that are not supported by either source.
unsupported_versions=$(comm -23 <(echo "$published_versions") <(echo "$supported_versions"))

# Versions that have modules but are missing in the enum.
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
