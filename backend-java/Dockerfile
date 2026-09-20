FROM maven:3.9.12-eclipse-temurin-21 AS build
WORKDIR /workspace
COPY pom.xml ./
COPY .mvn .mvn
COPY mvnw mvnw
RUN chmod +x mvnw && ./mvnw -B -DskipTests dependency:go-offline
COPY src src
RUN ./mvnw -B -DskipTests package

FROM eclipse-temurin:21-jre-noble
RUN apt-get update && apt-get install -y --no-install-recommends curl && rm -rf /var/lib/apt/lists/* \
    && groupadd --system knowledgeops && useradd --system --gid knowledgeops --home /app knowledgeops
WORKDIR /app
COPY --from=build /workspace/target/backend-java-*.jar /app/application.jar
COPY docker/healthcheck /app/healthcheck
RUN chmod 0555 /app/healthcheck && chown -R knowledgeops:knowledgeops /app
USER knowledgeops
EXPOSE 8080
HEALTHCHECK --interval=10s --timeout=3s --start-period=30s --retries=10 CMD ["/app/healthcheck"]
ENTRYPOINT ["java","-jar","/app/application.jar"]
