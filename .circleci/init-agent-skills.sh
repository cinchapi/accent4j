#!/usr/bin/env bash
# Check out the pinned private build tools with the existing checkout identity
# or an optional read-only identity. Remove temporary key material on every
# exit.
set +x
set -euo pipefail
cd "$(dirname "$0")/.."

if [ -n "${CINCHAPI_AGENT_SKILLS_READ_KEY:-}" ]; then
    umask 077
    key_file=$(mktemp)
    trap 'rm -f "$key_file"' EXIT
    printf '%s\n' "$CINCHAPI_AGENT_SKILLS_READ_KEY" > "$key_file"
    if GIT_SSH_COMMAND="ssh -i '$key_file' -o IdentitiesOnly=yes -o StrictHostKeyChecking=yes" \
            git submodule update --init .agents; then
        :
    else
        printf '%s\n' 'The configured read key cannot fetch the pinned agent tools.' >&2
        exit 1
    fi
else
    if git submodule update --init .agents; then
        :
    else
        printf '%s\n' 'The checkout identity needs read access to cinchapi-agent-skills.' \
            'Set CINCHAPI_AGENT_SKILLS_READ_KEY to a dedicated read-only key if needed.' >&2
        exit 1
    fi
fi
