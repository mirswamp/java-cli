#! /bin/bash

DIR_NAME="$(dirname $(dirname $0))"
VERSION=$(mvn -Dexec.executable='echo' -Dexec.args='${project.version}' --non-recursive exec:exec -q)

JAR_FILE="$DIR_NAME/target/java-cli-$VERSION-jar-with-dependencies.jar"

[[ ! -f  "$JAR_FILE" ]] && echo "File not found: $JAR_FILE" && exit 1

java -jar "$JAR_FILE" "$@"

