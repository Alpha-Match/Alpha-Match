'use client';

import { createSlice, PayloadAction } from '@reduxjs/toolkit';
import { ExperienceLevel, UserMode } from '../../../../types'; // UserMode 임포트

export interface ModeSpecificSearchState {
  selectedSkills: string[];
  selectedExperience: string | null;
  isInitial: boolean; // 해당 모드에서 검색이 수행되었는지 여부
}

export interface SearchState {
  [UserMode.CANDIDATE]: ModeSpecificSearchState;
  [UserMode.RECRUITER]: ModeSpecificSearchState;
  skillCategories: string[];
  skillsLoaded: boolean;
}

const initialModeSpecificState: ModeSpecificSearchState = {
  selectedSkills: [],
  selectedExperience: ExperienceLevel.MID, // 기본 경력 수준
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
        state[action.payload] = { ...initialModeSpecificState }; // 현재 모드의 상태만 리셋
    },
    setSkillCategories: (state, action: PayloadAction<string[]>) => {
      state.skillCategories = action.payload;
      state.skillsLoaded = true;
    },
  },
});

// 향후 리팩토링 참고:
// 검색 작업과 같이 여러 디스패치와 비동기 호출을 포함하는 더 복잡한 비동기 로직의 경우,
// createAsyncThunk 사용을 고려해볼 수 있습니다.
// 이를 통해 page.tsx의 `handleSearch`와 같은 컴포넌트의 로직을
// 이 슬라이스로 추상화하여 애플리케이션의 비즈니스 로직을 중앙에서 관리할 수 있습니다.
// 이를 위해서는 `runSearch` 함수를 전달하거나 지연 쿼리가 호출되는 위치를 리팩토링해야 합니다.

export const { 
  toggleSkill, 
  setExperience, 
  setSearchPerformed, 
  resetSearch,
  setSkillCategories,
} = searchSlice.actions;

export default searchSlice.reducer;


