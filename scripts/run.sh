#! /bin/bash

DIR_NAME=$(dirname $(dirname $0))

[[ ! -f "$DIR_NAME/target/swamp-api-client-1.0-SNAPSHOT.jar" ]] && echo "File not found: $DIR_NAME/target/swamp-api-client-1.0-SNAPSHOT.jar" && exit 1

java -jar "$DIR_NAME/target/swamp-api-client-1.0-SNAPSHOT.jar" "$@"
