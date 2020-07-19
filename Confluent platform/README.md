# Confluent Platform - Kafka

## Getting Started
For this project we decided to install Confluent Platform because we wanted to be able to easily add multiple connectors. Also, the Confluent Platform comes with everything that's needed (Zookeeper, Schema Registry, Kafka and Kafka-rest).
To install Confluent Platform, we used the following commands:

First we need the Confluent public key to sign the packages in the repository:
```
wget -qO - https://packages.confluent.io/deb/5.5/archive.key | sudo apt-key add -
```

Then we add the repository to the `/etc/apt/sources.list`:
```
sudo add-apt-repository "deb [arch=amd64] https://packages.confluent.io/deb/5.5 stable main"
```

After we added the Repository and the key to sign the packages we install Confluent Platform:
```
sudo apt-get update && sudo apt-get install confluent-platform-2.12
```



## Kafka configuration
For this project we used one Kafka broker. In retrospect it would have been better to use multiple brokers to ensure a safe stream of the data. In the end we didn't have the time to change to a cluster of Kafka brokers. Therefor our topic to stream the data to has also only a replication factor of one. We created it with the following command:

```
kafka-topics --zookeeper 127.0.0.1:2181 --topic btc_topic --create --partitions 1 --replication-factor 1
```

All the server properties can be found at the following location: https://github.com/TimBeutelspacher/DataPipeline/blob/master/Confluent%20platform/server.properties



## InfluxDB Sink Connector

Now when the data got from the JavaProducer into the Kafka topic, we want to stream the data live into a database to store it permanent. This stream gets realized through a sink connector to a timeseries database called "InfluxDB".

To install this connector, you have run the following command:
```
confluent-hub install confluentinc/kafka-connect-influxdb:latest
```

After the connector itself is installed the properties have to be configured. The topic must be set to the topic the data shall be streamed from. Also, the database which the data shall be streamed to must be given.
The whole code of the properties and the corresponding JSON file can be found at: https://github.com/TimBeutelspacher/DataPipeline/tree/master/Confluent%20platform



#### Lessons learned:

In future projects we will start with an architecture that includes more than only one Kafka broker to ensure the data is always available. If there are multiple Kafka brokers available, the topics could also have a replication factor that's higher than one.



