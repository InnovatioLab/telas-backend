# Etapa de build
FROM eclipse-temurin:17-jdk-jammy AS builder
WORKDIR /opt/app
COPY . .
RUN bash ./mvnw dependency:go-offline
RUN bash ./mvnw clean install -DskipTests

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