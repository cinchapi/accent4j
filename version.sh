# Cinchapi Inc. CONFIDENTIAL
# Copyright (c) 2016 Cinchapi Inc. All Rights Reserved.
#
# All information contained herein is, and remains the property of Cinchapi.
# The intellectual and technical concepts contained herein are proprietary to
# Cinchapi and may be covered by U.S. and Foreign Patents, patents in process,
# and are protected by trade secret or copyright law. Dissemination of this
# information or reproduction of this material is strictly forbidden unless
# prior written permission is obtained from Cinchapi. Access to the source code
# contained herein is hereby forbidden to anyone except current Cinchapi
# employees, managers or contractors who have executed Confidentiality and
# Non-disclosure agreements explicitly covering such access.
#
# The copyright notice above does not evidence any actual or intended
# publication or disclosure of this source code, which includes information
# that is confidential and/or proprietary, and is a trade secret, of Cinchapi.
#
# ANY REPRODUCTION, MODIFICATION, DISTRIBUTION, PUBLIC PERFORMANCE, OR PUBLIC
# DISPLAY OF OR THROUGH USE OF THIS SOURCE CODE WITHOUT THE EXPRESS WRITTEN
# CONSENT OF COMPANY IS STRICTLY PROHIBITED, AND IN VIOLATION OF APPLICABLE
# LAWS AND INTERNATIONAL TREATIES. THE RECEIPT OR POSSESSION OF THIS SOURCE
# CODE AND/OR RELATED INFORMATION DOES NOT CONVEY OR IMPLY ANY RIGHTS TO
# REPRODUCE, DISCLOSE OR DISTRIBUTE ITS CONTENTS, OR TO MANUFACTURE, USE, OR
# SELL ANYTHING THAT IT MAY DESCRIBE, IN WHOLE OR IN PART.

#################################################
###  Script to get or set the current version ###
#################################################

#Ensure that this script operates from the directory in which it resides
cd "$(dirname "$0")"

# Bootstrap Git's ability to give the branch, if necessary
git rev-parse --abbrev-ref HEAD > /dev/null 2>&1
if [ $? -ne 0 ]; then
  git commit --allow-empty -m "Bootstrapping version.sh"
fi

BASE_VERSION_FILE=".version"
JENKINS_HOME="/opt/jenkins"
if [ -w $JENKINS_HOME ]; then
  COUNTER_FILE="$JENKINS_HOME/.counter"
else
  COUNTER_FILE=".counter"
fi

if [ -z "$1" ] ; then
  VERSION=`cat $BASE_VERSION_FILE`
  BRANCH=`git rev-parse --abbrev-ref HEAD`
  COMMIT=`git rev-parse HEAD | cut -c1-10`

  # Get counter value
  if [ ! -f $COUNTER_FILE ] ; then
    echo 0 > $COUNTER_FILE
  fi
  if [ -z ${CONTAINER_BUILD+x} ]; then
    COUNTER=`cat $COUNTER_FILE`
    ((COUNTER++))
  else
    # If the build is run in a container, the COUNTER_FILE will be wiped
    # away on each build, so we'll use the current unix timestamp for the
    # counter.
    COUNTER=`date +%s`
  fi
  echo $COUNTER > $COUNTER_FILE
  VERSION=$VERSION.$COUNTER
  case $BRANCH in
    develop )
      EXTRA="-SNAPSHOT"
      ;;
    feature* )
      IFS='/'
      PARTS=( $BRANCH )
      EXTRA="-${PARTS[1]}"
      EXTRA=`echo $EXTRA | tr '[:lower:]' '[:upper:]'`
      ;;
    release* )
      EXTRA=""
      ;;
    * )
      # At this point we do not need to refer
      # to any commit hash since we'll have a
      # tag that represents the overall release
      EXTRA=""
      ;;
  esac
  echo $VERSION$EXTRA
else
  NEW_VERSION=$1
  if [[ $NEW_VERSION =~ ^[0-9]+\.[0-9]+\.[0-9]+$ ]] ; then
    echo $NEW_VERSION > $BASE_VERSION_FILE
    rm $COUNTER_FILE 2>/dev/null
    sed -i '' -E "s/[0-9]+\.[0-9]+\.[0-9]+/$NEW_VERSION/g" README.md

    echo "The version has been set to $NEW_VERSION"
  else
    echo "Please specify a valid version <major>.<minor>.<patch>"
    exit 1
  fi
fi
exit 0
