# Estágio 1: Imagem final (sem a necessidade de build no Dockerfile)
FROM eclipse-temurin:17-jre-jammy

WORKDIR /opt/app

# Copiar o arquivo JAR da pasta 'target' para a imagem final
COPY target/*.jar ./app.jar

EXPOSE 8080

# Definir o comando de entrada
ENTRYPOINT ["java", "-jar", "./app.jar", "--spring.profiles.active=dev"]
