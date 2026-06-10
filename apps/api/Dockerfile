FROM maven:3.9.9-eclipse-temurin-21 AS build
WORKDIR /workspace
COPY pom.xml .
COPY src ./src
RUN mvn -q -DskipTests package

FROM eclipse-temurin:21-jre
WORKDIR /app
ARG IMAGE_REVISION=unknown
ARG BUILD_TIME=unknown
ENV IMAGE_REVISION=${IMAGE_REVISION}
ENV BUILD_TIME=${BUILD_TIME}
LABEL org.opencontainers.image.revision=${IMAGE_REVISION}
LABEL org.opencontainers.image.created=${BUILD_TIME}
COPY --from=build /workspace/target/api-0.1.0.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
