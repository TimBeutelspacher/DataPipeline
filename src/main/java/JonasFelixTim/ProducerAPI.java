package JonasFelixTim;

import io.confluent.kafka.schemaregistry.client.CachedSchemaRegistryClient;
import io.confluent.kafka.schemaregistry.client.rest.exceptions.RestClientException;
import io.confluent.kafka.serializers.AbstractKafkaAvroSerDeConfig;
import org.apache.avro.Schema;
import org.apache.avro.generic.GenericData;
import org.apache.avro.generic.GenericRecord;
import org.apache.commons.io.IOUtils;
import org.apache.kafka.clients.producer.*;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import scala.util.parsing.json.JSON;

import java.io.FileInputStream;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.sql.Timestamp;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Properties;
import java.util.Scanner;
import java.util.concurrent.TimeUnit;

public class ProducerAPI {


    public static void main(String[] args) throws IOException, InterruptedException, RestClientException, ParseException {

        /*
            Variables
         */

        // Kafka variables
        String bootstrapServers = "193.196.54.92:9092";
        Logger logger = LoggerFactory.getLogger(ProducerAPI.class);
        String topic = "btc2";
        GenericData.Record record;
        String schema = null;

        // Schema variables
        String schemaRegURL = "http://193.196.54.92:8081";
        String schemaPath = System.getProperty("user.dir") + "/btc_schema.avsc";

        // Schema to test local
        //String schemaPath = System.getProperty("user.dir")+"/src/main/resources/schemes/btc_schema.avsc";

        // subject convention is "<topic-name>-value"
        String subject = topic + "-value";
        String latestBlockHash = "";

        // API URLs as String
        String latestBlockHashURL = "https://blockchain.info/latestblock";
        String basicBlockURLString = "https://api.blockcypher.com/v1/btc/main/blocks/";
        String prevBlockURLString = "https://api.blockcypher.com/v1/btc/main/blocks/";

        // Output values
        String blockhash;
        int n_tx;
        int blockHeight;
        long secondsSincePrevBlock = 0;
        int size;
        long totalVol;
        long totalFees;

        // Variables to calculate time difference between blocks
        long latestBlockTime = 0;
        long currentBlockTime = 0;


        /*
            Configurations
         */

        // Date-converter
        DateFormat formatter = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");

        // Configure inputStream
        FileInputStream inputStream = new FileInputStream(schemaPath);
        try {
            schema = IOUtils.toString(inputStream, "UTF-8");
        } finally {
            inputStream.close();
        }

        // Create Schema-object from given schema
        Schema avroSchema = new Schema.Parser().parse(schema);
        CachedSchemaRegistryClient client = new CachedSchemaRegistryClient(schemaRegURL, 20);
        client.register(subject, avroSchema);

        // Set Producer properties
        logger.info("Setting up producer");
        Properties properties = new Properties();
        properties.setProperty(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        properties.setProperty(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, "io.confluent.kafka.serializers.KafkaAvroSerializer");
        properties.setProperty(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, "io.confluent.kafka.serializers.KafkaAvroSerializer");
        properties.setProperty(AbstractKafkaAvroSerDeConfig.SCHEMA_REGISTRY_URL_CONFIG, schemaRegURL);

        // Create the producer
        Producer<Integer, GenericRecord> producer = new KafkaProducer<Integer, GenericRecord>(properties);

        logger.info("Producer working | Topic: " + topic);


        /*
            Procedures
         */

        // Infinite loop to catch data 24/7
        while (true) {

            // Requesting latest block
            logger.info(new Timestamp(System.currentTimeMillis()).getTime() + " - Requesting newest blockhash.");
            JSONObject latestBlockHashJsonObject = requestAPI(latestBlockHashURL);

            // If the requested blockhash is different to the stored one, then its a new one
            if (!latestBlockHash.equals(latestBlockHashJsonObject.get("hash").toString())) {

                // Set the new requested blockhash as the newest stored
                logger.info(new Timestamp(System.currentTimeMillis()).getTime() + " - New Block detected: " + latestBlockHashJsonObject.get("hash").toString());
                latestBlockHash = latestBlockHashJsonObject.get("hash").toString();

                // Wait before creating a Request, just to be sure the block is available in the API
                logger.info("Waiting 30 seconds to be sure the block is available in the second API.");
                TimeUnit.SECONDS.sleep(30);

                // Request detailed API to get all the information
                JSONObject singleBlockHashJsonObject = requestAPI(basicBlockURLString + latestBlockHash);
                logger.info("Requested detailed information of block at URL: " + basicBlockURLString + latestBlockHash);

                // Create generic record
                record = new GenericData.Record(avroSchema);

                // Calculate seconds between Blocks
                // First time the script runs, the time of the previous block needs to get requested to get that time
                if (latestBlockTime != 0) {
                    currentBlockTime = formatter.parse(singleBlockHashJsonObject.get("time").toString()).getTime() / 1000;
                    secondsSincePrevBlock = currentBlockTime - latestBlockTime;
                } else {
                    // Request detailed API to get the time of the previous block
                    JSONObject prevBlockHashJsonObject = requestAPI(prevBlockURLString + singleBlockHashJsonObject.get("prev_block").toString());
                    logger.info("Requested previous block URL: " + prevBlockURLString + singleBlockHashJsonObject.get("prev_block").toString());

                    // Calculating the time difference between the current and the previous block.
                    latestBlockTime = formatter.parse(prevBlockHashJsonObject.get("time").toString()).getTime() / 1000;
                    currentBlockTime = formatter.parse(singleBlockHashJsonObject.get("time").toString()).getTime() / 1000;
                    secondsSincePrevBlock = currentBlockTime - latestBlockTime;
                }

                // Setting the creation time of the latest block to be able to compare them in the next execution
                latestBlockTime = formatter.parse(singleBlockHashJsonObject.get("time").toString()).getTime() / 1000;

                // Set attributes of record got from the API
                blockhash = singleBlockHashJsonObject.get("hash").toString();
                n_tx = (int) singleBlockHashJsonObject.get("n_tx");
                blockHeight = (int) singleBlockHashJsonObject.get("height");
                size = (int) singleBlockHashJsonObject.get("size");
                totalVol = (long) singleBlockHashJsonObject.get("total");
                totalFees = (int) singleBlockHashJsonObject.get("fees");


                // Put the elements to the generic record according to the avro schema.
                record.put("blockhash", blockhash);
                record.put("n_tx", n_tx);
                record.put("blockheight", blockHeight);
                record.put("totalVol", totalVol);
                record.put("totalFees", totalFees);
                record.put("secondsSincePrevBlock", secondsSincePrevBlock);
                record.put("size", size);

                // Send avro message to the topic with the blockHeight as key
                logger.info(new Timestamp(System.currentTimeMillis()).getTime() + " - Sending data to kafka.");
                producer.send(new ProducerRecord<Integer, GenericRecord>(topic, blockHeight, record));

                // flush producer
                logger.info(new Timestamp(System.currentTimeMillis()).getTime() + " - Flushing kafka producer.");
                producer.flush();
            } else {
                // The requested block has already been stored
                logger.info(new Timestamp(System.currentTimeMillis()).getTime() + " - The requested blockhash (" + latestBlockHashJsonObject.get("hash").toString() + ") is not new.");
            }

            // Wait before creating a new Request
            TimeUnit.SECONDS.sleep(20);
        }
    }


    // Method to get a JSONObject from a given API-URL
    public static JSONObject requestAPI(String _URL) throws IOException {

        // Convert given String to URL-object
        URL requestedURL = new URL(_URL);

        // Configure Connection to requested API
        HttpURLConnection connection = (HttpURLConnection) requestedURL.openConnection();
        connection.setRequestMethod("GET");
        connection.connect();

        int responseCode = connection.getResponseCode();

        // Check responsecode
        String inline;
        if (responseCode != 200) {
            throw new RuntimeException("HttpResponseCode: " + responseCode);
        } else {
            // Convert response to String
            Scanner sc = new Scanner(requestedURL.openStream());
            inline = "";
            while (sc.hasNext()) {
                inline += sc.nextLine();
            }
            sc.close();
        }

        // Close connection
        connection.disconnect();

        // Convert String into a JSON Object
        JSONObject createdJSON = new JSONObject(inline);

        // Returning JSON
        return createdJSON;
    }
}
