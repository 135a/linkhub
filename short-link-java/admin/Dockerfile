FROM eclipse-temurin:17-jdk-jammy

LABEL maintainer="shortlink-team"

ENV TZ=Asia/Shanghai
ENV LANG C.UTF-8
ENV JAVA_OPTS="-Xms256m -Xmx512m -XX:MetaspaceSize=128m -XX:MaxMetaspaceSize=256m -XX:+UseSerialGC -Djava.security.egd=file:/dev/./urandom"

RUN ln -sf /usr/share/zoneinfo/$TZ /etc/localtime && echo $TZ > /etc/timezone

RUN mkdir -p /app

WORKDIR /app

EXPOSE 8002

COPY admin/target/shortlink-admin-1.0.jar ./app.jar

CMD java $JAVA_OPTS -jar app.jar
