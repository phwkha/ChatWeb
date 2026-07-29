import { createSlice } from '@reduxjs/toolkit';

const initialState = {
  activeChatId: null,
  conversations: [], // List of friends/rooms
  messages: {}, // Map of chatId -> Array of messages
  unreadCounts: {}, // Map of chatId -> count
  onlineUsers: [],
};

const chatSlice = createSlice({
  name: 'chat',
  initialState,
  reducers: {
    setActiveChat(state, action) {
      state.activeChatId = action.payload;
    },
    setConversations(state, action) {
      state.conversations = action.payload;
    },
    addMessage(state, action) {
      const { chatId, message } = action.payload;
      if (!state.messages[chatId]) {
        state.messages[chatId] = [];
      }
      state.messages[chatId].push(message);
    },
    setMessages(state, action) {
      const { chatId, messages } = action.payload;
      state.messages[chatId] = messages;
    },
    setUnreadCount(state, action) {
      const { chatId, count } = action.payload;
      state.unreadCounts[chatId] = count;
    },
    setOnlineUsers(state, action) {
      state.onlineUsers = action.payload;
    }
  }
});

export const { 
  setActiveChat, 
  setConversations, 
  addMessage, 
  setMessages, 
  setUnreadCount,
  setOnlineUsers
} = chatSlice.actions;

export default chatSlice.reducer;
