# ---- Build Stage ----
FROM gradle:8.7.0-jdk17 AS build
WORKDIR /app
COPY . .
RUN gradle :application:bootJar

# ---- Run Stage ----
FROM eclipse-temurin:17-jre
WORKDIR /app
COPY --from=build /app/application/build/libs/application-*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]