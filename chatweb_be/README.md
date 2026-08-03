# ChatWeb Backend

Đây là repository backend của hệ thống ChatWeb. Cung cấp API mạnh mẽ, bảo mật và khả năng kết nối thời gian thực cho ứng dụng chat.

## 🚀 Công nghệ sử dụng
- **Ngôn ngữ & Framework**: Java 21, Spring Boot 3
- **Cơ sở dữ liệu (Database)**: 
  - PostgreSQL (Dữ liệu quan hệ, người dùng, v.v.)
  - MongoDB (Dữ liệu phi quan hệ, tin nhắn chat)
- **Bộ nhớ đệm (Caching)**: Redis
- **Message Broker**: Apache Kafka (Xử lý hàng đợi và luồng dữ liệu)
- **Bảo mật**: Spring Security, JWT (JSON Web Tokens), OAuth2 Client
- **Kết nối thời gian thực (Real-time)**: Spring Boot WebSocket
- **Lưu trữ file đa phương tiện**: Cloudinary
- **Log & Giám sát hệ thống (Monitoring)**: Prometheus, Logstash, ELK stack (Filebeat)
- **Tài liệu API**: Swagger (Springdoc OpenAPI)

## 🛠 Hướng dẫn chạy ứng dụng cục bộ

### 1. Yêu cầu hệ thống
- Java 21
- Maven
- Cài đặt và chạy sẵn PostgreSQL, MongoDB, Redis, Kafka (Có thể dùng Docker).

### 2. Cài đặt và chạy
1. Di chuyển vào thư mục backend:
   ```bash
   cd chatweb_be
   ```
2. Cấu hình biến môi trường kết nối database trong `application.yml` hoặc các file `.env` tương ứng.
3. Chạy ứng dụng bằng Maven:
   ```bash
   ./mvnw spring-boot:run
   ```

### 3. Xem Tài liệu API (Swagger)
Khi ứng dụng đã chạy thành công, truy cập Swagger UI để kiểm thử API tại địa chỉ:
👉 **[http://localhost:8080/swagger-ui/index.html](http://localhost:8080/swagger-ui/index.html)**

## 🐳 Docker (Jib)
Dự án sử dụng Google Jib plugin để build Docker image một cách tối ưu, không cần đến Docker daemon:
```bash
./mvnw compile jib:build
```
*(Cần cấu hình tài khoản Docker trong `pom.xml` hoặc truyền tham số tương ứng).*
