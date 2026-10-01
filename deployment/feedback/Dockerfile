FROM maven:3.9.11-eclipse-temurin-21 AS build
WORKDIR /source
COPY backend/pom.xml ./pom.xml
COPY backend/src ./src
RUN mvn -B -DskipTests package
FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /source/target/cerebro-0.0.1-SNAPSHOT.jar /app/temper-feedback.jar
RUN mkdir -p /data/feedback && chown -R 10001:10001 /data
USER 10001:10001
ENV SERVER_PORT=8080 TEMPER_LEARNING_ENABLED=true TEMPER_LEARNING_DIRECTORY=/data/feedback TEMPER_STORE_ENABLED=false TEMPER_DEMO_ENABLED=false
EXPOSE 8080
ENTRYPOINT ["java","-XX:MaxRAMPercentage=65","-jar","/app/temper-feedback.jar"]
