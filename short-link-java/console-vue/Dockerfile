FROM node:22-alpine AS builder

WORKDIR /app

# Copy package files
COPY package.json ./

# Install dependencies
RUN npm install

# Copy source code
COPY . .

# Public URLs baked at build time (override via docker-compose build.args)
ARG VITE_SHORT_LINK_PUBLIC_SCHEME=http
ARG VITE_PUBLIC_SHORTLINK_BASE_URL=
ARG VITE_PUBLIC_LOG_CONSOLE_URL=http://localhost:3001
ENV VITE_SHORT_LINK_PUBLIC_SCHEME=$VITE_SHORT_LINK_PUBLIC_SCHEME
ENV VITE_PUBLIC_SHORTLINK_BASE_URL=$VITE_PUBLIC_SHORTLINK_BASE_URL
ENV VITE_PUBLIC_LOG_CONSOLE_URL=$VITE_PUBLIC_LOG_CONSOLE_URL

# Build the project
RUN npm run build

# Production stage
FROM nginx:alpine

# Copy built assets to Nginx
COPY --from=builder /app/dist /usr/share/nginx/html

# Nginx config for Vue router (if using history mode) or general static file serving
RUN echo 'server { \
    listen       80; \
    server_name  localhost; \
    location / { \
        root   /usr/share/nginx/html; \
        index  index.html index.htm; \
        try_files $uri $uri/ /index.html; \
    } \
    error_page   500 502 503 504  /50x.html; \
    location = /50x.html { \
        root   /usr/share/nginx/html; \
    } \
}' > /etc/nginx/conf.d/default.conf

# Expose port 80
EXPOSE 80

# Start Nginx
CMD ["nginx", "-g", "daemon off;"]
