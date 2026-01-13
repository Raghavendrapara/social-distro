# Stage 1: Build the application
FROM maven:3.9.12-amazoncorretto-25-debian AS build
WORKDIR /app
COPY pom.xml .
RUN mvn dependency:go-offline -B
COPY src ./src
RUN mvn clean package -DskipTests

# Stage 2: Extract layers
FROM eclipse-temurin:25-jre-alpine AS builder
WORKDIR /builder
COPY --from=build /app/target/*.jar application.jar
RUN java -Djarmode=layertools -jar application.jar extract

# Stage 3: Create the final image
FROM eclipse-temurin:25-jre-alpine
WORKDIR /app
RUN addgroup -S spring && adduser -S spring -G spring
USER spring:spring
COPY --from=builder /builder/dependencies/ ./
COPY --from=builder /builder/spring-boot-loader/ ./
COPY --from=builder /builder/snapshot-dependencies/ ./
COPY --from=builder /builder/application/ ./
EXPOSE 8080
ENTRYPOINT ["java", "org.springframework.boot.loader.launch.JarLauncher"]