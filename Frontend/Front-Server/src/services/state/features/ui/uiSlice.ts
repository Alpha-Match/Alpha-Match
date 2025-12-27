import { createSlice, PayloadAction } from '@reduxjs/toolkit';
import { UserMode } from '../../../../types';

type PageViewMode = 'dashboard' | 'results' | 'detail';

export interface ModeSpecificUiState {
  pageViewMode: PageViewMode;
  selectedMatchId: string | null;
}

interface UiState {
  isSidebarOpen: boolean;
  theme: 'light' | 'dark';
  viewResetCounter: number;
  activeTooltipId: string | null;
  userMode: UserMode;
  [UserMode.CANDIDATE]: ModeSpecificUiState;
  [UserMode.RECRUITER]: ModeSpecificUiState;
}

const initialModeSpecificUiState: ModeSpecificUiState = {
  pageViewMode: 'dashboard',
  selectedMatchId: null,
};

const initialState: UiState = {
  isSidebarOpen: true,
  theme: 'dark',
  viewResetCounter: 0,
  activeTooltipId: null,
  userMode: UserMode.CANDIDATE, // Default mode
  [UserMode.CANDIDATE]: { ...initialModeSpecificUiState },
  [UserMode.RECRUITER]: { ...initialModeSpecificUiState },
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
    resetView(state, action: PayloadAction<UserMode>) {
      // Resets view state for a specific user mode
      state.viewResetCounter += 1; // Global counter still increments for debugging/tracking
      state[action.payload].pageViewMode = 'dashboard';
      state[action.payload].selectedMatchId = null;
    },
    setActiveTooltip(state, action: PayloadAction<string | null>) {
      state.activeTooltipId = action.payload;
    },
    setUserMode(state, action: PayloadAction<UserMode>) {
      state.userMode = action.payload;
    },
    setPageViewMode(state, action: PayloadAction<{ userMode: UserMode; pageViewMode: PageViewMode }>) {
        state[action.payload.userMode].pageViewMode = action.payload.pageViewMode;
    },
    setSelectedMatchId(state, action: PayloadAction<{ userMode: UserMode; selectedMatchId: string | null }>) {
        state[action.payload.userMode].selectedMatchId = action.payload.selectedMatchId;
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

