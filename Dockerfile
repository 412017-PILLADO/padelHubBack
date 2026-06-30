# ── Build ──
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app
COPY pom.xml .
COPY .mvn .mvn
COPY mvnw .
RUN ./mvnw -q -B dependency:go-offline
COPY src src
# Tests se corren aparte (necesitan Docker/Testcontainers); el build de imagen los saltea.
RUN ./mvnw -q -B -DskipTests package

# ── Run ──
FROM eclipse-temurin:17-jre
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar
# El puerto lo define la plataforma vía env PORT (Railway/Render/etc.); cae a 8080.
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
