# --- etap 1: budowanie ---
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /build

# Zaleznosci pobierane osobno, zeby Docker mogl je cache'owac
# i nie sciagac wszystkiego przy kazdej zmianie w kodzie.
COPY pom.xml .
RUN mvn -B -q dependency:go-offline

COPY src ./src
RUN mvn -B -q clean package -DskipTests

# --- etap 2: obraz uruchomieniowy ---
FROM eclipse-temurin:17-jre-noble
WORKDIR /app

# Aplikacja nie ma powodu dzialac jako root.
RUN groupadd --system app && useradd --system --gid app app \
 && mkdir -p /app/storage && chown -R app:app /app

COPY --from=build /build/target/*.jar app.jar

USER app
EXPOSE 8080

# Bez tego JVM widzi pamiec calego hosta, a nie limit kontenera.
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75", "-jar", "/app/app.jar"]
