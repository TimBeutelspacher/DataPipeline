# InfluxDB
Stores the data we want to display in our Grafana dashboard.
## Getting started
Downloading and installing InfluxDB to a docker container
``` 
docker run -d -p 8086:8086 --name influxdb-local influxdb:1.7.7
```

To run the InfluxDB shell:
``` 
docker exec -it influxdb-name influx
```

## InfluxQL
Command to create a database:
``` 
Create Database btcDB
```

Command to show the databases:
``` 
Show databases
```

Command to insert data into your database:
``` 
Insert btcMeasurement,btcChain=main blockheight=640000,ntx=2000 1465839830100400200
```
In this example we add one entry to the measurement "btcMeasurement" which we create with writing to it. With the second part of the statement we can tag the data. This is optional and we don't need to add a tag. In this exapmle "btcChain" is the tag key, while "main" is the tag value. In the next part of the statement we have two field keys ("blockheight" and "ntx") and their field values. The number at the end of the statement is a timestamp which we don't have to add manually, but can if we want to.
