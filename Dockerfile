# Build stage
FROM eclipse-temurin:17-jdk-jammy AS build
WORKDIR /app
COPY backend/.mvn/ .mvn
COPY backend/mvnw backend/pom.xml ./
RUN chmod +x ./mvnw
RUN ./mvnw dependency:go-offline

COPY backend/src ./src
RUN ./mvnw clean package -DskipTests

# Run stage
FROM eclipse-temurin:17-jre-jammy
WORKDIR /app
COPY --from=build /app/target/meetingsummarizer-0.0.1-SNAPSHOT.jar app.jar
EXPOSE 8081
ENV PORT=8081
ENTRYPOINT ["java", "-Dserver.port=${PORT}", "-jar", "app.jar"]
