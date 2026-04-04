FROM eclipse-temurin:25-jdk AS maven
WORKDIR /usr/src/app
COPY . /usr/src/app
RUN ./mvnw clean package -DskipTests

FROM eclipse-temurin:25-jre
ARG JAR_FILE=discorki.jar
WORKDIR /opt/app
COPY --from=maven /usr/src/app/target/${JAR_FILE} /opt/app/
ENTRYPOINT ["java","-jar","discorki.jar"]
