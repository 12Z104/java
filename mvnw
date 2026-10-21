#!/usr/bin/env sh
# OHCP 一键启动脚本（Linux / macOS）
set -e
MAVEN_VERSION=3.9.9
ROOT=$(cd "$(dirname "$0")" && pwd)
MVN_HOME="$ROOT/.mvn-dist/apache-maven-$MAVEN_VERSION"
MVN_CMD="$MVN_HOME/bin/mvn"

if [ ! -x "$MVN_CMD" ]; then
  echo "[OHCP] Downloading Apache Maven $MAVEN_VERSION ..."
  mkdir -p "$ROOT/.mvn-dist"
  ZIP="$ROOT/.mvn-dist/apache-maven-$MAVEN_VERSION-bin.zip"
  if [ ! -f "$ZIP" ]; then
    curl -L -o "$ZIP" "https://repo.maven.apache.org/maven2/org/apache/maven/apache-maven/$MAVEN_VERSION/apache-maven-$MAVEN_VERSION-bin.zip"
  fi
  (cd "$ROOT/.mvn-dist" && unzip -q -o "$ZIP")
fi
exec "$MVN_CMD" "$@"