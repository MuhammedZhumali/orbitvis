FROM gradle:8.5-jdk21 AS builder

WORKDIR /app

COPY build.gradle.kts ./
COPY settings.grade.kts ./settings.gradle.kts
COPY gradle ./gradle
COPY src ./src

RUN gradle bootJar --no-daemon -x test

FROM eclipse-temurin:21-jre

WORKDIR /app

COPY --from=builder /app/build/libs/*.jar app.jar
COPY backend/resources/orekit-data /app/orekit-data

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
