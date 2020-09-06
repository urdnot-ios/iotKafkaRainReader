#!/usr/bin/env bash
export SCALA_MAJOR_VERSION='2.12'
export SCALA_VERSION='2.12.4'
export AKKA_VERSION='2.5.7'
export TYPESAFE_CONFIG_VERSION=1.3.1
export CLASSPATH=/var/lib/dependancies/akka-actor_${SCALA_MAJOR_VERSION}-${AKKA_VERSION}.jar:/var/lib/dependancies/akka-remote_${SCALA_MAJOR_VERSION}-${AKKA_VERSION}.jar:/var/lib/dependancies/config-$TYPESAFE_CONFIG_VERSION.jar:/var/application.conf
java -cp /home/appuser/lib/iotKafkaRainReader.jar:${CLASSPATH} com.urdnot.iot.KafkaReader