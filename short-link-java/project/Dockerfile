FROM eclipse-temurin:17-jdk-jammy

LABEL maintainer="shortlink-team"

ENV TZ=Asia/Shanghai
ENV LANG C.UTF-8
ENV JVM_PROFILE="g1"
ENV JAVA_OPTS_DEFAULT="-Xms128m -Xmx384m -XX:MetaspaceSize=96m -XX:MaxMetaspaceSize=192m -XX:+UseG1GC -XX:MaxGCPauseMillis=200 -Djava.security.egd=file:/dev/./urandom"
ENV JAVA_OPTS_G1="-Xms128m -Xmx384m -XX:MetaspaceSize=96m -XX:MaxMetaspaceSize=192m -XX:+UseG1GC -XX:MaxGCPauseMillis=200 -Djava.security.egd=file:/dev/./urandom"
ENV JAVA_OPTS_ZGC="-Xms128m -Xmx384m -XX:MetaspaceSize=96m -XX:MaxMetaspaceSize=192m -XX:+UseZGC -XX:SoftMaxHeapSize=320m -Djava.security.egd=file:/dev/./urandom"
ENV JAVA_OPTS_EXTRA=""

RUN ln -sf /usr/share/zoneinfo/$TZ /etc/localtime && echo $TZ > /etc/timezone

RUN mkdir -p /app

WORKDIR /app

EXPOSE 8001

COPY project/target/shortlink-project-1.0.jar ./app.jar

CMD sh -c 'PROFILE=$(echo "${JVM_PROFILE:-g1}" | tr "[:upper:]" "[:lower:]"); if [ "$PROFILE" = "zgc" ]; then JAVA_OPTS="$JAVA_OPTS_ZGC"; elif [ "$PROFILE" = "g1" ]; then JAVA_OPTS="$JAVA_OPTS_G1"; else JAVA_OPTS="$JAVA_OPTS_DEFAULT"; fi; exec java $JAVA_OPTS $JAVA_OPTS_EXTRA -jar app.jar'
