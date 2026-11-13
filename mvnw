#!/bin/sh
# ----------------------------------------------------------------------------
# Licensed to the Apache Software Foundation (ASF) under one
# or more contributor license agreements.  See the NOTICE file
# distributed with this work for additional information
# regarding copyright ownership.  The ASF licenses this file
# to you under the Apache License, Version 2.0 (the
# "License"); you may not use this file except in compliance
# with the License.  You may obtain a copy of the License at
#
#    http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing,
# software distributed under the License is distributed on an
# "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
# KIND, either express or implied.  See the License for the
# specific language governing permissions and limitations
# under the License.
# ----------------------------------------------------------------------------

# ----------------------------------------------------------------------------
# Apache Maven Wrapper startup batch script, version 3.3.2
#
# Optional ENV vars
# -----------------
#   JAVA_HOME - location of a JDK home dir, required when download maven via java source
#   MVNW_REPOURL - repo url base for downloading maven distribution
#   MVNW_USERNAME/MVNW_PASSWORD - user and password for downloading maven
#   MVNW_VERBOSE - true: enable verbose log; others: silence the output
# ----------------------------------------------------------------------------

set -euf
[ "${MVNW_VERBOSE-}" != true ] || set -x

# OS specific support.
native_path() { printf %s\\n "$1"; }
case "$(uname)" in
CYGWIN* | MINGW*)
  [ -z "${JAVA_HOME-}" ] || JAVA_HOME="$(cygpath --unix "$JAVA_HOME")"
  native_path() { cygpath --path --windows "$1"; }
  ;;
esac

# set JAVA_HOME to default if not set
if [ -z "${JAVA_HOME-}" ]; then
  if [ -x /usr/libexec/java_home ]; then
    JAVA_HOME="$(/usr/libexec/java_home)" || :
  fi
fi

# Determine the Java command to use to start the JVM.
if [ -n "${JAVA_HOME-}" ] ; then
  if [ -x "$JAVA_HOME/jre/sh/java" ] ; then
    # IBM's JDK on AIX uses strange locations for the executables
    JAVACMD="$JAVA_HOME/jre/sh/java"
  else
    JAVACMD="$JAVA_HOME/bin/java"
  fi
  if [ ! -x "$JAVACMD" ] ; then
    echo "ERROR: JAVA_HOME is set to an invalid directory: $JAVA_HOME" >&2
    echo "" >&2
    echo "Please set the JAVA_HOME variable in your environment to match the" >&2
    echo "location of your Java installation." >&2
    exit 1
  fi
else
  JAVACMD="$(\unset -f command 2>/dev/null; \command -v java)" || :
  if [ ! -x "${JAVACMD-}" ]; then
    echo "ERROR: JAVA_HOME is not set and no 'java' command could be found in your PATH." >&2
    echo "" >&2
    echo "Please set the JAVA_HOME variable in your environment to match the" >&2
    echo "location of your Java installation." >&2
    exit 1
  fi
fi

# hash://sha-256 of the wrapper jar
wrapperSha256Sum=""

# Determine the project base directory
MAVEN_PROJECTBASEDIR="${MAVEN_BASEDIR:-$(cd -- "$(dirname -- "$0")" && pwd -P)}"
MAVEN_WRAPPERDIR="$MAVEN_PROJECTBASEDIR/.mvn/wrapper"

# Extension to allow automatically downloading the maven-wrapper.jar from Maven-central
# This allows using the maven wrapper in projects that prohibit checking in binary data.
wrapperJar="$MAVEN_WRAPPERDIR/maven-wrapper.jar"
if [ -r "$MAVEN_WRAPPERDIR/maven-wrapper.properties" ]; then
  wrapperUrl="$(sed -n 's/^wrapperUrl=//p' "$MAVEN_WRAPPERDIR/maven-wrapper.properties" | tr -d '\r')"
  wrapperSha256Sum="$(sed -n 's/^wrapperSha256Sum=//p' "$MAVEN_WRAPPERDIR/maven-wrapper.properties" | tr -d '\r')"
fi

# If the wrapper jar doesn't exist, download it
if [ ! -r "$wrapperJar" ]; then
  if [ -n "${wrapperUrl-}" ]; then
    [ "${MVNW_VERBOSE-}" != true ] || echo "Downloading from: $wrapperUrl"

    if [ -n "${MVNW_USERNAME-}" ] && [ -n "${MVNW_PASSWORD-}" ]; then
      MVNW_AUTH="--user $MVNW_USERNAME:$MVNW_PASSWORD"
    else
      MVNW_AUTH=""
    fi

    if command -v wget > /dev/null 2>&1; then
      [ "${MVNW_VERBOSE-}" != true ] || echo "Found wget ... using wget"
      if [ -z "${MVNW_AUTH}" ]; then
        wget ${MVNW_VERBOSE:+--verbose} --output-document="$wrapperJar" "$wrapperUrl" -q
      else
        wget ${MVNW_VERBOSE:+--verbose} --output-document="$wrapperJar" $MVNW_AUTH "$wrapperUrl" -q
      fi
    elif command -v curl > /dev/null 2>&1; then
      [ "${MVNW_VERBOSE-}" != true ] || echo "Found curl ... using curl"
      if [ -z "${MVNW_AUTH}" ]; then
        curl ${MVNW_VERBOSE:+--verbose} --fail --location --output "$wrapperJar" "$wrapperUrl" -s
      else
        curl ${MVNW_VERBOSE:+--verbose} --fail --location --output "$wrapperJar" $MVNW_AUTH "$wrapperUrl" -s
      fi
    else
      echo "Falling back to using Java to download"
      javaSource="$MAVEN_WRAPPERDIR/MavenWrapperDownloader.java"
      javaClass="$MAVEN_WRAPPERDIR/MavenWrapperDownloader.class"
      if [ -r "$javaSource" ]; then
        if [ ! -r "$javaClass" ] || [ "$javaSource" -nt "$javaClass" ]; then
          echo " - Compiling MavenWrapperDownloader.java ..."
          ("$JAVACMD" -d "$MAVEN_WRAPPERDIR" "$javaSource")
        fi
        if [ -r "$javaClass" ]; then
          echo " - Running MavenWrapperDownloader.java ..."
          ("$JAVACMD" -cp "$MAVEN_WRAPPERDIR" MavenWrapperDownloader "$wrapperUrl" "$wrapperJar") || :
        fi
      fi
    fi

    if [ -n "${wrapperSha256Sum-}" ]; then
      if command -v sha256sum > /dev/null 2>&1; then
        actualSum="$(sha256sum "$wrapperJar" | cut -d' ' -f1)"
      elif command -v shasum > /dev/null 2>&1; then
        actualSum="$(shasum -a 256 "$wrapperJar" | cut -d' ' -f1)"
      else
        echo "WARNING: Could not verify maven-wrapper.jar SHA-256 checksum (no sha256sum or shasum command found)" >&2
        actualSum=""
      fi
      if [ -n "$actualSum" ] && [ "$actualSum" != "$wrapperSha256Sum" ]; then
        echo "ERROR: Failed to validate Maven wrapper SHA-256, your Maven wrapper might be compromised." >&2
        echo "Investigate or delete $wrapperJar to attempt a clean download." >&2
        echo "If you updated your Maven version, you need to update the specified wrapperSha256Sum property." >&2
        exit 1
      fi
    fi
  fi
fi
# End of extension

# If specified, validate the SHA-256 sum of the Maven distribution zip file
distributionSha256Sum=""
if [ -r "$MAVEN_WRAPPERDIR/maven-wrapper.properties" ]; then
  distributionSha256Sum="$(sed -n 's/^distributionSha256Sum=//p' "$MAVEN_WRAPPERDIR/maven-wrapper.properties" | tr -d '\r')"
fi

# Provide a "standardized" way to retrieve the CLI args that will
# work with both Windows and non-Windows executions.
MAVEN_CMD_LINE_ARGS="$*"
export MAVEN_CMD_LINE_ARGS

# Use the wrapper jar if it exists, otherwise download Maven directly
if [ -r "$wrapperJar" ]; then
  WRAPPER_LAUNCHER=org.apache.maven.wrapper.MavenWrapperMain
  # shellcheck disable=SC2086
  exec "$JAVACMD" \
    ${MAVEN_OPTS:+$MAVEN_OPTS} \
    ${MAVEN_DEBUG_OPTS:+$MAVEN_DEBUG_OPTS} \
    -classpath "$wrapperJar" \
    "-Dmaven.multiModuleProjectDirectory=${MAVEN_PROJECTBASEDIR}" \
    ${MAVEN_ARGS:+$MAVEN_ARGS} \
    $WRAPPER_LAUNCHER ${MAVEN_CONFIG:+$MAVEN_CONFIG} "$@"
else
  # Download Maven distribution directly
  distributionUrl="$(sed -n 's/^distributionUrl=//p' "$MAVEN_WRAPPERDIR/maven-wrapper.properties" | tr -d '\r')"
  mavenHome="${HOME}/.m2/wrapper/dists/apache-maven-$(echo "$distributionUrl" | sed 's/.*apache-maven-\([0-9.]*\).*/\1/')"

  if [ ! -d "$mavenHome" ]; then
    distributionZip="${mavenHome}.zip"
    mkdir -p "$(dirname "$distributionZip")"

    if command -v wget > /dev/null 2>&1; then
      wget --output-document="$distributionZip" "$distributionUrl" -q
    elif command -v curl > /dev/null 2>&1; then
      curl --fail --location --output "$distributionZip" "$distributionUrl" -s
    else
      echo "ERROR: Neither wget nor curl found to download Maven distribution" >&2
      exit 1
    fi

    if [ -n "${distributionSha256Sum-}" ]; then
      if command -v sha256sum > /dev/null 2>&1; then
        actualSum="$(sha256sum "$distributionZip" | cut -d' ' -f1)"
      elif command -v shasum > /dev/null 2>&1; then
        actualSum="$(shasum -a 256 "$distributionZip" | cut -d' ' -f1)"
      else
        actualSum=""
      fi
      if [ -n "$actualSum" ] && [ "$actualSum" != "$distributionSha256Sum" ]; then
        echo "ERROR: Failed to validate Maven distribution SHA-256" >&2
        exit 1
      fi
    fi

    mkdir -p "$mavenHome"
    if command -v unzip > /dev/null 2>&1; then
      unzip -q "$distributionZip" -d "$mavenHome" 2>/dev/null
    elif command -v jar > /dev/null 2>&1; then
      (cd "$mavenHome" && jar xf "$distributionZip")
    else
      echo "ERROR: Neither unzip nor jar found to extract Maven distribution" >&2
      exit 1
    fi
    # Move contents from nested directory
    mv "$mavenHome"/apache-maven-*/* "$mavenHome"/ 2>/dev/null || :
    rm -f "$distributionZip"
  fi

  exec "$mavenHome/bin/mvn" \
    ${MAVEN_OPTS:+$MAVEN_OPTS} \
    ${MAVEN_DEBUG_OPTS:+$MAVEN_DEBUG_OPTS} \
    "-Dmaven.multiModuleProjectDirectory=${MAVEN_PROJECTBASEDIR}" \
    ${MAVEN_ARGS:+$MAVEN_ARGS} \
    ${MAVEN_CONFIG:+$MAVEN_CONFIG} "$@"
fi
