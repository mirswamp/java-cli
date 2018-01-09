#! /bin/bash

VERSION=$(mvn -Dexec.executable='echo' -Dexec.args='${project.version}' --non-recursive exec:exec -q)

function main {
    #DIR_NAME="$(dirname $(dirname $0))"

    if [ -n "$SWAMP_CLI_JAR" ] ; then
	local cli_jar="$SWAMP_CLI_JAR"
    else
	local cli_jar="$PWD/target/java-cli-${VERSION}-jar-with-dependencies.jar"
    fi

    [[ ! -f  "$cli_jar" ]] && echo "File not found: $cli_jar" && exit 1

    #JYTHON_JAR="$HOME/jython/jython/jython-standalone-2.7.0.jar"

    if [[ ! -f  "$JYTHON_JAR" ]] ; then
        echo "Requires Jython stand-alone Jar file, from http://www.jython.org/downloads.html download the latest Jython stand-alone jar and create an environment variable by the name JYTHON_JAR" \
        && exit 1
    fi
    
    CLASSPATH="$JYTHON_JAR:$cli_jar"

    echo java -cp "$CLASSPATH" org.python.util.jython "$PWD/scripts/func_tests.py" "$@"
    java -cp "$CLASSPATH" org.python.util.jython "$PWD/scripts/func_tests.py" "$@"

}

main  "$@"
