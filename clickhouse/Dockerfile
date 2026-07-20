FROM clickhouse/clickhouse-server:24.3

# Copy init SQL into the container image to avoid Windows bind-mount path issues
COPY ./init/ /docker-entrypoint-initdb.d/
