FROM mysql:8.0

# Set timezone
ENV TZ=Asia/Shanghai
RUN ln -sf /usr/share/zoneinfo/$TZ /etc/localtime && echo $TZ > /etc/timezone

# Copy SQL initialization files from merged mysql directory
COPY ./mysql/database/link.sql /docker-entrypoint-initdb.d/1-link.sql
COPY ./mysql/database/link-data.sql /docker-entrypoint-initdb.d/2-link-data.sql

# Expose MySQL port
EXPOSE 3306
