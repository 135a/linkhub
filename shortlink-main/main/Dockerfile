FROM eclipse-temurin:17-jdk-jammy

LABEL maintainer="shortlink-team"

ENV TZ=Asia/Shanghai
ENV LANG C.UTF-8
ENV JAVA_OPTS="-Xms128m -Xmx384m -XX:MetaspaceSize=96m -XX:MaxMetaspaceSize=192m -XX:+UseSerialGC -Djava.security.egd=file:/dev/./urandom"

RUN ln -sf /usr/share/zoneinfo/$TZ /etc/localtime && echo $TZ > /etc/timezone

RUN mkdir -p /app

WORKDIR /app

EXPOSE 8001

COPY project/target/shortlink-project-1.0.jar ./app.jar

CMD java $JAVA_OPTS -jar app.jar
