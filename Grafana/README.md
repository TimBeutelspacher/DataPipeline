# Grafana
We are using Grafana as our tool to display time series data from our InfluxDB.
## Getting started
To download and install Grafana in a docker container you have to use the following command.
```bash
docker run -d --name=grafana -p 3000:3000 grafana/grafana
```
If you want to access Grafana from your browser you have to enter: <IP of the VM>:3000

## Configuring Grafana
Firstly you have to add a data source with the following parameters.
- Name: Choose a name
- URL: http://<IP of the VM>:8086/
- Access: Server
- Database: The name of your InfluxDB database.
- User & password: Your InfluxDb user.
- HTTP Method: Post
  
## Building a dashboard
If there is data in the configured database you can start building a dashboard.

The first option to query data ist to use the existing form provided by Grafana.
![GrafanaQuery](https://github.com/TimBeutelspacher/DataPipeline/blob/master/images/GrafanaQuery.PNG)
