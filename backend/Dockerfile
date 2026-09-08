# 1. Build Stage
FROM maven:3.9.6-eclipse-temurin-17 AS build
WORKDIR /app
COPY pom.xml .
# Download dependencies
RUN mvn dependency:go-offline -B
COPY src ./src
# Build package without running tests
RUN mvn clean package -DskipTests

# 2. Runtime Stage
FROM eclipse-temurin:17-jre-jammy
WORKDIR /app

# Install Tesseract OCR and English language data for Linux
RUN apt-get update && \
    apt-get install -y --no-install-recommends tesseract-ocr tesseract-ocr-eng && \
    rm -rf /var/lib/apt/lists/*

# Copy built jar from build stage
COPY --from=build /app/target/*.jar app.jar

# Expose Render default port or 8085
EXPOSE 8085

# Run Spring Boot app
ENTRYPOINT ["java", "-Dserver.port=${PORT:8085}", "-Dtesseract.datapath=${TESSERACT_DATAPATH:/usr/share/tesseract-ocr/5/tessdata}", "-jar", "app.jar"]
