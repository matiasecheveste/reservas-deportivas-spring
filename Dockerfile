# Reservas deportivas — Matías Nicolás Echeveste Cardozo
FROM eclipse-temurin:21-jdk AS compilacion
WORKDIR /fuente
COPY .mvn .mvn
COPY mvnw pom.xml ./
RUN ./mvnw -B -ntp dependency:go-offline
COPY src src
RUN ./mvnw -B -ntp -DskipTests package && java -Djarmode=tools -jar target/reservas-deportivas-1.0.0.jar extract --layers --launcher --destination capas

FROM eclipse-temurin:21-jre
RUN useradd --system --uid 10001 app
WORKDIR /app
COPY --from=compilacion /fuente/capas/dependencies/ ./
COPY --from=compilacion /fuente/capas/spring-boot-loader/ ./
COPY --from=compilacion /fuente/capas/snapshot-dependencies/ ./
COPY --from=compilacion /fuente/capas/application/ ./
USER app
EXPOSE 8080
HEALTHCHECK CMD curl -fs http://localhost:8080/actuator/health || exit 1
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75", "org.springframework.boot.loader.launch.JarLauncher"]
