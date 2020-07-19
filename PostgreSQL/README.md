# PostgreSQL

Gets hashes from the API_Requester and stores them in a PostgreSQL Database.


## Getting Started

Pull the newest image from Docker Hub

    docker pull postgres 
    
Make an empty directory for storing data outside of the container.
    
    mkdir -p $HOME/docker/volumes/postgres 


Start the Postgres Container

    docker run --rm   --name pg-docker -e POSTGRES_PASSWORD=<Your_Password> -d -p 5432:5432 -v $HOME/docker/volumes/postgres:/var/lib/postgresql/data  postgres 

-p: Bind port 5432 on localhost to port 5432 within the container. After setting up firewall rules the container is accessible from outside at http://<ip>:5432.
-v: Mount $HOME/docker/volumes/postgres on the host machine to the container side volume path /var/lib/postgresql/data created inside the container. This ensures that postgres data persists even after the container is removed.

## Sources:

https://www.tutorialspoint.com/postgresql/postgresql_java.htm
https://mkyong.com/jdbc/jdbc-preparestatement-example-insert-a-record/ 
https://hackernoon.com/dont-install-postgres-docker-pull-postgres-bee20e200198
https://hub.docker.com/_/postgres
