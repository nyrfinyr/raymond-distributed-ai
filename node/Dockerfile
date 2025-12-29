# --- Stage 1: Build ---
FROM gradle:8.5-jdk21 AS builder

WORKDIR /app

COPY build.gradle settings.gradle ./
COPY src ./src

RUN gradle installDist --no-daemon -x test

# --- Stage 2: Runtime ---
FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

COPY --from=builder /app/build/install/node/lib/ /app/lib/

RUN rm /app/lib/node-*.jar

COPY --from=builder /app/build/libs/node-*.jar /app/lib/
COPY --from=builder /app/build/install/node/bin/node /app/bin/

RUN chmod +x /app/bin/node

ENTRYPOINT ["/app/bin/node"]