FROM eclipse-temurin:17-jdk

ARG JAR_FILE=target/backend.jar

COPY ${JAR_FILE} backend.jar

ENTRYPOINT ["java", "-jar", "backend.jar"]

EXPOSE 8080