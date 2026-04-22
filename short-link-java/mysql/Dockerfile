FROM mysql:8.0

# Set timezone
ENV TZ=Asia/Shanghai
RUN ln -sf /usr/share/zoneinfo/$TZ /etc/localtime && echo $TZ > /etc/timezone

# Copy SQL initialization files
COPY ./resources/database/link.sql /docker-entrypoint-initdb.d/1-link.sql

COPY ./resources/database/link-data.sql /docker-entrypoint-initdb.d/2-link-data.sql

# Copy Nacos config database initialization script
COPY ./resources/nacos-config.sql /docker-entrypoint-initdb.d/3-nacos-config.sql

# Expose MySQL port
EXPOSE 3306
