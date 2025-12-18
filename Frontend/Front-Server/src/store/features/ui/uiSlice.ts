import { createSlice, PayloadAction } from '@reduxjs/toolkit';

interface UiState {
  isSidebarOpen: boolean;
  theme: 'light' | 'dark';
}

const initialState: UiState = {
  isSidebarOpen: true,
  theme: 'dark',
};

const uiSlice = createSlice({
  name: 'ui',
  initialState,
  reducers: {
    toggleSidebar(state) {
      state.isSidebarOpen = !state.isSidebarOpen;
    },
    setTheme(state, action: PayloadAction<'light' | 'dark'>) {
      state.theme = action.payload;
    },
  },
});

export const { toggleSidebar, setTheme } = uiSlice.actions;
export default uiSlice.reducer;
