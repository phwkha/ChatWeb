import { api } from "./api";

export const adminService = {
    getAllUsers: async () => {
        try {
            const response = await api.get("/api/admin/users");
            return response.data;
        } catch (error) {
            console.error("Get all users error:", error);
            throw error;
        }
    },

    adminCreateUser: async (userData) => {
        const response = await api.post("/api/admin/add", userData);
        return response.data;
    },

    adminUpdateUser: async (username, userData) => {
        const response = await api.put(`/api/admin/${username}`, userData);
        return response.data;
    },

    lockUser: async (username) => {
        const response = await api.post(`/api/admin/${username}/lock`);
        return response.data;
    },

    unlockUser: async (username) => {
        const response = await api.post(`/api/admin/${username}/unlock`);
        return response.data;
    },

    deleteUserAdmin: async (username) => {
        await api.delete(`/api/admin/${username}`);
    }
};
