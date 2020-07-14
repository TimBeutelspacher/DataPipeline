package JonasFelixTim;

import io.confluent.kafka.schemaregistry.client.CachedSchemaRegistryClient;
import io.confluent.kafka.schemaregistry.client.rest.exceptions.RestClientException;
import io.confluent.kafka.serializers.AbstractKafkaAvroSerDeConfig;
import org.apache.avro.Schema;
import org.apache.avro.generic.GenericData;
import org.apache.avro.generic.GenericRecord;
import org.apache.commons.io.IOUtils;
import org.apache.kafka.clients.producer.*;
import org.apache.kafka.common.serialization.StringSerializer;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import scala.Int;

import java.io.FileInputStream;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.sql.Timestamp;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Properties;
import java.util.Scanner;
import java.util.concurrent.TimeUnit;

public class ProducerAPI {

    public static void main(String[] args) throws IOException, InterruptedException, RestClientException, ParseException {

        // Kafka variables
        String bootstrapServers = "193.196.54.92:9092";
        Logger logger = LoggerFactory.getLogger(ProducerAPI.class);
        String topic = "btc";
        int userIdInt = 1;
        GenericRecord recordGen;
        GenericData.Record record;

        // Schema variables
        String schemaRegURL = "http://193.196.54.92:8081";
        String schemaPath = System.getProperty("user.dir") + "/btc_schema.avsc";
        //String schemaPath = System.getProperty("user.dir")+"/src/main/resources/schemes/btc_schema.avsc";
        String schemaString = null;

        // subject convention is "<topic-name>-value"
        String subject = topic + "-value";

        // avsc json string
        String schema = null;

        String latestBlockHash = "";
        String inline = "";
        int responseCode;
        URL latestBlockHashURL = new URL("https://api.blockcypher.com/v1/btc/main");
        String basicBlockURLString = "https://api.blockcypher.com/v1/btc/main/blocks/";
        URL singleBlockURL;
        URL prevBlockURL;
        String output;

        // Output values
        String blockhash;
        int n_tx;
        int blockHeight;
        long secondsSincePrevBlock = 0;
        int size;
        long totalVol;
        long totalFees;

        long latestBlockTime = 0;
        long currentBlockTime = 0;

        DateFormat formatter = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");

        FileInputStream inputStream = new FileInputStream(schemaPath);
        try {
            schema = IOUtils.toString(inputStream, "UTF-8");
        } finally {
            inputStream.close();
        }

        Schema avroSchema = new Schema.Parser().parse(schema);

        CachedSchemaRegistryClient client = new CachedSchemaRegistryClient(schemaRegURL, 20);
        client.register(subject, avroSchema);

        logger.info("Setting up producer");

        // Set Producer properties
        Properties properties = new Properties();
        properties.setProperty(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        properties.setProperty(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, "io.confluent.kafka.serializers.KafkaAvroSerializer");
        properties.setProperty(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, "io.confluent.kafka.serializers.KafkaAvroSerializer");
        properties.setProperty(AbstractKafkaAvroSerDeConfig.SCHEMA_REGISTRY_URL_CONFIG, schemaRegURL);

        // Create the producer
        Producer<Integer, GenericRecord> producer = new KafkaProducer<Integer, GenericRecord>(properties);

        logger.info("Producer working | Topic: " + topic);

        /*
            Get the Hash of the latest Block the information from the API
         */

        // infinite loop to catch data
        while (true) {

            logger.info(new Timestamp(System.currentTimeMillis()).getTime() + " - Requesting newest blockhash.");

            // Configure Connection to "latest Hash-API"
            HttpURLConnection latestBlockHashCon = (HttpURLConnection) latestBlockHashURL.openConnection();
            latestBlockHashCon.setRequestMethod("GET");
            latestBlockHashCon.connect();
            responseCode = latestBlockHashCon.getResponseCode();

            // checking the response and getting it into a String
            if (responseCode != 200) {
                throw new RuntimeException("HttpResponseCode: " + responseCode);
            } else {
                Scanner sc = new Scanner(latestBlockHashURL.openStream());
                inline = "";
                while (sc.hasNext()) {
                    inline += sc.nextLine();
                }
                sc.close();
            }

            latestBlockHashCon.disconnect();

            // Convert String into a JSON Object
            JSONObject latestBlockHashJsonObject = new JSONObject(inline);

            // if the requested blockhash is different to the stored one, then its a new one
            if (!latestBlockHash.equals(latestBlockHashJsonObject.get("hash").toString())) {

                logger.info(new Timestamp(System.currentTimeMillis()).getTime() + " - New Block detected: " + latestBlockHashJsonObject.get("hash").toString());

                // Set the new requested blockhash as the newest stored
                latestBlockHash = latestBlockHashJsonObject.get("hash").toString(); //.substring(1, latestBlockHashJsonObject.get("hash").toString().length()-1);

                // Request "BlockAPI" with all informations
                singleBlockURL = new URL(basicBlockURLString + latestBlockHash);
                logger.info("Requested URL: " + singleBlockURL.toString());

                // Configure Connection to "latest Hash-API"
                HttpURLConnection singleBlockCon = (HttpURLConnection) singleBlockURL.openConnection();
                singleBlockCon.setRequestMethod("GET");
                singleBlockCon.connect();
                responseCode = singleBlockCon.getResponseCode();

                logger.info(new Timestamp(System.currentTimeMillis()).getTime() + " - Sent request to single block " + latestBlockHash);

                // Convert response to String
                if (responseCode != 200) {
                    throw new RuntimeException("HttpResponseCode: " + responseCode);
                } else {
                    Scanner sc = new Scanner(singleBlockURL.openStream());
                    inline = "";
                    while (sc.hasNext()) {
                        inline += sc.nextLine();
                    }
                    sc.close();
                }

                singleBlockCon.disconnect();

                // Convert String into a JSON Object (using Gson)
                JSONObject singleBlockHashJsonObject = new JSONObject(inline);

                // Create record
                record = new GenericData.Record(avroSchema);

                // calculate seconds between Blocks
                if (latestBlockTime != 0) {
                    currentBlockTime = formatter.parse(singleBlockHashJsonObject.get("time").toString()).getTime() / 1000;
                    secondsSincePrevBlock = currentBlockTime - latestBlockTime;
                } else {

                    // Request "BlockAPI" with all informations
                    prevBlockURL = new URL(basicBlockURLString + singleBlockHashJsonObject.get("prev_block").toString());
                    logger.info("Requested previous block URL: " + prevBlockURL.toString());

                    // Configure Connection to "latest Hash-API"
                    HttpURLConnection prevBlockCon = (HttpURLConnection) prevBlockURL.openConnection();
                    prevBlockCon.setRequestMethod("GET");
                    prevBlockCon.connect();
                    responseCode = prevBlockCon.getResponseCode();

                    logger.info(new Timestamp(System.currentTimeMillis()).getTime() + " - Sent request to previous block " + latestBlockHash);

                    // Convert response to String
                    if (responseCode != 200) {
                        throw new RuntimeException("HttpResponseCode: " + responseCode);
                    } else {
                        Scanner sc = new Scanner(prevBlockURL.openStream());
                        inline = "";
                        while (sc.hasNext()) {
                            inline += sc.nextLine();
                        }
                        sc.close();
                    }

                    prevBlockCon.disconnect();

                    // Convert String into a JSON Object (using Gson)
                    JSONObject prevBlockHashJsonObject = new JSONObject(inline);

                    latestBlockTime = formatter.parse(prevBlockHashJsonObject.get("time").toString()).getTime() / 1000;
                    currentBlockTime = formatter.parse(singleBlockHashJsonObject.get("time").toString()).getTime() / 1000;
                    secondsSincePrevBlock = currentBlockTime - latestBlockTime;
                }


                latestBlockTime = formatter.parse(singleBlockHashJsonObject.get("time").toString()).getTime() / 1000;

                // Set attributes got from the API
                blockhash = singleBlockHashJsonObject.get("hash").toString();
                n_tx = (int) singleBlockHashJsonObject.get("n_tx");
                blockHeight = (int) singleBlockHashJsonObject.get("height");
                size = (int) singleBlockHashJsonObject.get("size");
                totalVol = (long) singleBlockHashJsonObject.get("total");
                totalFees = (int) singleBlockHashJsonObject.get("fees");


                // put the elements according to the avro schema.
                record.put("blockhash", blockhash);
                record.put("n_tx", n_tx);
                record.put("blockheight", blockHeight);
                record.put("totalVol", totalVol);
                record.put("totalFees", totalFees);
                record.put("secondsSincePrevBlock", secondsSincePrevBlock);
                record.put("size", size);


                // Send data to kafka

                logger.info(new Timestamp(System.currentTimeMillis()).getTime() + " - Sending data to kafka.");


                // send avro message to the topic page-view-event.
                producer.send(new ProducerRecord<Integer, GenericRecord>(topic, null, record));

                logger.info(new Timestamp(System.currentTimeMillis()).getTime() + " - Closing kafka producer.");

                // flush and close producer
                producer.flush();
            } else {
                logger.info(new Timestamp(System.currentTimeMillis()).getTime() + " - The requested blockhash (" + latestBlockHashJsonObject.get("hash").toString() + ") is not new.");
            }

            // Wait before creating a new Request
            TimeUnit.SECONDS.sleep(30);
        }

    }
}
