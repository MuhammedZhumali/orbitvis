FROM node:20-alpine AS frontend-builder

WORKDIR /app

COPY frontend/package.json frontend/package-lock.json ./
RUN npm ci

COPY frontend/ ./
RUN npm run build

FROM nginx:alpine

COPY nginx/conf.d /etc/nginx/conf.d
COPY --from=frontend-builder /app/dist /usr/share/nginx/html
