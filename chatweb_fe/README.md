# ChatWeb Frontend

Giao diện người dùng cho hệ thống ChatWeb. Được thiết kế tập trung vào trải nghiệm mượt mà, kết nối thời gian thực (real-time) và giao diện hiện đại.

## 🚀 Công nghệ sử dụng
- **Core**: React 19, Vite (Trình đóng gói và môi trường phát triển siêu tốc)
- **Giao diện (UI/CSS)**: Tailwind CSS 4, Framer Motion (Animation mượt mà), Lucide React (Icons)
- **Quản lý trạng thái (State)**: Redux Toolkit, React-Redux
- **Định tuyến (Routing)**: React Router DOM 7
- **Kết nối thời gian thực (WebSockets)**: `@stomp/stompjs`, `sockjs-client`
- **Đa ngôn ngữ (i18n)**: i18next, react-i18next
- **Call API**: Axios
- **Công cụ kiểm tra mã (Linting)**: Oxlint (Linter hiệu suất cao)

## 🛠 Hướng dẫn chạy cục bộ

### 1. Yêu cầu
- Node.js (khuyến nghị phiên bản LTS mới nhất)

### 2. Cài đặt và Khởi chạy
1. Cài đặt các gói phụ thuộc (dependencies):
   ```bash
   npm install
   ```

2. Khởi chạy máy chủ phát triển (Development Server):
   ```bash
   npm run dev
   ```

3. Mở trình duyệt và truy cập vào địa chỉ (thường là `http://localhost:5173`):
   👉 **[http://localhost:5173](http://localhost:5173)**

## 📜 Các lệnh cơ bản
- `npm run dev`: Chạy ứng dụng ở chế độ phát triển (HMR).
- `npm run build`: Đóng gói ứng dụng tối ưu hóa cho môi trường Production (vào thư mục `dist`).
- `npm run preview`: Chạy thử bản build production trên máy local.
- `npm run lint`: Chạy Oxlint để kiểm tra lỗi code và quy tắc chuẩn.
