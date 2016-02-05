#!/usr/bin/env bash

if [ "$JAVA_1_8_HOME" != "" ]; then
  JAVA_HOME=$JAVA_1_8_HOME
fi
echo "Using Java Home $JAVA_HOME"

git clean -xdf
  
echo "Downloading docker-compose"
mkdir -p .bin
curl -L https://github.com/docker/compose/releases/download/1.5.1/docker-compose-`uname -s`-`uname -m` > .bin/docker-compose
chmod +x .bin/docker-compose
echo "docker-compose downloaded"
  
DOCKER_COMPOSE_LOCATION=`pwd .bin/docker-compose` 
echo "Using docker-compose $DOCKER_COMPOSE_LOCATION"
./gradlew build publish --info --stacktrace -Dhttps.protocols=TLSv1,TLSv2

