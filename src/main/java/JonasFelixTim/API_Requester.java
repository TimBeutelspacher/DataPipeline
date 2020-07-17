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
    private static Logger logger = LoggerFactory.getLogger(API_Requester.class);
    private static final String SQL_INSERT = "INSERT INTO Uff1 (HASH) VALUES (?)";

    public static void main(String[] args) throws IOException, InterruptedException {

        // API URL as String
        String latestBlockHashURL = "https://api.blockcypher.com/v1/btc/main";

        // Variable to store latest blockhash
        String latestBlockhash = getLatestHash();

        // PostgreSQL Variables
        Statement stmt = null;
        int index = 1;

        // Infinite loop to catch data 24/7
        while (true) {

            logger.info("Request: " + index);
            index++;
            // Requesting API to get the newest blockhash
            logger.info(new Timestamp(System.currentTimeMillis()).getTime() + " - Requesting latest blockhash");
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
                    logger.info(new Timestamp(System.currentTimeMillis()).getTime() + " - Opened database successfully");
                    preparedStatement.setString(1, latestBlock.get("hash").toString());

                } catch (SQLException e) {
                    logger.info(new Timestamp(System.currentTimeMillis()).getTime() + "SQL State: %s\n%s", e.getSQLState(), e.getMessage());
                } catch (Exception e) {
                    e.printStackTrace();
                }

                logger.info(new Timestamp(System.currentTimeMillis()).getTime() + " - Block has been sent to postgreSQL ");
            } else {
                logger.info(new Timestamp(System.currentTimeMillis()).getTime() + " - The requested blockhash is not new. " + latestBlockhash);
            }

            // Wait before creating a new Request
            // MAX_REQUESTS = 2000/day => 83/hour => 1,38/minute => 1 request every 43 seconds | to be sure we don exceed the limit we take 45 seconds
            TimeUnit.SECONDS.sleep(45);
        }
    }


    // Method to get a JSONObject from a given API-URL
    public static JSONObject requestAPI(String _URL) throws IOException, InterruptedException {

        // Convert given String to URL-object
        String token1 = "?token=62d8d1ca62e845959421b3e7a08e139b";
        String token2 = "?token=ff09471dd4394d4aa844dc106fa932e8";
        URL requestedURL1 = new URL(_URL +token1);
        URL requestedURL2 = new URL(_URL +token2);

        // Configure Connection to requested API
        HttpURLConnection connection = (HttpURLConnection) requestedURL1.openConnection();
        connection.setRequestMethod("GET");
        connection.connect();

        int responseCode = connection.getResponseCode();

        // Check responsecode
        String inline = "";
        if (responseCode != 200) {
            logger.info(new Timestamp(System.currentTimeMillis()).getTime() + " - Responsecode: " + responseCode + " Retrying in 25 seconds.");
            // Wait before creating a new Request
            TimeUnit.SECONDS.sleep(25);
            requestAPI(_URL);
        } else {
            // Convert response to String
            Scanner sc = new Scanner(requestedURL2.openStream());
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

    public static String getLatestHash() {
        String hash = "";
        try {
            logger.info(new Timestamp(System.currentTimeMillis()).getTime() + " + Requesting newest blockhash from database.");

            Connection c = null;
            Statement stmt = null;
            Class.forName("org.postgresql.Driver");
            c = DriverManager
                    .getConnection("jdbc:postgresql://193.196.55.36:5432/test",
                            "postgres", "Uff");
            c.setAutoCommit(false);


            stmt = c.createStatement();
            ResultSet rs = stmt.executeQuery("SELECT * FROM Uff1 ORDER BY id DESC LIMIT 1");
            while (rs.next()) {
                hash = rs.getString("hash");

            }
            rs.close();
            stmt.close();
            c.close();


        } catch (Exception e) {
            logger.info(e.getClass().getName() + ": " + e.getMessage());
        }
        logger.info(new Timestamp(System.currentTimeMillis()).getTime() + " - Database request successful");

        return hash;
    }
}