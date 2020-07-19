# PostgreSQL

Gets hashes from the API_Requester and stores them in a table.


## Getting Started

Pull the newest image from Docker Hub

    docker pull postgres 
    
Make an empty directory for storing data outside of the container.
    
    mkdir -p $HOME/docker/volumes/postgres 


Start the Postgres Container

    docker run --rm   --name pg-docker -e POSTGRES_PASSWORD=<Your_Password> -d -p 5432:5432 -v $HOME/docker/volumes/postgres:/var/lib/postgresql/data  postgres 

-p: Bind port 5432 on localhost to port 5432 within the container. After setting up firewall rules the container is accessible from outside at http://<your_ip>:5432.

-v: Mount $HOME/docker/volumes/postgres on the host machine to the container side volume path /var/lib/postgresql/data created inside the container. This ensures that postgres data persists even after the container is removed.

## Inserting Data

#### 1. Create a Database

        docker exec -it <container_id> bash
        psql -U postgres
        
Create Database SQL statement.  

        CREATE DATABASE test;
        
Connect to test   

        \c test 
        
#### 2. Create a Table

For creating the table we have used following Java code:

        public class CreateTable {
            public static void main(String args[]) {
                Connection c = null;
                Statement stmt = null;
                try {
                    Class.forName("org.postgresql.Driver");
                    // Connect to our PostgreSQL Database called "hashdb"
                    c = DriverManager
                            .getConnection("jdbc:postgresql://<your_ip>:5432/hashdb",
                                    "postgres", "KSC4ever");
                    System.out.println("Opened database successfully");

                    stmt = c.createStatement();
                    // defines an auto-increment column in a table as well as a text column for storing the hashes
                    String sql = "CREATE TABLE hashes " +
                            "(id SERIAL PRIMARY KEY," +
                            " hash TEXT NOT NULL)";
                    // execute the SQL statement
                    stmt.executeUpdate(sql);
                    stmt.close();
                    c.close();
                } catch (Exception e) {
                    System.err.println(e.getClass().getName() + ": " + e.getMessage());
                }
                System.out.println("Table created successfully");

            }
        }

#### 3. Insert Data

-> https://github.com/TimBeutelspacher/DataPipeline/tree/master/API_Requester

## Sources:

Java Code: https://www.tutorialspoint.com/postgresql/postgresql_java.htm

Prepared Statemets: https://mkyong.com/jdbc/jdbc-preparestatement-example-insert-a-record/ 

PostgreSQL Docker Container: https://hackernoon.com/dont-install-postgres-docker-pull-postgres-bee20e200198

PostgreSQL Docker Container: https://hub.docker.com/_/postgres

Serial Primary Key: https://www.postgresqltutorial.com/postgresql-serial/
