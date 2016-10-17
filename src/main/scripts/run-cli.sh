HOME=/home/ncsa/dev/swamp-git/ncsa-swamp/swamp-java-api-example/target
cliJar=$HOME/swamp-cli.jar

java -Done-jar.silent=true -jar $cliJar $@

if [ $? != 0 ]; then
  exit 1
fi

exit 0
