#! /bin/bash
java -Xdebug -agentlib:jdwp=transport=dt_socket,address=8000,server=y,suspend=y -jar target/swamp-api-client-1.0-SNAPSHOT.jar "$@"
