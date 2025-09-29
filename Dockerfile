FROM maven:3.9.6-eclipse-temurin-17 AS build
WORKDIR /app
COPY pom.xml .
COPY src ./src
RUN mvn -q -DskipTests package

FROM eclipse-temurin:17-jre
ENV TZ=UTC JAVA_OPTS=""
WORKDIR /
COPY --from=build /app/target/*.jar /application.jar
VOLUME ["/upload-files"]
EXPOSE 8080
ENTRYPOINT ["sh","-c","java $JAVA_OPTS -jar /application.jar"]
