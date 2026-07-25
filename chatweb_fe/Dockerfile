# Bác bỏ (Ignore) các file không cần thiết trong .dockerignore

# Giai đoạn 1: Build ứng dụng React với Node.js
FROM node:20-alpine as build-stage

# Thiết lập thư mục làm việc trong container
WORKDIR /app

# Copy file package.json và package-lock.json (nếu có)
COPY package*.json ./

# Cài đặt các dependencies
RUN npm install

# Copy toàn bộ mã nguồn vào container
COPY . .

# Nhận biến môi trường từ docker-compose lúc build
ARG VITE_API_URL
ARG VITE_SOCKET_URL
ENV VITE_API_URL=$VITE_API_URL
ENV VITE_SOCKET_URL=$VITE_SOCKET_URL

# Build ứng dụng Vite
RUN npm run build

# Giai đoạn 2: Phục vụ file tĩnh với Nginx
FROM nginx:alpine as production-stage

# Copy cấu hình Nginx tùy chỉnh (hỗ trợ React Router)
COPY nginx.conf /etc/nginx/conf.d/default.conf

# Copy thư mục đã build từ giai đoạn 1 sang thư mục phục vụ của Nginx
# Vite mặc định xuất file ra thư mục dist
COPY --from=build-stage /app/dist /usr/share/nginx/html

# Expose port 80
EXPOSE 80

# Chạy Nginx
CMD ["nginx", "-g", "daemon off;"]
