'use client';

import { createSlice, PayloadAction } from '@reduxjs/toolkit';
import { ExperienceLevel } from '../../../../types';

export interface SearchState {
  selectedSkills: string[];
  selectedExperience: string;
  isInitial: boolean;
  skillCategories: string[];
  skillsLoaded: boolean;
}

const initialState: SearchState = {
  selectedSkills: [],
  selectedExperience: ExperienceLevel.MID,
  isInitial: true,
  skillCategories: [],
  skillsLoaded: false,
};

export const searchSlice = createSlice({
  name: 'search',
  initialState,
  reducers: {
    toggleSkill: (state, action: PayloadAction<string>) => {
      const skill = action.payload;
      const index = state.selectedSkills.indexOf(skill);
      if (index >= 0) {
        state.selectedSkills.splice(index, 1);
      } else {
        state.selectedSkills.push(skill);
      }
    },
    setExperience: (state, action: PayloadAction<string>) => {
      state.selectedExperience = action.payload;
    },
    setSearchPerformed: (state) => {
      state.isInitial = false;
    },
    resetSearch: (state) => {
        state.isInitial = true;
        state.selectedSkills = [];
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

