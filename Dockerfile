FROM eclipse-temurin:21.0.2_13-jdk-alpine
EXPOSE 8084
ADD target/AdministrativeareaApi-0.0.1-SNAPSHOT.jar AdministrativeareaApi-0.0.1-SNAPSHOT.jar
ENTRYPOINT ["java","-jar","/AdministrativeareaApi-0.0.1-SNAPSHOT.jar"]