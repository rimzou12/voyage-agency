# Multi-stage build for the agency-voyage backend (Maven reactor: domain, application,
# infrastructure, web). Built from the backend/ directory as context (see render.yaml).
FROM maven:3.9-eclipse-temurin-25 AS build
WORKDIR /build

# Cache dependency resolution separately from source changes.
COPY pom.xml .
COPY domain/pom.xml domain/pom.xml
COPY application/pom.xml application/pom.xml
COPY infrastructure/pom.xml infrastructure/pom.xml
COPY web/pom.xml web/pom.xml
RUN mvn -q -pl domain,application,infrastructure,web -am dependency:go-offline

COPY domain domain
COPY application application
COPY infrastructure infrastructure
COPY web web
RUN mvn -q -pl domain,application,infrastructure,web -am package -DskipTests

FROM eclipse-temurin:25-jre
WORKDIR /app
COPY --from=build /build/web/target/agency-voyage.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
