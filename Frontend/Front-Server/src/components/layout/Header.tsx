// src/components/layout/Header.tsx
'use client';

import React from 'react';
import { useAppSelector, useAppDispatch } from '../../services/state/hooks';
import { resetSearch } from '../../services/state/features/search/searchSlice';
import { setUserMode, resetView } from '../../services/state/features/ui/uiSlice';
import { UserMode } from '../../types';
import { Briefcase, UserSearch } from 'lucide-react';
import { ThemeToggle } from '../common/ThemeToggle';
import { CANDIDATE_THEME_COLORS, RECRUITER_THEME_COLORS } from '../../constants';

export const Header: React.FC = () => {
  const dispatch = useAppDispatch();
  const userMode = useAppSelector((state) => state.ui.userMode);
  const theme = useAppSelector((state) => state.ui.theme);

  const handleTabChange = (mode: UserMode) => {
    dispatch(setUserMode(mode));
    dispatch(resetSearch());
    dispatch(resetView());
  };

  const handleNavigateHome = () => {
    dispatch(resetView());
  };
  
  const themeColors = userMode === UserMode.CANDIDATE ? CANDIDATE_THEME_COLORS : RECRUITER_THEME_COLORS;
  const activeColor = theme === 'dark' ? themeColors[1] : themeColors[0];
  
  // The logo color is now also based on the candidate theme color for brand consistency.
  const logoColor = CANDIDATE_THEME_COLORS[0];

  return (
    <header className="bg-white dark:bg-slate-800 border-b border-slate-200 dark:border-slate-700 z-50 flex-none h-16 flex items-center justify-between px-6">
      <button onClick={handleNavigateHome} className="flex items-center gap-2 focus:outline-none focus:ring-2" style={{'--ring-color': logoColor} as React.CSSProperties}>
        <div className="w-8 h-8 rounded-lg flex items-center justify-center" style={{backgroundColor: logoColor}}>
          <svg className="w-5 h-5 text-white" fill="none" viewBox="0 0 24 24" stroke="currentColor">
            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2.5} d="M13 10V3L4 14h7v7l9-11h-7z" />
          </svg>
        </div>
        <h1 className="text-gray-800 dark:text-slate-100 text-xl font-black tracking-tight">
          Alpha<span style={{color: logoColor}}>Match</span>
        </h1>
      </button>

      <div className="flex bg-slate-100 dark:bg-slate-700 p-1 rounded-xl">
        <button
          onClick={() => handleTabChange(UserMode.CANDIDATE)}
          className={`flex items-center gap-2 px-6 py-2 rounded-lg text-sm font-bold transition-all ${
            userMode === UserMode.CANDIDATE 
              ? 'bg-white dark:bg-slate-800 shadow-md' 
              : 'text-gray-600 dark:text-slate-400 hover:bg-slate-200 dark:hover:bg-slate-600 dark:hover:text-slate-200'
          }`}
          style={userMode === UserMode.CANDIDATE ? { color: CANDIDATE_THEME_COLORS[0] } : {}}
        >
          <Briefcase className="w-4 h-4" />
          Job Seeker View
        </button>
        <button
          onClick={() => handleTabChange(UserMode.RECRUITER)}
          className={`flex items-center gap-2 px-6 py-2 rounded-lg text-sm font-bold transition-all ${
            userMode === UserMode.RECRUITER 
              ? 'bg-white dark:bg-slate-800 shadow-md' 
              : 'text-gray-600 dark:text-slate-400 hover:bg-slate-200 dark:hover:bg-slate-600 dark:hover:text-slate-200'
          }`}
          style={userMode === UserMode.RECRUITER ? { color: RECRUITER_THEME_COLORS[0] } : {}}
        >
          <UserSearch className="w-4 h-4" />
          Recruiter View
        </button>
      </div>
      <div className="w-32 flex justify-end">
        <ThemeToggle />
      </div>
    </header>
  );
};
