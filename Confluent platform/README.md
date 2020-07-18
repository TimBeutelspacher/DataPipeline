# Confluent Platform - Kafka

## Kafka configuration

For this project we decided to install Confluent Platform because we wanted to be able to easily add multiple connectors. Also the Confluent Platform comes with everything thats needed (Zookeeper, Schema Registry, Kafka and Kafka-rest).
For this project we used one Kafka broker. In retrospect it would have been better to use multiple brokers to ensure a safe stream of the data. In the end we didnt have the time to change to a cluster of kafka brokers. Therefor our topic to stream the data to has also only an replication factor of one. We created it with the following command:

```
kafka-topics --zookeeper 127.0.0.1:2181 --topic btc_topic --create --partitions 1 --replication-factor 1
```

All the server properties can be found at the following location: https://github.com/TimBeutelspacher/DataPipeline/blob/master/Confluent%20platform/server.properties



## InfluxDB Sink Connector

Now when the data got from the JavaProducer into the Kafka topic, we want to stream the data live into a database to store it permanent. This stream gets realized through a sink connector to a timeseries database called "InfluxDB".

To install this connector you have run the following command:
```
confluent-hub install confluentinc/kafka-connect-influxdb:latest
```

After the connector itsself is installed the properties have to be configured. The topic has to be set to the topic the data shall be streamed from. Also the database which the data shall be streamed to must be given.
The whole code of the properties and the corresponding JSON file can be found at: https://github.com/TimBeutelspacher/DataPipeline/tree/master/Confluent%20platform



#### Lessons learned:

In future projects we will start with an architecture that includes more than only one Kafka broker to ensure the data is always available. If there are multiple Kafka brokers available, the topics could also have a replication factor thats higher than one.



