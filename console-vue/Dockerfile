# Stage 1: Build the Vue application
FROM node:18-alpine AS build-stage

WORKDIR /app

# Copy package files
COPY console-vue/package*.json ./

# Install dependencies
RUN npm install --registry=https://registry.npmmirror.com

# Copy source code
COPY console-vue/ .

ARG VITE_API_BASE_URL=/api/short-link/admin/v1
ENV VITE_API_BASE_URL=${VITE_API_BASE_URL}

# Build the application
RUN npm run build

# Stage 2: Serve with Nginx
FROM nginx:1.25-alpine AS production-stage

# Remove default nginx config
RUN rm /etc/nginx/conf.d/default.conf

# Copy custom nginx config
COPY console-vue/nginx.conf /etc/nginx/conf.d/default.conf

# Copy built files from build stage
COPY --from=build-stage /app/dist /usr/share/nginx/html

# Expose port 3000
EXPOSE 3000

# Health check
HEALTHCHECK --interval=30s --timeout=3s \
    CMD wget --quiet --tries=1 --spider http://localhost:3000/ || exit 1

# Start nginx
CMD ["nginx", "-g", "daemon off;"]
