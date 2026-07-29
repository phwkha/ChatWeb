import { createSlice } from '@reduxjs/toolkit';

const initialState = {
  user: null,
  isAuthenticated: false,
  isInitialized: false,
  loading: false,
  error: null,
};

const authSlice = createSlice({
  name: 'auth',
  initialState,
  reducers: {
    setInitialized(state, action) {
      state.isInitialized = action.payload;
    },
    loginStart(state) {
      state.loading = true;
      state.error = null;
    },
    loginSuccess(state, action) {
      state.loading = false;
      state.isAuthenticated = true;
      // Depending on API response, it might be action.payload.user or action.payload directly
      state.user = action.payload.user || action.payload;
      state.isInitialized = true;
    },
    loginFailure(state, action) {
      state.loading = false;
      state.error = action.payload;
      state.isInitialized = true;
    },
    logout(state) {
      state.user = null;
      state.isAuthenticated = false;
      state.isInitialized = true;
      localStorage.removeItem('accessToken'); // Keeping for cleanup just in case
      localStorage.removeItem('refreshToken');
    },
    updateProfile(state, action) {
      if (state.user) {
        state.user = { ...state.user, ...action.payload };
      }
    }
  }
});

export const { setInitialized, loginStart, loginSuccess, loginFailure, logout, updateProfile } = authSlice.actions;
export default authSlice.reducer;
