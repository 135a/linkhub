# Stage 1: Build the Vue application
FROM node:18-alpine AS build-stage

WORKDIR /app

# Copy package files
COPY console-vue/package*.json ./

# Install dependencies
RUN npm install --registry=https://registry.npmmirror.com

# Copy source code
COPY console-vue/ .

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

# Expose port 80
EXPOSE 80

# Health check
HEALTHCHECK --interval=30s --timeout=3s \
    CMD wget --quiet --tries=1 --spider http://localhost:80/ || exit 1

# Start nginx
CMD ["nginx", "-g", "daemon off;"]
