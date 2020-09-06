#!/bin/zsh

sbt clean
sbt assembly
sbt docker:publishLocal
docker image tag iotkafkarainreader:latest intel-server-03:5000/iotkafkarainreader
docker image push intel-server-03:5000/iotkafkarainreader
# Server side:
# kubectl apply -f /home/appuser/deployments/iotKafkaRainBucket.yaml
# If needed:
# kubectl delete deployment iot-kafka-windvane
# For troubleshooting
# kubectl exec --stdin --tty iot-kafka-windvane -- /bin/bash