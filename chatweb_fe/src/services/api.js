import axios from "axios";

const API_URL = import.meta.env.VITE_API_URL;

export const api = axios.create({
    baseURL: API_URL,
    headers: {
        "Content-Type": "application/json",
    },
    withCredentials: true
});

// response interceptor fro global error handling
api.interceptors.response.use(
    (response) => {
        // Unwrap backend ApiResponse wrapper
        if (response.data && response.data.status === 'success') {
            response.message = response.data.message; // preserve the backend success message
            response.data = response.data.data;       // unwrap the actual data payload
        }
        return response;
    },
    (error) => {
        let errorMessage = "Lỗi hệ thống";
        
        if (error.response && error.response.data && error.response.data.message) {
            errorMessage = error.response.data.message;
        } else if (error.response) {
            switch (error.response.status) {
                case 401:
                    errorMessage = "Phiên đăng nhập hết hạn.";
                    localStorage.removeItem("currentUser");
                    window.location.href = "/login";
                    break;
                case 403:
                    errorMessage = "Bạn không có quyền truy cập.";
                    console.error("Access forbidden");
                    break;
                case 404:
                    errorMessage = "Không tìm thấy dữ liệu.";
                    console.error("Resource not found");
                    break;
                case 500:
                    errorMessage = "Lỗi máy chủ nội bộ.";
                    console.error("Server error");
                    break;
                default:
                    errorMessage = `Lỗi HTTP ${error.response.status}`;
                    break;
            }
        } else if (error.request) {
            errorMessage = "Không thể kết nối đến máy chủ.";
            console.error("No response received from server:", error.request);
        } else {
            errorMessage = error.message;
            console.error("Error setting up request:", error.message);
        }
        
        error.message = errorMessage;
        return Promise.reject(error);
    }
);
