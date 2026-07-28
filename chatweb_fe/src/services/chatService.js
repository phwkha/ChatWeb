import { api } from "./api";

export const chatService = {
    fetchPrivateChat: async (user1, user2, cursor = null, size = 20) => {
        try {
            let url = `/api/messages/private?user1=${user1}&user2=${user2}&size=${size}`;
            if (cursor) url += `&cursor=${cursor}`;
            const response = await api.get(url);
            return response.data;
        } catch (error) {
            console.error("Fetch private chat error:", error);
            throw error;
        }
    },

    fetchGroupChat: async (cursor = null, size = 20) => {
        try {
            let url = `/api/systems/message?size=${size}`;
            if (cursor) url += `&cursor=${cursor}`;
            const response = await api.get(url);
            return response.data;
        } catch (error) {
            console.error("Fetch group chat error:", error);
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

    uploadImage: async (file) => {
        try {
            const formData = new FormData();
            formData.append("image", file);
            const response = await api.post("/api/chat/image", formData, {
                headers: { 'Content-Type': 'multipart/form-data' }
            });
            return response.data;
        } catch (error) {
            console.error("Upload image error:", error);
            throw error;
        }
    },

    uploadVideo: async (file) => {
        try {
            const formData = new FormData();
            formData.append("video", file);
            const response = await api.post("/api/chat/video", formData, {
                headers: { 'Content-Type': 'multipart/form-data' }
            });
            return response.data;
        } catch (error) {
            console.error("Upload video error:", error);
            throw error;
        }
    }
};
