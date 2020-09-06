# iotKafkaRainSensor

The source data flows from an outdoor rain bucket sensor to a Kafka topic. This code picks it up from there, verifies and parses it, then loads it in an InfluxDB for consumption.