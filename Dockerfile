FROM eclipse-temurin:25-jre

ARG SIGNAL_CLI_VERSION=0.14.2
ARG SIGNAL_CLI_HOME=/opt/signal-cli

ENV APP_NAME=rent-wise \
    PORT=8080 \
    SPRING_PROFILES_ACTIVE=prod \
    SIGNAL_CLI_PATH=/usr/local/bin/signal-cli

RUN apt-get update \
    && apt-get install -y --no-install-recommends curl ca-certificates tar \
    && rm -rf /var/lib/apt/lists/*

RUN set -eux; \
    curl -fL "https://github.com/AsamK/signal-cli/releases/download/v${SIGNAL_CLI_VERSION}/signal-cli-${SIGNAL_CLI_VERSION}-Linux-native.tar.gz" -o /tmp/signal-cli.tar.gz; \
    tar -xzf /tmp/signal-cli.tar.gz -C /opt; \
    rm /tmp/signal-cli.tar.gz; \
    extracted_dir="$(find /opt -maxdepth 1 -type d -name 'signal-cli*' | sort | tail -1)"; \
    ln -sfn "${extracted_dir}" "${SIGNAL_CLI_HOME}"; \
    ln -sfn "${SIGNAL_CLI_HOME}/bin/signal-cli" "${SIGNAL_CLI_PATH}"

WORKDIR /app
COPY backend/build/libs/rent-wise.jar /app/rent-wise.jar

EXPOSE 8080

ENTRYPOINT ["sh", "-c", "java ${JAVA_OPTS:-} -Dserver.port=${PORT} -Dspring.profiles.active=${SPRING_PROFILES_ACTIVE} -Dapp.frontend-base-url=${FRONTEND_BASE_URL:-http://localhost:5173} -jar /app/rent-wise.jar"]
