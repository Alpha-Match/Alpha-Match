import { createSlice, PayloadAction } from '@reduxjs/toolkit';
import { UserMode } from '../../../../types';

type PageViewMode = 'dashboard' | 'results' | 'detail';

export interface HistoryEntry {
  pageViewMode: PageViewMode;
  selectedMatchId: string | null;
}

export interface ModeSpecificUiState {
  history: HistoryEntry[];
  currentIndex: number;
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
  history: [{ pageViewMode: 'dashboard', selectedMatchId: null }],
  currentIndex: 0,
};

const initialState: UiState = {
  isSidebarOpen: true,
  theme: 'dark',
  viewResetCounter: 0,
  activeTooltipId: null,
  userMode: UserMode.CANDIDATE, // Default mode
  [UserMode.CANDIDATE]: { ...initialModeSpecificUiState },
  [UserMode.RECRUITER]: { 
    ...initialModeSpecificUiState,
    history: [{ pageViewMode: 'dashboard', selectedMatchId: null }],
    currentIndex: 0,
  },
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
      state.viewResetCounter += 1;
      state[action.payload] = { ...initialModeSpecificUiState };
    },
    setActiveTooltip(state, action: PayloadAction<string | null>) {
      state.activeTooltipId = action.payload;
    },
    setUserMode(state, action: PayloadAction<UserMode>) {
      state.userMode = action.payload;
    },
    pushHistory(state, action: PayloadAction<{ userMode: UserMode; view: HistoryEntry }>) {
      const { userMode, view } = action.payload;
      const modeState = state[userMode];
      
      // If we navigate forward from a past state, trim the future history
      const newHistory = modeState.history.slice(0, modeState.currentIndex + 1);
      
      newHistory.push(view);
      modeState.history = newHistory;
      modeState.currentIndex = newHistory.length - 1;
    },
    navigateBack(state, action: PayloadAction<{ userMode: UserMode }>) {
        const { userMode } = action.payload;
        const modeState = state[userMode];
        if (modeState.currentIndex > 0) {
            modeState.currentIndex -= 1;
        }
    }
  },
});

export const { 
    toggleSidebar, 
    setTheme, 
    resetView, 
    setActiveTooltip, 
    setUserMode,
    pushHistory,
    navigateBack
} = uiSlice.actions;
export default uiSlice.reducer;

