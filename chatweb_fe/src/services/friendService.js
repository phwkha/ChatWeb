import { api } from "./api";

export const friendService = {
  getRequests: async (page = 0, size = 10) => {
    try {
      const response = await api.get(
        `/api/friends/requests?page=${page}&size=${size}`,
      );
      return response.data;
    } catch (error) {
      console.error("Fetch friend requests error:", error);
      throw error;
    }
  },

  getSentRequests: async (page = 0, size = 10) => {
    try {
      const response = await api.get(
        `/api/friends/sent?page=${page}&size=${size}`,
      );
      return response.data;
    } catch (error) {
      console.error("Fetch sent requests error:", error);
      throw error;
    }
  },

  getFriendsList: async (page = 0, size = 10) => {
    try {
      const response = await api.get(`/api/friends?page=${page}&size=${size}`);
      return response.data;
    } catch (error) {
      console.error("Fetch friends list error:", error);
      throw error;
    }
  },

  deleteFriendship: async (username) => {
    try {
      const response = await api.delete(`/api/friends/${username}`);
      return response.data;
    } catch (error) {
      console.error("Delete friendship error:", error);
      throw error;
    }
  },

  blockUser: async (username) => {
    try {
      const response = await api.post(`/api/friends/block/${username}`);
      return response.data;
    } catch (error) {
      console.error("Block user error:", error);
      throw error;
    }
  },
};
