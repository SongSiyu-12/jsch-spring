#!/usr/bin/env sh
# Minimal Maven Wrapper script for environments without preinstalled Maven.
# Downloads Apache Maven and executes it with all arguments passed to this script.

set -e

# Resolve script location
PRG="$0"
while [ -h "$PRG" ]; do
  ls=`ls -ld "$PRG"`
  link=`expr "$ls" : '.*-> \(.*\)$'`
  if expr "$link" : '/.*' > /dev/null; then
    PRG="$link"
  else
    PRG=`dirname "$PRG"`/"$link"
  fi
done
SAVED="`pwd`"
cd "`dirname \"$PRG\"`/"
BASEDIR="`pwd -P`"
cd "$SAVED"

MVN_VERSION="3.9.6"
DIST_URL="https://repo.maven.apache.org/maven2/org/apache/maven/apache-maven/${MVN_VERSION}/apache-maven-${MVN_VERSION}-bin.zip"
DIST_DIR="$BASEDIR/.mvn/dist"
INSTALL_DIR="$DIST_DIR/apache-maven-${MVN_VERSION}"
ZIP_FILE="$DIST_DIR/apache-maven-${MVN_VERSION}-bin.zip"

# Ensure directories exist
mkdir -p "$DIST_DIR"

# Download if not already downloaded
if [ ! -f "$ZIP_FILE" ]; then
  echo "Downloading Apache Maven ${MVN_VERSION}..." >&2
  if command -v curl >/dev/null 2>&1; then
    curl -fsSL "$DIST_URL" -o "$ZIP_FILE"
  elif command -v wget >/dev/null 2>&1; then
    wget -q "$DIST_URL" -O "$ZIP_FILE"
  else
    echo "ERROR: curl or wget is required to download Maven." >&2
    exit 1
  fi
fi

# Extract if not already extracted
if [ ! -x "$INSTALL_DIR/bin/mvn" ]; then
  echo "Extracting Maven..." >&2
  rm -rf "$INSTALL_DIR"
  mkdir -p "$INSTALL_DIR"
  # unzip preserves directory structure
  unzip -q -o "$ZIP_FILE" -d "$DIST_DIR"
fi

MVN_BIN="$INSTALL_DIR/bin/mvn"

if [ ! -x "$MVN_BIN" ]; then
  echo "ERROR: Maven binary not found at $MVN_BIN" >&2
  exit 1
fi

exec "$MVN_BIN" "$@"
