import { api } from "./api";

export const userService = {
    fetchCurrentUser: async () => {
        try {
            const response = await api.get("/api/users/me");
            localStorage.setItem("currentUser", JSON.stringify(response.data));
            return response.data;
        } catch (error) {
            console.error("Fetch current user error:", error);
            if (error.response && error.response.status === 401) {
                localStorage.removeItem("currentUser");
                window.location.href = "/login";
            }
        }
    },

    getOnlineUsers: async () => {
        try {
            const response = await api.get("/api/admin/online");
            return response.data;
        } catch (error) {
            console.error("Fetch online users error:", error);
            throw error;
        }
    },

    updateAvatar: async (avatarUrl) => {
        try {
            const response = await api.patch("/api/users/avatar", { url: avatarUrl });
            return response.data;
        } catch (error) {
            console.error("Update avatar error:", error);
            throw error;
        }
    },

    updateUserProfile: async (username, userData) => {
        try {
            const response = await api.put("/api/users/profile", userData);
            return response.data;
        } catch (error) {
            console.error("Update profile error:", error);
            throw error;
        }
    },
    
    // Address endpoints
    getAddresses: async () => {
        const response = await api.get("/api/users/addresses");
        return response.data;
    },
    
    addAddress: async (addressData) => {
        const response = await api.post("/api/users/address", addressData);
        return response.data;
    },
    
    updateAddress: async (addressId, addressData) => {
        const response = await api.put(`/api/users/address/${addressId}`, addressData);
        return response.data;
    },
    
    deleteAddress: async (addressId) => {
        const response = await api.delete(`/api/users/address/${addressId}`);
        return response.data;
    },
    
    initiateEmailChange: async (newEmail, currentPassword) => {
        const response = await api.post("/api/users/initiate-email-change", { newEmail, currentPassword });
        return response.data;
    },
    
    verifyEmailChange: async (email, otp) => {
        const response = await api.post("/api/users/verify-email-change", { email, otp });
        return response.data;
    },
    
    resendEmailChangeOtp: async () => {
        const response = await api.post("/api/users/resend-email-verification");
        return response.data;
    },
    
    initiatePhoneChange: async (newPhone, currentPassword) => {
        const response = await api.post("/api/users/initiate-phone-change", { newPhone, currentPassword });
        return response.data;
    },
    
    verifyPhoneChange: async (email, otp) => {
        const response = await api.post("/api/users/verify-phone-change", { email, otp });
        return response.data;
    },
    
    resendPhoneChangeOtp: async () => {
        const response = await api.post("/api/users/resend-phone-change-verification");
        return response.data;
    },

    updateUserProfile: async (username, userData) => {
        try {
            const response = await api.put("/api/users/profile", userData);
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
            const response = await api.post("/api/users/change-password", {
                currentPassword,
                newPassword
            });
            return response.data;
        } catch (error) {
            console.error("Change password error:", error);
            throw new Error(error.message);
        }
    }
};
