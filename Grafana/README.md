# Grafana
We are using Grafana as our tool to display time series data from our InfluxDB.
## Getting started
To download and install Grafana in a docker container you have to use the following command.
```
docker run -d --name=grafana -p 3000:3000 grafana/grafana
```
If you want to configure and access Grafana from your browser you have to enter: ipaddress:3000

## Adding a data source to Grafana
Firstly you have to add a data source with the following parameters.
- Name: Choose a name
- URL: http://ipaddress:8086/
- Access: Server
- Database: The name of your InfluxDB database.
- User & password: Your InfluxDb user.
- HTTP Method: Post
  
## Building a dashboard
If there is data in the configured database you can start building a dashboard.

The first option to query data ist to use the existing form provided by Grafana:
![GrafanaQuery](https://github.com/TimBeutelspacher/DataPipeline/blob/master/images/GrafanaQuery.PNG)

The second option is to write a query in InfluxQL:
```
SELECT * FROM "topic_btc" 
GROUP BY * 
ORDER BY DESC LIMIT 5
```
In this case I want to query all field values from the measurement "topic_btc", but I only want the last 5 entries of the measurement.

Sometimes the data we recieve in Grafana isn't in the format in which we would like to display it. Therefore we have to use a calculation to recieve the data in the right format. This is possible in the tab "Transform". In our case we store the transaction volume of a bitcoin block in satoshi, but we want to display it in bitcoin (1 bitcoin = 100 000 000 satoshi). In the following piture you can see how we calculate this. 
https://github.com/TimBeutelspacher/DataPipeline/blob/master/images/GrafanaTransformData.PNG
![GrafanaTransformData](https://github.com/TimBeutelspacher/DataPipeline/blob/master/images/GrafanaTransformData.PNG)
