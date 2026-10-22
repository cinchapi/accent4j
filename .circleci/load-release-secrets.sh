#!/usr/bin/env bash

# Copyright (c) 2013-2026 Cinchapi Inc.
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
# http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.

# Export the release signing key and the Maven Central token from Google
# Secret Manager into the shell that sources this file:
#
#   source .circleci/load-release-secrets.sh && load_release_secrets
#
# Only a CircleCI job that uses the release-publishing context, on develop,
# master or a release/* branch, can read the secrets. Google checks the job's
# CircleCI identity token and refuses every other job. The values stay in
# memory: nothing is written to disk.

# The Google Cloud project that holds the release secrets
RELEASE_SECRETS_PROJECT=cinchapi-release

# The identity provider that trusts CircleCI's identity tokens
RELEASE_SECRETS_PROVIDER=//iam.googleapis.com/projects/166595785014/locations/global/workloadIdentityPools/circleci/providers/circleci-cinchapi

# The service account that may read the release secrets
RELEASE_SECRETS_ACCOUNT=release-publisher@cinchapi-release.iam.gserviceaccount.com

# Print the latest value of the named secret, using the provided access token.
release_secret() {
  local -
  set -o pipefail
  curl -fsS -H "Authorization: Bearer $1" \
    "https://secretmanager.googleapis.com/v1/projects/$RELEASE_SECRETS_PROJECT/secrets/$2/versions/latest:access" \
    | jq -er .payload.data | base64 -d
}

# Export ORG_GRADLE_PROJECT_signingKey, ORG_GRADLE_PROJECT_signingPassword,
# CENTRAL_TOKEN_USER and CENTRAL_TOKEN_PASS, or return non-zero if any of
# them cannot be read.
load_release_secrets() {
  local -
  set -o pipefail
  if [ -z "${CIRCLE_OIDC_TOKEN_V2:-}" ]; then
    echo "No CircleCI identity token is available to this job"
    return 1
  fi
  local federated
  federated=$(curl -fsS https://sts.googleapis.com/v1/token \
    --data-urlencode "grant_type=urn:ietf:params:oauth:grant-type:token-exchange" \
    --data-urlencode "audience=$RELEASE_SECRETS_PROVIDER" \
    --data-urlencode "scope=https://www.googleapis.com/auth/cloud-platform" \
    --data-urlencode "requested_token_type=urn:ietf:params:oauth:token-type:access_token" \
    --data-urlencode "subject_token_type=urn:ietf:params:oauth:token-type:jwt" \
    --data-urlencode "subject_token=$CIRCLE_OIDC_TOKEN_V2" \
    | jq -er .access_token) || { echo "Google refused this job's identity token"; return 1; }
  local access
  access=$(curl -fsS -X POST -H "Authorization: Bearer $federated" \
    -H "Content-Type: application/json" \
    -d '{"scope": ["https://www.googleapis.com/auth/cloud-platform"]}' \
    "https://iamcredentials.googleapis.com/v1/projects/-/serviceAccounts/$RELEASE_SECRETS_ACCOUNT:generateAccessToken" \
    | jq -er .accessToken) || { echo "Could not act as $RELEASE_SECRETS_ACCOUNT"; return 1; }
  local signing_key signing_password token_user token_pass
  signing_key=$(release_secret "$access" signing-key) \
    && signing_password=$(release_secret "$access" signing-key-passphrase) \
    && token_user=$(release_secret "$access" central-token-user) \
    && token_pass=$(release_secret "$access" central-token-pass) \
    || { echo "Could not read the release secrets"; return 1; }
  export ORG_GRADLE_PROJECT_signingKey="$signing_key"
  export ORG_GRADLE_PROJECT_signingPassword="$signing_password"
  export CENTRAL_TOKEN_USER="$token_user"
  export CENTRAL_TOKEN_PASS="$token_pass"
  echo "Loaded the release signing key and the Maven Central token"
}
