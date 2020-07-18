
FROM openjdk:8-jre-alpine
ADD DataPipeline.jar /
COPY requirements.txt requirements.txt
COPY . .
CMD [ "java", "-jar", "./DataPipeline.jar"]

