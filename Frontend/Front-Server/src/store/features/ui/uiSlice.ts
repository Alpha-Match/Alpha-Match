import { createSlice, PayloadAction } from '@reduxjs/toolkit';
import { UserMode } from '../../../types/appTypes';

type PageViewMode = 'dashboard' | 'results' | 'detail';

interface UiState {
  isSidebarOpen: boolean;
  theme: 'light' | 'dark';
  viewResetCounter: number;
  activeTooltipId: string | null;
  userMode: UserMode;
  pageViewMode: PageViewMode;
  selectedMatchId: string | null;
}

const initialState: UiState = {
  isSidebarOpen: true,
  theme: 'dark',
  viewResetCounter: 0,
  activeTooltipId: null,
  userMode: UserMode.CANDIDATE, // Default mode
  pageViewMode: 'dashboard',
  selectedMatchId: null,
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
    resetView(state) {
      state.viewResetCounter += 1;
      state.pageViewMode = 'dashboard';
      state.selectedMatchId = null;
    },
    setActiveTooltip(state, action: PayloadAction<string | null>) {
      state.activeTooltipId = action.payload;
    },
    setUserMode(state, action: PayloadAction<UserMode>) {
      state.userMode = action.payload;
    },
    setPageViewMode(state, action: PayloadAction<PageViewMode>) {
        state.pageViewMode = action.payload;
    },
    setSelectedMatchId(state, action: PayloadAction<string | null>) {
        state.selectedMatchId = action.payload;
    }
  },
});

export const { 
    toggleSidebar, 
    setTheme, 
    resetView, 
    setActiveTooltip, 
    setUserMode,
    setPageViewMode,
    setSelectedMatchId
} = uiSlice.actions;
export default uiSlice.reducer;
