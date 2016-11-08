
JYTHON_PATH="$HOME/Tools/Jython/jython-standalone-2.7.0.jar"
CLASSPATH="$JYTHON_PATH:$PWD/target/swamp-api-client-1.0-SNAPSHOT.jar"

java -cp "$CLASSPATH" org.python.util.jython "$PWD/scripts/func_tests.py" "$@"
