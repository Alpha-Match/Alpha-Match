'use client';

import { createSlice, PayloadAction } from '@reduxjs/toolkit';

export interface NotificationState {
  message: string | null;
  type: 'error' | 'success' | 'info';
  open: boolean;
}

const initialState: NotificationState = {
  message: null,
  type: 'info',
  open: false,
};

export const notificationSlice = createSlice({
  name: 'notification',
  initialState,
  reducers: {
    showNotification: (state, action: PayloadAction<{ message: string; type: 'error' | 'success' | 'info' }>) => {
      state.message = action.payload.message;
      state.type = action.payload.type;
      state.open = true;
    },
    hideNotification: (state) => {
      state.open = false;
    },
  },
});

export const { showNotification, hideNotification } = notificationSlice.actions;

export default notificationSlice.reducer;
