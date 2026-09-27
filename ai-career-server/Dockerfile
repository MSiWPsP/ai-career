# syntax=docker/dockerfile:1.7

FROM maven:3.9.11-eclipse-temurin-21 AS build

WORKDIR /workspace

ARG RUN_TESTS=false
ARG MAVEN_OPTS="-Xms128m -Xmx512m -XX:MaxMetaspaceSize=192m -Dfile.encoding=UTF-8"
ENV MAVEN_OPTS=${MAVEN_OPTS}

COPY ai-career-server/pom.xml ./pom.xml
RUN --mount=type=cache,target=/root/.m2 \
    mvn -B -ntp -DskipTests dependency:resolve

COPY ai-career-server/src ./src
RUN --mount=type=cache,target=/root/.m2 \
    if [ "$RUN_TESTS" = "true" ]; then \
      mvn -B -ntp clean verify; \
    else \
      mvn -B -ntp -DskipTests clean verify; \
    fi && \
    cp "target/ai-career-server-0.0.1-SNAPSHOT.jar" target/application.jar && \
    if jar tf target/application.jar | grep -q 'application-local.yml'; then \
      echo '安全检查失败：application-local.yml 被打入 JAR' >&2; exit 1; \
    fi && \
    java -Djarmode=tools -jar target/application.jar extract --layers --destination target/extracted

FROM eclipse-temurin:21-jre-jammy AS runtime

ENV TZ=Asia/Shanghai \
    LANG=C.UTF-8 \
    LC_ALL=C.UTF-8 \
    JAVA_TOOL_OPTIONS="-Xms128m -Xmx352m -XX:MaxMetaspaceSize=144m -XX:MaxDirectMemorySize=64m -Dfile.encoding=UTF-8 -Duser.timezone=Asia/Shanghai"

RUN apt-get update && \
    DEBIAN_FRONTEND=noninteractive apt-get install -y --no-install-recommends curl tzdata && \
    ln -snf /usr/share/zoneinfo/Asia/Shanghai /etc/localtime && \
    echo Asia/Shanghai > /etc/timezone && \
    groupadd --system --gid 10001 app && \
    useradd --system --uid 10001 --gid app --home-dir /app --shell /usr/sbin/nologin app && \
    rm -rf /var/lib/apt/lists/*

WORKDIR /app

COPY --from=build --chown=10001:10001 /workspace/target/extracted/dependencies/ ./
COPY --from=build --chown=10001:10001 /workspace/target/extracted/spring-boot-loader/ ./
COPY --from=build --chown=10001:10001 /workspace/target/extracted/snapshot-dependencies/ ./
COPY --from=build --chown=10001:10001 /workspace/target/extracted/application/ ./

COPY --chown=10001:10001 docs/知识库/catalog.json /app/knowledge/catalog.json
COPY --chown=10001:10001 docs/知识库/java-backend-capabilities.md /app/knowledge/java-backend-capabilities.md
COPY --chown=10001:10001 docs/知识库/backend-learning-path.md /app/knowledge/backend-learning-path.md
COPY --chown=10001:10001 docs/知识库/internship-preparation.md /app/knowledge/internship-preparation.md
COPY --chown=10001:10001 docs/知识库/project-evidence.md /app/knowledge/project-evidence.md
COPY --chown=10001:10001 docs/知识库/career-decision.md /app/knowledge/career-decision.md

USER 10001:10001

EXPOSE 8080

HEALTHCHECK --interval=15s --timeout=5s --start-period=60s --retries=10 \
  CMD curl --fail --silent --show-error http://127.0.0.1:8080/actuator/health >/dev/null || exit 1

ENTRYPOINT ["java", "-jar", "/app/application.jar"]
