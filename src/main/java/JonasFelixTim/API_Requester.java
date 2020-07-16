package JonasFelixTim;

import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import scala.util.parsing.json.JSON;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.ProtocolException;
import java.net.URL;
import java.sql.Timestamp;
import java.util.Scanner;
import java.util.concurrent.TimeUnit;

public class API_Requester {

    // Logger to see in what stage this script currently is
    private static Logger logger = LoggerFactory.getLogger(API_Requester.class);;


    public static void main(String[] args) throws IOException, InterruptedException {

        // API URLs as String
        String latestBlockHashURL = "https://api.blockcypher.com/v1/btc/main";
        String latestBlockhash = "";


        // Infinite loop to catch data 24/7
        while(true){

            // Requesting API to get the newest blockhash
            logger.info(new Timestamp(System.currentTimeMillis()).getTime() + " - Requesting newest blockhash");
            JSONObject latestBlock = requestAPI(latestBlockHashURL);

            // Check if the requested hash is new
            if(!latestBlockhash.equals(latestBlock.get("hash").toString())){

                // Set latest blockhash
                latestBlockhash = latestBlock.get("hash").toString();
                logger.info(new Timestamp(System.currentTimeMillis()).getTime() + " - New blockhash found: " +latestBlockhash);

                /*
                    TODO: CODE to write in PostgreSQL
                 */

                logger.info(new Timestamp(System.currentTimeMillis()).getTime() + " - Block has been send to postgreSQL ");
            } else{
                logger.info(new Timestamp(System.currentTimeMillis()).getTime() + " - The requested blockhash is not new. " +latestBlockhash);
            }


            // Wait before creating a new Request
            // MAX_REQUESTS = 200/hour => 1 request every 18 seconds | to be sure we don exceed the limit we take 20 seconds
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
        String inline ="";
        if (responseCode != 200) {
            logger.info(new Timestamp(System.currentTimeMillis()).getTime() +" - Responsecode: " +responseCode +" Retrying in 10 seconds.");
            requestAPI(_URL);
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
