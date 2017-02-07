#! /bin/bash

DIR_NAME="$(dirname $(dirname $0))"
CLI_JAR="$DIR_NAME/target/swamp-cli-jar-with-dependencies.jar"

[[ ! -f  "$CLI_JAR" ]] && echo "File not found: $CLI_JAR" && exit 1

JYTHON_JAR="$HOME/jython/jython/jython-standalone-2.7.0.jar"

[[ ! -f  "$JYTHON_JAR" ]] && echo "File not found: $JYTHON_JAR" && exit 1

CLASSPATH="$JYTHON_JAR:$CLI_JAR"

java -cp "$CLASSPATH" org.python.util.jython "$PWD/scripts/func_tests.py" "$@"
