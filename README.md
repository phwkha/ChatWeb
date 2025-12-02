🚀 Hướng Dẫn Cài Đặt & Chạy
1. Yêu Cầu Tiên Quyết
Docker & Docker Compose (Khuyên dùng để chạy Backend + Database).

Node.js 18+ (Để chạy Frontend).

Java 17+ & Maven (Nếu muốn chạy Backend thủ công).

2. Thiết Lập Backend (chatweb_be)
Backend đã được tích hợp sẵn Docker Compose để khởi chạy Database (Mongo, Postgres) và API Server.

Di chuyển vào thư mục backend:

Bash

cd chatweb_be
Cấu hình biến môi trường: Tạo file .env từ file mẫu .env.example:

Bash

cp .env.example .env
Lưu ý: Bạn có thể giữ nguyên cấu hình mặc định trong .env.example để chạy thử nghiệm ngay lập tức.

Khởi chạy hệ thống (Database + Backend):

Bash

docker-compose up -d --build

Backend sẽ chạy tại: http://localhost:8080

Swagger API Docs: http://localhost:8080/swagger-ui.html

3. Thiết Lập Frontend (chatweb_fe)
Di chuyển vào thư mục frontend: (Mở một terminal mới tại thư mục gốc)

Bash

cd chatweb_fe
Cài đặt thư viện:

Bash

npm install
Cấu hình kết nối: Tạo file .env tại thư mục chatweb_fe với nội dung:

Properties

VITE_API_URL=http://localhost:8080
Chạy ứng dụng:

Bash

npm run dev

Frontend sẽ chạy tại: http://localhost:5173 (hoặc port hiển thị trên terminal).

🧪 Tài Khoản Demo
Sau khi khởi chạy, bạn có thể tạo tài khoản mới hoặc sử dụng Swagger để tạo User Admin (nếu chưa có dữ liệu sẵn).

Truy cập Frontend.

Đăng ký 2 tài khoản khác nhau trên 2 trình duyệt (hoặc tab ẩn danh).

Bắt đầu chat để trải nghiệm tính năng mã hóa E2EE.
