# syntax=docker/dockerfile:1
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /usr/src/app
COPY pom.xml .
COPY src ./src
RUN --mount=type=cache,target=/root/.m2 mvn -B -q package -DskipTests

FROM eclipse-temurin:21-jre
RUN useradd --system --create-home discorki
WORKDIR /opt/app
COPY --from=build /usr/src/app/target/discorki.jar ./discorki.jar
USER discorki
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "discorki.jar"]
