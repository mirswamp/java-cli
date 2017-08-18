#! /bin/bash

function main {
    #DIR_NAME="$(dirname $(dirname $0))"
    local cli_jar="$PWD/target/swamp-cli-jar-with-dependencies.jar"

    [[ ! -f  "$cli_jar" ]] && echo "File not found: $cli_jar" && exit 1

    #JYTHON_JAR="$HOME/jython/jython/jython-standalone-2.7.0.jar"

    if [[ ! -f  "$JYTHON_JAR" ]]; then
        echo "Requires Jython stand-alone Jar file, from http://www.jython.org/downloads.html download the latest Jython stand-alone jar and create an environment variable by the name JYTHON_JAR" \
        && exit 1
    fi
    
    CLASSPATH="$JYTHON_JAR:$cli_jar"

    echo java -cp "$CLASSPATH" org.python.util.jython "$PWD/scripts/func_tests.py" "$@"
    java -cp "$CLASSPATH" org.python.util.jython "$PWD/scripts/func_tests.py" "$@"

}

main  "$@"
