# Runtime-only image for CI use.
# Assumes `mvn package` has already run on the Jenkins agent (see Jenkinsfile: Package stage)
# and produced target/taskapi-1.0.0.jar — this stage just packages that jar into an image.

FROM eclipse-temurin:17-jre-alpine

WORKDIR /app

COPY target/taskapi-1.0.0.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
