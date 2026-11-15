# ===== Etapa 1: compila o projeto com Maven =====
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app
COPY pom.xml .
RUN mvn -q -B dependency:go-offline
COPY src ./src
RUN mvn -q -B -DskipTests package

# ===== Etapa 2: imagem final, só com o Java e o .jar =====
FROM eclipse-temurin:17-jre
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar

# Fuso do negócio e pasta das fotos (monte um disco/volume persistente em /data/uploads)
ENV TZ=America/Cuiaba \
    UPLOAD_DIR=/data/uploads \
    JAVA_OPTS="-Xmx384m"
RUN mkdir -p /data/uploads

EXPOSE 8080
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
