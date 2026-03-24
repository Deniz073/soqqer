# Build stage
FROM maven:3.9.12-eclipse-temurin-25 AS build

WORKDIR /app

COPY pom.xml .

RUN mvn -B -q -e -C -DskipTests dependency:go-offline

COPY src ./src

RUN mvn -B -q -DskipTests package

# Runtime stage
FROM eclipse-temurin:25-jre-jammy

WORKDIR /app

RUN useradd -r -u 1001 appuser

COPY --from=build /app/target/*jar /app/app.jar

USER appuser

EXPOSE 8080

HEALTHCHECK --interval=30s --timeout=5s --start-period=20s --retries=3 \
  CMD curl --fail http://localhost:8080/actuator/health || exit 1

ENTRYPOINT ["java","-XX:MaxRAMPercentage=75","-XX:+ExitOnOutOfMemoryError","-jar","/app/app.jar"]