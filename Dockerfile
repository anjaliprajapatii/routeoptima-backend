# Step 1: Build the application using Maven and JDK 17
FROM maven:3.8.5-openjdk-17 AS build
WORKDIR /app
COPY . .
RUN mvn clean package -DskipTests

# Step 2: Run the application using a stable JDK 17 image
FROM eclipse-temurin:17-jdk-jammy
WORKDIR /app
# Make sure this jar name matches your pom.xml artifactId and version
COPY --from=build /app/target/RouteOptima-0.0.1-SNAPSHOT.jar app.jar
EXPOSE 8082
ENTRYPOINT ["java", "-jar", "app.jar"]