FROM openjdk:17-jdk-slim

LABEL maintainer="shortlink-team"

ENV TZ=Asia/Shanghai
ENV LANG C.UTF-8
ENV JAVA_OPTS="-Xms256m -Xmx512m -XX:MetaspaceSize=96m -XX:MaxMetaspaceSize=192m -XX:+UseSerialGC -Djava.security.egd=file:/dev/./urandom"

RUN ln -sf /usr/share/zoneinfo/$TZ /etc/localtime && echo $TZ > /etc/timezone

RUN mkdir -p /app

WORKDIR /app

EXPOSE 8000

COPY gateway/target/shortlink-gateway.jar ./app.jar

CMD java $JAVA_OPTS -jar app.jar
