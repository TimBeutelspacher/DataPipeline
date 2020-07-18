# JavaProducer


## Procedure

The JavaProducer requests the latest blockhash from the PostgreSQL database every 20 seconds. The latest blockhash send to the Kafka topic gets saved in the class aswell. By comparing these two hashes the JavaProducer knows if the requested hash refers to a new block on the Bitcoin blockchain. If thats not the case, a new request will be sent after 20 second. Otherwise a request to the endpoint `https://api.blockcypher.com/v1/btc/main/blocks/$BLOCKHASH$` will be send. After the JavaProducer received all the detailled information about the block, the time difference between that block and the previous block gets calculated. Then all the attribute we need are ready to be set into a generic record. When all the attributes have been set to the record, the producer will send this record with the blockheight as a key to the given Kafka topic.

`producer.send(new ProducerRecord<Integer, GenericRecord>(topic, blockHeight, record));`

After a record has been sent, the JavaProducer again waits 20 seconds before creating a new request to the PostgreSQL database.

The complete code incl. comments can be found at: https://github.com/TimBeutelspacher/DataPipeline/blob/master/JavaCode/src/main/java/JonasFelixTim/JavaProducer.java



## Kafka properties

The main purpose of this JavaProducer is to send data to a Kafka topic on our virtual machine. To be able to do so, we create an instance of the imported `<Producer>` class. To create such an instance we need to set proper properties:
key & value serializer = `io.confluent.kafka.serializers.KafkaAvroSerializer`
schema registry URL = `http://193.196.54.92:8081`
bootstrap server = `193.196.54.92:9092`

Especially the serializers played a very important role in making the Producer working correctly. 



## Avro schema

To be able to send a generic record with multiple attributes and a key to a Kafka topic we needed to set a fixed schema. This schema is set in a `.avsc`-file and contains all the names and data types of the attributes we want to set when sending a record to the Kafka topic. The attributes and their data types we needed to set are the following:
* `"name":"blockhash","type":"string"`
* `"name":"n_tx","type":"int"`
* `"name":"blockheight","type":"int"`
* `"name":"totalVol","type":"long"`
* `"name":"totalFees","type":"long"`
* `"name":"secondsSincePrevBlock","type":"long"`
* `"name":"size","type":"int"`

The whole schema can be found at https://github.com/TimBeutelspacher/DataPipeline/blob/master/JavaCode/src/main/resources/schemes/btc_schema.avsc .



## Applied Programming Interface

We are using an API called "BlockCypher". This is a very powerful API with a lot of information about many different blockchains. We use the endpoint of the Bitcoin blockchain.
For the JavaProducer we basically send 2 different requests.
1. First time execution
  When we execute the code for the very first time, there is no timestamp from the previous block. To be able to calculate the time between the latest two blocks, we send a request to the API to get the detailled information about the previous block. After we got all the information we needed, the time difference between the latest two blocks can be calculated. 
  
2. Detailled Blockinformation
  After we requested a new blockhash from the PostgreSQL database, we need to get more information about the newest block. To do so, we send a request to the following endpoint: `https://api.blockcypher.com/v1/btc/main/blocks/$BLOCKHASH$`
  
