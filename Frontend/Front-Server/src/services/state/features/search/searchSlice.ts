'use client';

import { createSlice, PayloadAction } from '@reduxjs/toolkit';
import { ExperienceLevel, UserMode } from '../../../../types'; // Import UserMode

export interface ModeSpecificSearchState {
  selectedSkills: string[];
  selectedExperience: string | null;
  isInitial: boolean; // Indicates if search has been performed for this mode
}

export interface SearchState {
  [UserMode.CANDIDATE]: ModeSpecificSearchState;
  [UserMode.RECRUITER]: ModeSpecificSearchState;
  skillCategories: string[];
  skillsLoaded: boolean;
}

const initialModeSpecificState: ModeSpecificSearchState = {
  selectedSkills: [],
  selectedExperience: ExperienceLevel.MID, // Default experience level
  isInitial: true,
};

const initialState: SearchState = {
  [UserMode.CANDIDATE]: { ...initialModeSpecificState },
  [UserMode.RECRUITER]: { ...initialModeSpecificState },
  skillCategories: [],
  skillsLoaded: false,
};

export const searchSlice = createSlice({
  name: 'search',
  initialState,
  reducers: {
    toggleSkill: (state, action: PayloadAction<{ userMode: UserMode; skill: string }>) => {
      const { userMode, skill } = action.payload;
      const modeState = state[userMode];
      const index = modeState.selectedSkills.indexOf(skill);
      if (index >= 0) {
        modeState.selectedSkills.splice(index, 1);
      } else {
        modeState.selectedSkills.push(skill);
      }
    },
    setExperience: (state, action: PayloadAction<{ userMode: UserMode; experience: string | null }>) => {
      const { userMode, experience } = action.payload;
      state[userMode].selectedExperience = experience;
    },
    setSearchPerformed: (state, action: PayloadAction<UserMode>) => {
      state[action.payload].isInitial = false;
    },
    resetSearch: (state, action: PayloadAction<UserMode>) => {
        state[action.payload] = { ...initialModeSpecificState }; // Reset only the current mode's state
    },
    setSkillCategories: (state, action: PayloadAction<string[]>) => {
      state.skillCategories = action.payload;
      state.skillsLoaded = true;
    },
  },
});

export const { 
  toggleSkill, 
  setExperience, 
  setSearchPerformed, 
  resetSearch,
  setSkillCategories,
} = searchSlice.actions;

export default searchSlice.reducer;


