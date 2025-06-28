FROM amazoncorretto:21.0.4-alpine3.18

# Install curl for healthcheck
RUN apk add --no-cache curl

# Create a non-root user
RUN addgroup -g 1000 spring && adduser -u 1000 -G spring -s /bin/sh -D spring

WORKDIR /app

COPY target/*.jar app.jar

# Change file ownership
RUN chown spring:spring app.jar

# Switch to non-root user
USER spring

# Expose port
EXPOSE 8080

# JVM environment variables (configurable)
#ENV JAVA_OPTS="-Xmx512m -Xms256m"

# Simple healthcheck
HEALTHCHECK --interval=30s --timeout=3s --start-period=45s --retries=3 \
    CMD curl -f http://localhost:8080/actuator/health || exit 1

ENTRYPOINT ["sh", "-c","java ${JAVA_OPTS:-}", "-jar", "app.jar"]
