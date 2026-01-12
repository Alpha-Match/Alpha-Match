// src/components/layout/Header.tsx
'use client';

import React from 'react';
import { useAppSelector, useAppDispatch } from '../../services/state/hooks';
import { resetSearch } from '../../services/state/features/search/searchSlice';
import { setUserMode, resetView } from '../../services/state/features/ui/uiSlice';
import { UserMode } from '../../types';
import { Briefcase, UserSearch, Home } from 'lucide-react';
import { ThemeToggle } from '../common/ThemeToggle';
import { CANDIDATE_THEME_COLORS, RECRUITER_THEME_COLORS } from '../../constants';

interface HeaderProps {
  showDashboardButton?: boolean;
  onNavigateToDashboard?: () => void;
}

export const Header: React.FC<HeaderProps> = ({ showDashboardButton, onNavigateToDashboard }) => {
  const dispatch = useAppDispatch();
  const userMode = useAppSelector((state) => state.ui.userMode);
  const theme = useAppSelector((state) => state.ui.theme);

  const handleTabChange = (mode: UserMode) => {
    dispatch(setUserMode(mode));
  };

  const handleNavigateHome = () => {
    dispatch(resetView(userMode));
  };
  
  const activeThemeColors = userMode === UserMode.CANDIDATE ? CANDIDATE_THEME_COLORS : RECRUITER_THEME_COLORS;
  const activeColor = theme === 'dark' ? activeThemeColors[1] : activeThemeColors[0];
  
  // 로고 색상을 현재 활성 사용자 모드의 테마 색상과 연동
  const logoColor = activeThemeColors[0];

  return (
    <header className="bg-panel-1 border-b border-border z-50 flex items-center justify-between px-6 flex-wrap min-h-16">
      <button onClick={handleNavigateHome} className="flex items-center gap-2 focus:outline-none focus:ring-2" style={{'--ring-color': logoColor} as React.CSSProperties}>
        <div className="w-8 h-8 rounded-lg flex items-center justify-center" style={{backgroundColor: logoColor}}>
          <svg className="w-5 h-5 text-white" fill="none" viewBox="0 0 24 24" stroke="currentColor">
            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2.5} d="M13 10V3L4 14h7v7l9-11h-7z" />
          </svg>
        </div>
        <h1 className="text-text-primary text-xl font-black tracking-tight">
          Alpha<span style={{color: logoColor}}>Match</span>
        </h1>
      </button>

      <div className="flex items-center gap-4">
        {showDashboardButton && (
          <button
            onClick={onNavigateToDashboard}
            className="flex items-center gap-2 px-4 py-2 rounded-lg text-sm font-bold transition-all bg-panel-2 text-text-secondary hover:bg-panel-main hover:text-primary"
          >
            <Home className="w-4 h-4" />
            대시보드
          </button>
        )}
        <div className="flex flex-wrap bg-background p-1 rounded-xl">
          <button
            onClick={() => handleTabChange(UserMode.CANDIDATE)}
            className={`flex flex-shrink-0 items-center gap-2 sm:px-6 px-3 py-2 rounded-lg text-sm font-bold transition-all ${
              userMode === UserMode.CANDIDATE 
                ? 'bg-panel-1 shadow-md' 
                : 'text-text-secondary hover:bg-panel-2 hover:text-text-primary'
            }`}
            style={userMode === UserMode.CANDIDATE ? { color: logoColor } : {}}
          >
            <Briefcase className="w-4 h-4" />
            구직자 모드
          </button>
          <button
            onClick={() => handleTabChange(UserMode.RECRUITER)}
            className={`flex flex-shrink-0 items-center gap-2 sm:px-6 px-3 py-2 rounded-lg text-sm font-bold transition-all ${
              userMode === UserMode.RECRUITER 
                ? 'bg-panel-1 shadow-md' 
                : 'text-text-secondary hover:bg-panel-2 hover:text-text-primary'
            }`}
            style={userMode === UserMode.RECRUITER ? { color: RECRUITER_THEME_COLORS[0] } : {}}
          >
            <UserSearch className="w-4 h-4" />
            리크루터 모드
          </button>
        </div>
      </div>
      <div className="w-auto flex justify-end flex-shrink-0">
        <ThemeToggle />
      </div>
    </header>
  );
};
