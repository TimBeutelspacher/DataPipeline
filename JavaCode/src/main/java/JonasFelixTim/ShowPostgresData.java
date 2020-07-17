package JonasFelixTim;

import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.text.ParseException;


public class ShowPostgresData {
    public static void main( String[] args) throws IOException, InterruptedException, ParseException {
        Connection c = null;
        Statement stmt = null;
        try {
            Class.forName("org.postgresql.Driver");
            c = DriverManager
                    .getConnection("jdbc:postgresql://193.196.55.36:5432/test",
                            "postgres", "Uff");
            c.setAutoCommit(false);
            System.out.println("Opened database successfully");

            stmt = c.createStatement();
            ResultSet rs = stmt.executeQuery( "SELECT * FROM Uff1;" );
            while ( rs.next() ) {
                int id = rs.getInt("id");
                String  hash = rs.getString("hash");
                System.out.println( "ID = " + id );
                System.out.println( "HASH = " + hash );

                System.out.println();
            }
            rs.close();
            stmt.close();
            c.close();
        } catch ( Exception e ) {
            System.err.println( e.getClass().getName()+": "+ e.getMessage() );
            System.exit(0);
        }
        System.out.println("Operation done successfully");
    }
}