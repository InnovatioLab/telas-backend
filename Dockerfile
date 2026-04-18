# syntax=docker/dockerfile:1
FROM maven:3-eclipse-temurin-17 AS builder

WORKDIR /opt/app

COPY pom.xml .
RUN --mount=type=cache,target=/root/.m2/repository \
    mvn -B -q dependency:go-offline -DskipTests

COPY . .
RUN --mount=type=cache,target=/root/.m2/repository \
    mvn -B clean install -DskipTests

# Etapa de runtime
FROM eclipse-temurin:17-jre-jammy
WORKDIR /opt/app

# Apenas ENV, sem valores sensíveis fixos
ENV TZ=America/New_York

# Copia o jar gerado
COPY --from=builder /opt/app/target/*.jar ./app.jar

# Expondo porta padrão
EXPOSE 8080

# Healthcheck para monitorar o container
HEALTHCHECK --interval=30s --timeout=10s --start-period=10s --retries=3 \
  CMD curl --fail http://localhost:8080/api/actuator/health || exit 1

# Entrypoint
ENTRYPOINT ["java", "-jar", "./app.jar"]
