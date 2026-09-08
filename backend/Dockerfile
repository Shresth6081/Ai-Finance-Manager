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

# Install Tesseract OCR, English language pack, and native libraries
RUN apt-get update && \
    apt-get install -y --no-install-recommends \
        tesseract-ocr \
        tesseract-ocr-eng \
        libtesseract-dev \
        libleptonica-dev && \
    rm -rf /var/lib/apt/lists/*

# Ensure symlinks for tessdata and native libraries exist
RUN mkdir -p /usr/share/tessdata && \
    if [ -d "/usr/share/tesseract-ocr/5/tessdata" ]; then \
        cp -rn /usr/share/tesseract-ocr/5/tessdata/* /usr/share/tessdata/ 2>/dev/null || true; \
    elif [ -d "/usr/share/tesseract-ocr/4.00/tessdata" ]; then \
        cp -rn /usr/share/tesseract-ocr/4.00/tessdata/* /usr/share/tessdata/ 2>/dev/null || true; \
    fi && \
    ldconfig

# Copy built jar from build stage
COPY --from=build /app/target/*.jar app.jar

# Expose Render default port or 8085
EXPOSE 8085

# Environment variable for Tesseract data path
ENV TESSDATA_PREFIX=/usr/share/tessdata

# Run Spring Boot app with optimized memory settings for 512MB RAM container
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=65.0", "-XX:+UseSerialGC", "-Xss512k", "-Djava.awt.headless=true", "-Dserver.port=${PORT:8085}", "-jar", "app.jar"]

