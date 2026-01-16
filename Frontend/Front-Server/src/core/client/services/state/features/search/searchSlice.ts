'use client';

import {createSlice, PayloadAction} from '@reduxjs/toolkit';
import {DashboardCategory, ExperienceLevel, MatchItem, SkillCategory, SkillFrequency, UserMode} from '@/types';

export interface ModeSpecificSearchState {
  selectedSkills: string[];
  searchedSkills: string[]; // 검색에 실제 사용된 스킬
  selectedExperience: string | null;
  isInitial: boolean;
  matches: MatchItem[];
  dashboardData: DashboardCategory[] | null;
  totalCount: number | null;
  topSkills: SkillFrequency[]; // GET_SEARCH_STATISTICS 결과
}

export interface SearchState {
  [UserMode.CANDIDATE]: ModeSpecificSearchState;
  [UserMode.RECRUITER]: ModeSpecificSearchState;
  skillCategories: SkillCategory[];
  skillsLoaded: boolean;
}

const initialModeSpecificState: ModeSpecificSearchState = {
  selectedSkills: [],
  searchedSkills: [], // 검색에 실제 사용된 스킬
  selectedExperience: ExperienceLevel.MID,
  isInitial: true,
  matches: [],
  dashboardData: null,
  totalCount: null,
  topSkills: [],
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
    setMatches: (state, action: PayloadAction<{ userMode: UserMode; matches: MatchItem[] }>) => {
      const { userMode, matches } = action.payload;
      state[userMode].matches = matches;
    },
    appendMatches: (state, action: PayloadAction<{ userMode: UserMode; matches: MatchItem[] }>) => {
      const { userMode, matches } = action.payload;
      // Append new matches to existing ones (for infinite scroll)
      state[userMode].matches = [...state[userMode].matches, ...matches];
    },
    setSearchedSkills: (state, action: PayloadAction<{ userMode: UserMode; skills: string[] }>) => {
      const { userMode, skills } = action.payload;
      state[userMode].searchedSkills = skills;
    },
    setDashboardData: (state, action: PayloadAction<{ userMode: UserMode; data: DashboardCategory[] }>) => {
      const { userMode, data } = action.payload;
      state[userMode].dashboardData = data;
    },
    setTotalCount: (state, action: PayloadAction<{ userMode: UserMode; count: number | null }>) => {
      const { userMode, count } = action.payload;
      state[userMode].totalCount = count;
    },
    setSearchStatistics: (state, action: PayloadAction<{ userMode: UserMode; totalCount: number; topSkills: SkillFrequency[] }>) => {
      const { userMode, totalCount, topSkills } = action.payload;
      state[userMode].totalCount = totalCount;
      state[userMode].topSkills = topSkills;
    },
    clearMatches: (state, action: PayloadAction<UserMode>) => {
      state[action.payload].matches = [];
      state[action.payload].totalCount = null;
      state[action.payload].topSkills = [];
    },
    resetSearch: (state, action: PayloadAction<UserMode>) => {
        state[action.payload] = { ...initialModeSpecificState, searchedSkills: [], dashboardData: state[action.payload].dashboardData };
    },
    setSkillCategories: (state, action: PayloadAction<SkillCategory[]>) => {
      state.skillCategories = action.payload;
      state.skillsLoaded = action.payload.length > 0;
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
  setMatches,
  appendMatches,
  setSearchedSkills,
  setDashboardData,
  setTotalCount,
  setSearchStatistics,
  clearMatches,
  resetSearch,
  setSkillCategories,
} = searchSlice.actions;

export default searchSlice.reducer;
