#!/usr/bin/env bash
# Copied to a repo's version.sh by .agents/setup.sh on every skills pull.
#
# Print the build version of the repo that holds this script, convert a build
# version to the version it publishes as, or set the repo's semantic version.
#
# Usage:
#   version.sh [--semantic | --calendar]
#   version.sh --published BUILD_VERSION
#   version.sh [--semantic] X.Y.Z
#   version.sh --help
#
# A repo with a .version file uses semantic versions, and any other repo uses
# calendar versions. --semantic or --calendar chooses the scheme instead.
#
# A build version is a base, a build number, and a suffix for the branch. A
# semantic base is the X.Y.Z in .version. Its build number is the epoch second
# when CONTAINER_BUILD is set, as it is in CI, and otherwise the next count in
# the repo's .counter. A calendar base is the day as YYYY.MM.DD, and its build
# number is the epoch second. The suffix depends on the branch:
#
#   master       no suffix           1.2.0.7
#   release/*    -rc                 1.2.0.7-rc
#   develop      -SNAPSHOT           1.2.0.7-SNAPSHOT
#   any other    its whole name      1.2.0.7-FEATURE-GH-60-FIX
#
# Any other branch's name is uppercased, with each run of other characters as
# one dash. It is cut only as far as keeps the build version within the 128
# characters of a Docker tag. A detached checkout is named HEAD, and a build
# outside Git is named BUILD. The branch is the first of CIRCLE_BRANCH,
# GITHUB_HEAD_REF, GITHUB_REF_NAME, BRANCH_NAME, CI_COMMIT_BRANCH, and
# GIT_BRANCH that is set and not empty, or else Git's.
#
# --published prints the version that a build publishes as: the build version
# without its build number, except that a -rc build carries the number as
# -rc<n>, so each release candidate is a distinct version. A version with no
# build number prints unchanged. It reads and writes no file, so it runs from
# anywhere, as tools/version too.
#
# X.Y.Z sets the version of a semantic repo. It first runs the repo's
# .on-version-set.sh script, if there is one, with bash from the repo's root,
# with the new and the old version as its arguments, so the repo can rewrite the
# other files that name its version. The script must be a bash script. The old
# version is empty when the repo had none. Then it writes .version and removes
# .counter. When the script fails, the set stops with the script's status:
# .version and .counter stay as they were, and the script's own edits stay in
# place.
#
# A build or a set works on the directory that holds the script, so it runs only
# as a repo's version.sh. Bad usage and bad input exit 2 before anything
# changes, and a failed .on-version-set.sh script exits with its own status.
# Needs bash 3.2, git, and stock macOS or Linux tools.
set -euo pipefail

# Docker caps a tag at 128 characters, and builds tag images with their version.
readonly MAX_VERSION_LENGTH=128
readonly RE_SEMANTIC='^[0-9]+\.[0-9]+\.[0-9]+$'
readonly RE_BUILD='^([0-9]+\.[0-9]+\.[0-9]+)\.([0-9]+)(-[^[:space:]]+)?$'
readonly RE_PUBLISHED='^[0-9]+\.[0-9]+\.[0-9]+(-[^[:space:]]+)?$'
readonly RE_COUNT='^[0-9]{1,15}$'

# Prints message $1 and exits 2.
die() {
  printf 'version.sh: %s\n' "$1" >&2
  exit 2
}

# Prints the usage from this script's header.
usage() { awk 'NR > 3 && !/^#/ { exit } NR > 3 { sub(/^# ?/, ""); print }' "$0"; }

# Prints the version that build version $1 publishes as. A version with no build
# number prints unchanged, and input that is no version exits 2.
published_version() {
  if [[ $1 =~ $RE_BUILD ]]; then
    if [ "${BASH_REMATCH[3]:-}" = -rc ]; then
      printf '%s-rc%s\n' "${BASH_REMATCH[1]}" "${BASH_REMATCH[2]}"
    else
      printf '%s%s\n' "${BASH_REMATCH[1]}" "${BASH_REMATCH[3]:-}"
    fi
  elif [[ $1 =~ $RE_PUBLISHED ]]; then
    printf '%s\n' "$1"
  else
    die "'$1' is not a version"
  fi
}

# Prints the X.Y.Z in .version, or exits 2 when .version is missing or holds
# something else.
semantic_base() {
  local base
  if [ -f .version ]; then
    base=$(tr -d '[:space:]' <.version)
  else
    die 'a semantic version needs a .version file'
  fi
  if [[ $base =~ $RE_SEMANTIC ]]; then
    printf '%s\n' "$base"
  else
    die ".version must hold X.Y.Z, not '$base'"
  fi
}

# Prints the next count in .counter and stores it there. A missing counter, or
# one that holds no count, starts again at 1.
next_count() {
  local n=""
  if [ -f .counter ]; then
    n=$(tr -d '[:space:]' <.counter)
  fi
  if [[ $n =~ $RE_COUNT ]]; then
    n=$((10#$n + 1))
  else
    n=1
  fi
  printf '%s\n' "$n" >.counter
  printf '%s\n' "$n"
}

# Prints the branch this build is for: the CI's, else Git's, with HEAD for a
# detached checkout, and nothing outside Git.
branch_name() {
  local ci=${CIRCLE_BRANCH:-${GITHUB_HEAD_REF:-${GITHUB_REF_NAME:-}}}
  ci=${ci:-${BRANCH_NAME:-${CI_COMMIT_BRANCH:-${GIT_BRANCH:-}}}}
  if [ -n "$ci" ]; then
    printf '%s\n' "$ci"
  elif git rev-parse --git-dir >/dev/null 2>&1; then
    git symbolic-ref --quiet --short HEAD || printf '%s\n' HEAD
  else
    printf '\n'
  fi
}

# Prints the suffix that branch $1 gives a build version whose part before the
# suffix is $2 characters long.
suffix_for() {
  local name room
  case "$1" in
    master) printf '\n' ;;
    release/*) printf '%s\n' -rc ;;
    develop) printf '%s\n' -SNAPSHOT ;;
    *)
      name=$(printf '%s\n' "$1" | LC_ALL=C tr '[:lower:]' '[:upper:]' |
        LC_ALL=C sed -E 's/[^A-Z0-9]+/-/g; s/^-+//; s/-+$//')
      name=${name:-BUILD}
      room=$((MAX_VERSION_LENGTH - $2 - 1))
      name=$(printf '%s\n' "${name:0:$room}" | sed -E 's/-+$//')
      printf -- '-%s\n' "$name"
      ;;
  esac
}

# Prints the next build version of the repo in the current directory under
# scheme $1, semantic or calendar.
build_version() {
  local base number stamp suffix
  if [ "$1" = semantic ]; then
    base=$(semantic_base)
    if [ -n "${CONTAINER_BUILD+x}" ]; then
      number=$(date +%s)
    else
      number=$(next_count)
    fi
  else
    # One reading of the clock keeps the day and the second from straddling
    # midnight.
    stamp=$(date +%Y.%m.%d.%s)
    base=${stamp%.*}
    number=${stamp##*.}
  fi
  suffix=$(suffix_for "$(branch_name)" $((${#base} + 1 + ${#number})))
  printf '%s.%s%s\n' "$base" "$number" "$suffix"
}

# Sets the repo in the current directory to semantic version $1, after the
# repo's .on-version-set.sh script rewrites the other files that name it. Writes
# .version, removes .counter, and prints a confirmation. When the script fails,
# exits with its status and leaves .version and .counter as they were.
set_version() {
  local old=""
  if [ -f .version ]; then
    old=$(tr -d '[:space:]' <.version)
  fi
  if [ -f .on-version-set.sh ]; then
    bash ./.on-version-set.sh "$1" "$old"
  fi
  printf '%s\n' "$1" >.version
  rm -f .counter
  printf 'The version has been set to %s\n' "$1"
}

scheme=""
publish=""
published=""
new=""
help=""
while [ $# -gt 0 ]; do
  case "$1" in
    --semantic|--calendar)
      if [ -n "$scheme" ]; then
        die 'give one of --semantic and --calendar'
      else
        scheme=${1#--}
        shift
      fi
      ;;
    --published)
      if [ $# -lt 2 ]; then
        die '--published needs a build version'
      else
        publish=1
        published=$2
        shift 2
      fi
      ;;
    -h|--help)
      help=1
      shift
      ;;
    -*)
      die "unknown option $1"
      ;;
    *)
      if [ -n "$new" ]; then
        die 'give one version to set'
      else
        new=$1
        shift
      fi
      ;;
  esac
done

if [ -n "$help" ]; then
  usage
elif [ -n "$publish" ]; then
  if [ -n "$scheme$new" ]; then
    die '--published takes nothing else'
  else
    published_version "$published"
  fi
elif [ "$(basename "$0")" != version.sh ]; then
  die "run the repo's version.sh, which works on the directory that holds it"
else
  cd "$(dirname "$0")"
  if [ -z "$scheme" ]; then
    if [ -f .version ]; then
      scheme=semantic
    else
      scheme=calendar
    fi
  fi
  if [ -z "$new" ]; then
    build_version "$scheme"
  elif ! [[ $new =~ $RE_SEMANTIC ]]; then
    die "set a version as X.Y.Z, not '$new'"
  elif [ "$scheme" != semantic ]; then
    die 'only a semantic version can be set; --semantic X.Y.Z starts one'
  else
    set_version "$new"
  fi
fi
