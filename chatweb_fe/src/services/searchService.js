import { api } from "./api";

export const searchService = {
    searchUsers: async (keyword = "", page = 0, size = 10) => {
        try {
            const response = await api.get(`/api/search/users?keyword=${keyword}&page=${page}&size=${size}`);
            return response.data;
        } catch (error) {
            console.error("Search users error:", error);
            throw error;
        }
    }
};
