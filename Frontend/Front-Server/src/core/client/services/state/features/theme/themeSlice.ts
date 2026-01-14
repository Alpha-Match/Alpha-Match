'use client';

import { createSlice, PayloadAction } from '@reduxjs/toolkit';

export interface ThemeState {
  theme: 'light' | 'dark';
}

const initialState: ThemeState = {
  theme: 'dark', // Default theme
};

export const themeSlice = createSlice({
  name: 'theme',
  initialState,
  reducers: {
    setTheme: (state, action: PayloadAction<'light' | 'dark'>) => {
      state.theme = action.payload;
    },
  },
});

export const { setTheme } = themeSlice.actions;

export default themeSlice.reducer;
