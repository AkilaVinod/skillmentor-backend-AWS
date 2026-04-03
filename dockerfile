FROM eclipse-temurin:17-jdk
WORKDIR /app

# Copy only the built JAR
COPY target/*.jar app.jar

# Expose standard Spring Boot port
EXPOSE 8080

# Run the app
ENTRYPOINT ["java","-jar","app.jar"]