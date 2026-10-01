FROM maven:3.9.11-eclipse-temurin-21 AS build
WORKDIR /source
COPY backend/pom.xml ./pom.xml
COPY backend/src ./src
RUN mvn -B -DskipTests package
FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /source/target/cerebro-0.0.1-SNAPSHOT.jar /app/temper-store.jar
USER 10001:10001
ENV SERVER_PORT=8080 TEMPER_STORE_ENABLED=true TEMPER_DEMO_ENABLED=false
EXPOSE 8080
ENTRYPOINT ["java","-XX:MaxRAMPercentage=65","-jar","/app/temper-store.jar"]
