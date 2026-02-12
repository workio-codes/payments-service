FROM node:20-alpine AS build
WORKDIR /app

ARG BACKEND_API_URL=http://localhost:9090
ENV BACKEND_API_URL=$BACKEND_API_URL

COPY package*.json ./
RUN npm ci

COPY . .
RUN npm run build

FROM nginx:1.27-alpine
WORKDIR /usr/share/nginx/html

COPY --from=build /app/dist ./
COPY nginx.conf /etc/nginx/conf.d/default.conf

EXPOSE 80
