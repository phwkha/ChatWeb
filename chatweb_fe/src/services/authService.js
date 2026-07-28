import { api } from "./api";
import { userService } from "./userService";

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
            const response = await api.post("/api/auth/login", { username, password });
            const userColor = generateUserColor();
            const userData = {
                ...response.data,
                color: userColor,
                loginTime: new Date().toISOString()
            };
            localStorage.setItem("currentUser", JSON.stringify(userData));
            return { success: true, user: userData };
        } catch (error) {
            throw new Error(error.message);
        }
    },

    signup: async ({ username, email, password }) => {
        try {
            const response = await api.post("/api/auth/register", { username, email, password });
            return { success: true, user: response.data };
        } catch (error) {
            throw new Error(error.message);
        }
    },

    logout: async () => {
        try {
            await api.post("/api/auth/logout");
        } catch (error) {
            console.error("Logout error:", error);
            throw error;
        } finally {
            localStorage.removeItem("currentUser");
        }
    },

    logoutAll: async () => {
        try {
            await api.post("/api/auth/logout-all-devices");
        } catch (error) {
            console.error("Logout all devices error:", error);
            throw error;
        } finally {
            localStorage.removeItem("currentUser");
        }
    },

    verifyAccount: async (email, otp) => {
        try {
            const response = await api.post("/api/auth/verify-account", { email, otp });
            return response.data;
        } catch (error) {
            console.error("Verify account error:", error);
            throw error;
        }
    },

    resendOtp: async (email) => {
        try {
            const response = await api.post("/api/auth/resend-otp", { email });
            return response.data;
        } catch (error) {
            console.error("Resend OTP error:", error);
            throw error;
        }
    },

    forgotPassword: async (email) => {
        try {
            const response = await api.post("/api/auth/forgot-password", { email });
            return response.data;
        } catch (error) {
            console.error("Forgot password error:", error);
            throw error;
        }
    },

    resetPassword: async (email, otp, newPassword) => {
        try {
            const response = await api.post("/api/auth/reset-password", { email, otp, newPassword });
            return response.data;
        } catch (error) {
            console.error("Reset password error:", error);
            throw error;
        }
    },

    getCurrentUser: () => {
        const currentUserStr = localStorage.getItem("currentUser");
        try {
            if (currentUserStr) {
                return JSON.parse(currentUserStr);
            }
            return null;
        } catch (error) {
            console.error("Error parsing user data:", error);
            return null;
        }
    },

    isAuthenticated: () => {
        return !!localStorage.getItem("currentUser");
    }
};
