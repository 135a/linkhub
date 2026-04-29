FROM nginx:alpine

# Remove default config and copy ours in to avoid Windows bind-mount issues
RUN rm /etc/nginx/conf.d/default.conf
COPY nginx.conf /etc/nginx/conf.d/default.conf
