package JonasFelixTim;

import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Scanner;
import java.util.concurrent.TimeUnit;

public class API_Requester {

    // Logger to see in what stage this script currently is
    private static Logger logger = LoggerFactory.getLogger(Test3.class);
    private static final String SQL_INSERT = "INSERT INTO Uff1 (HASH) VALUES (?)";

    public static void main(String[] args) throws IOException, InterruptedException {

        // API URLs as String
        String latestBlockHashURL = "https://api.blockcypher.com/v1/btc/main";
        String latestBlockhash = "";

        // PostgreSQL Variables

        Statement stmt = null;


        // Infinite loop to catch data 24/7
        while (true) {

            // Requesting API to get the newest blockhash
            logger.info(new Timestamp(System.currentTimeMillis()).getTime() + " - Requesting newest blockhash");
            JSONObject latestBlock = requestAPI(latestBlockHashURL);

            // Check if the requested hash is new
            if (!latestBlockhash.equals(latestBlock.get("hash").toString())) {

                // Set latest blockhash
                latestBlockhash = latestBlock.get("hash").toString();
                logger.info(new Timestamp(System.currentTimeMillis()).getTime() + " - New blockhash found: " + latestBlockhash);

                /*
                    PostgreSQL Code
                 */
                try (Connection c = DriverManager
                        .getConnection("jdbc:postgresql://193.196.55.36:5432/test",
                                "postgres", "Uff");
                     PreparedStatement preparedStatement = c.prepareStatement(SQL_INSERT)) {
                    System.out.println("Opened database successfully");
                    preparedStatement.setString(1, latestBlock.get("hash").toString());

                    int row = preparedStatement.executeUpdate();

                    // rows affected
                    System.out.println(row); //1

                } catch (SQLException e) {
                    System.err.format("SQL State: %s\n%s", e.getSQLState(), e.getMessage());
                } catch (Exception e) {
                    e.printStackTrace();
                }

                System.out.println("Records created successfully");

                logger.info(new Timestamp(System.currentTimeMillis()).getTime() + " - Block has been send to postgreSQL ");
            } else {
                logger.info(new Timestamp(System.currentTimeMillis()).getTime() + " - The requested blockhash is not new. " + latestBlockhash);
            }


            // Wait before creating a new Request
            // MAX_REQUESTS = 200/hour => 1 request every 18 seconds | to be sure we don exceed the limit we take 20 seconds
            TimeUnit.SECONDS.sleep(20);
        }

    }


    // Method to get a JSONObject from a given API-URL
    public static JSONObject requestAPI(String _URL) throws IOException, InterruptedException {

        // Convert given String to URL-object
        URL requestedURL = new URL(_URL);

        // Configure Connection to requested API
        HttpURLConnection connection = (HttpURLConnection) requestedURL.openConnection();
        connection.setRequestMethod("GET");
        connection.connect();

        int responseCode = connection.getResponseCode();

        // Check responsecode
        String inline = "";
        if (responseCode != 200) {
            logger.info(new Timestamp(System.currentTimeMillis()).getTime() + " - Responsecode: " + responseCode + " Retrying in 10 seconds.");
            // Wait before creating a new Request
            TimeUnit.SECONDS.sleep(10);
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