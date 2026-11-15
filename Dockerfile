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

# Fuso do negócio. As fotos ficam no banco de dados, então não é preciso disco/volume.
# JAVA_OPTS pensado para hospedagem gratuita (512 MB de RAM e CPU pequena):
#  -Xmx300m  limita a memória do Java, deixando folga para o restante do processo;
#  SerialGC e TieredStopAtLevel=1  usam menos memória e iniciam mais rápido em CPU fraca.
# Para um servidor maior, sobrescreva a variável JAVA_OPTS no painel da hospedagem.
ENV TZ=America/Cuiaba \
    JAVA_OPTS="-Xmx300m -XX:+UseSerialGC -XX:TieredStopAtLevel=1 -Djava.awt.headless=true"

EXPOSE 8080
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
