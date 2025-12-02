import axios from "axios";
import { cryptoService } from "./cryptoService";

const API_URL = import.meta.env.VITE_API_URL;

const api = axios.create({
    baseURL: API_URL,
    headers: {
        "Content-Type": "application/json",
    },
    withCredentials: true
});

// response interceptor fro global error handling
api.interceptors.response.use(
    (response) => response,
    (error) => {
        if (error.response) {
            switch (error.response.status) {
                case 401:
                    // unauthorized
                    authService.logout();
                    window.location.href = "/login";
                    break;
                case 403:
                    // Access forbidden
                    console.error("Access forbidden");
                    break;
                case 404:
                    // resource not found
                    console.error("Resource not found");
                    break;
                case 500:
                    // server error
                    console.error("Server error");
                    break;
                default:
                    break;
            }
        } else if (error.request) {
            console.error("No response received from server:", error.request);
        } else {
            console.error("Error setting up request:", error.message);
        }
        return Promise.reject(error);
    }
);

const generateUserColor = () => {
    const colors = [
        "#e6194b", "#3cb44b", "#ffe119", "#4363d8", "#f58231",
        "#911eb4", "#46f0f0", "#f032e6", "#bcf60c", "#fabebe",
        "#008080", "#e6beff", "#9a6324", "#fffac8", "#800000",
        "#aaffc3", "#808000", "#ffd8b1", "#000075", "#808080"
    ];
    return colors[Math.floor(Math.random() * colors.length)];
}

export const authService = {

    login: async (username, password) => {
        try {
            const response = await api.post("/auth/login", { username, password });
            console.log("Login response:", response.data);

            const userColor = generateUserColor();
            const userData = {
                ...response.data,
                color: userColor,
                loginTime: new Date().toISOString()
            };
            localStorage.setItem("currentUser", JSON.stringify(userData));

            console.log("Login successful:", response.data);
            return { success: true, user: userData };
        } catch (error) {
            console.error("Login error:", error);
            const errorMessage = error.response?.data?.message || "Login failed. Please try again.";
            throw new Error(errorMessage);

        }
    },

    signup: async ({ username, email, password }) => {
        try {
            const response = await api.post("/user/register", { username, email, password });
            return { success: true, user: response.data };
        } catch (error) {
            console.error("Signup error:", error);
            const errorMessage = error.response?.data?.message || "Signup failed. Please try again.";
            throw new Error(errorMessage);
        }
    },

    logout: async () => {
        try {
            await api.post("/auth/logout");
        } catch (error) {
            console.error("Logout error:", error);
        } finally {
            localStorage.removeItem("currentUser");
        }
    },

    fetchCurrentUser: async () => {
        try {
            const response = await api.get("/user/current");
            localStorage.setItem("currentUser", JSON.stringify(response.data));
            return response.data;
        } catch (error) {
            console.error("Fetch current user error:", error);

            if (error.response && error.response.status === 401) {
                await authService.logout();
                window.location.href = "/login";
            }
        }
    },

    getCurrentUser: () => {
        const currentUserStr = localStorage.getItem("currentUser");
        try {
            if (currentUserStr) {
                return JSON.parse(currentUserStr);
            } else {
                return authService.fetchCurrentUser();
            }

        } catch (error) {
            console.error("Error parsing user data:", error);
            return null;
        }
    },
    isAuthenticated: () => {
        const user = localStorage.getItem("currentUser");
        return !!user;
    },
    fetchPrivateChat: async (user1, user2, cursor = null, size = 20) => {
        try {
            let url = `/api/messages/private?user1=${user1}&user2=${user2}&size=${size}`;
            if (cursor) {
                url += `&cursor=${cursor}`;
            }
            const response = await api.get(url);
            return response.data; // Trả về object { content: [], nextCursor: "...", hasMore: true/false }
        } catch (error) {
            console.error("Fetch private chat error:", error);
            throw error; 
        }
    },
    fetchGroupChat: async (cursor = null, size = 20) => {
        try {
            let url = `/api/messages/group?size=${size}`;
            if (cursor) {
                url += `&cursor=${cursor}`;
            }
            const response = await api.get(url);
            return response.data;
        } catch (error) {
            console.error("Fetch group chat error:", error);
            throw error;
        }
    },
    getOnlineUsers: async () => {
        try {
            const response = await api.get("/user/online");
            return response.data;
        } catch (error) {
            console.error("Fetch online users error:", error);
        }
    },
    getPublicKey: async (username) => {
        try {
            const response = await api.get(`/user/public-key/${username}`);
            if (response.data && response.data.publicKey) {
                return cryptoService.importPublicKey(response.data.publicKey);
            }
            return null;
        } catch (error) {
            console.error("Fetch public key error:", error);
            throw error;
        }
    },
    savePublicKey: async (publicKey) => {
        try {
            const exportedKey = await cryptoService.exportPublicKey(publicKey);
            await api.post("/user/public-key", { publicKey: exportedKey });
        } catch (error) {
            console.error("Save public key error:", error);
            throw error;
        }
    },
    getEncryptedRsaKey: async () => {
        try {
            const response = await api.get("/api/keys/rsa");
            return response.data.key; 
        } catch (error) {
            console.error("Get RSA key error:", error);
            throw error;
        }
    },
    saveEncryptedRsaKey: async (encryptedKey) => {
        try {
            await api.post("/api/keys/rsa", { key: encryptedKey });
        } catch (error) {
            console.error("Save RSA key error:", error);
            throw error;
        }
    },
    getUnreadCounts: async () => {
        try {
            const response = await api.get("/api/messages/unread-counts");
            return response.data;
        } catch (error) {
            console.error("Fetch unread counts error:", error);
            return {};
        }
    },

    markAsRead: async (senderUsername) => {
        try {
            await api.post("/api/messages/mark-as-read", { sender: senderUsername });
        } catch (error) {
            console.error("Mark as read error:", error);
        }
    },
    updateUserProfile: async (username, userData) => {
        try {
            // userData: { fullName, email, phone }
            const response = await api.put(`/user/${username}`, userData);
            
            const currentUser = JSON.parse(localStorage.getItem("currentUser"));
            if (currentUser && currentUser.username === username) {
                const updatedUser = { ...currentUser, ...response.data };
                localStorage.setItem("currentUser", JSON.stringify(updatedUser));
            }
            
            return response.data;
        } catch (error) {
            console.error("Update user profile error:", error);
            throw error;
        }
    },

    changePassword: async (currentPassword, newPassword) => {
        try {
            const response = await api.post("/user/change-password", {
                currentPassword,
                newPassword
            });
            return response.data;
        } catch (error) {
            console.error("Change password error:", error);
            throw error.response?.data || error.message || "Lỗi đổi mật khẩu";
        }
    },

    // --- ADMIN API ---
    getAllUsers: async () => {
        try {
            const response = await api.get("/admin/users");
            return response.data;
        } catch (error) {
            console.error("Get all users error:", error);
            throw error;
        }
    },

    adminCreateUser: async (userData) => { // userData: {username, password, email, role}
        try {
            const response = await api.post("/admin/add", userData);
            return response.data;
        } catch (error) {
            throw error;
        }
    },

    adminUpdateUser: async (username, userData) => { // userData: {fullName, email, phone, role}
        try {
            const response = await api.put(`/admin/${username}`, userData);
            return response.data;
        } catch (error) {
            throw error;
        }
    },

    lockUser: async (username) => {
        try {
            const response = await api.post(`/admin/${username}/lock`);
            return response.data;
        } catch (error) {
            throw error;
        }
    },

    unlockUser: async (username) => {
        try {
            const response = await api.post(`/admin/${username}/unlock`);
            return response.data;
        } catch (error) {
            throw error;
        }
    },

    deleteUserAdmin: async (username) => {
        try {
            await api.delete(`/admin/${username}`);
        } catch (error) {
            throw error;
        }
    },

};

