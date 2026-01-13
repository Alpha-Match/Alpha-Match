// src/components/layout/TabController.tsx
'use client';

import React from 'react';
import {BarChart3, FileText, LayoutDashboard, Settings} from 'lucide-react';
import {UserMode} from '@/types';
import {useAppSelector} from '@/core/client/services/state/hooks'; // useAppSelector 임포트
import {CANDIDATE_THEME_COLORS, RECRUITER_THEME_COLORS} from '@/constants'; // 테마 색상 임포트

type PageViewMode = 'dashboard' | 'input' | 'results' | 'detail' | 'analysis';

interface TabControllerProps {
  activeView: PageViewMode;
  onTabChange: (view: PageViewMode) => void;
  userMode: UserMode;
  detailAvailable: boolean;
}

const TABS: { id: PageViewMode; label: string; icon: React.ElementType }[] = [
  { id: 'dashboard', label: '대시보드', icon: LayoutDashboard },
  { id: 'input', label: '검색 조건', icon: Settings },
  { id: 'results', label: '검색 결과', icon: BarChart3 },
  { id: 'analysis', label: '분석', icon: BarChart3 }, // Use BarChart3 for analysis
  { id: 'detail', label: '상세 정보', icon: FileText },
];

export const TabController: React.FC<TabControllerProps> = ({ activeView, onTabChange, userMode, detailAvailable }) => {
  const theme = useAppSelector((state) => state.ui.theme); // useAppSelector를 통해 theme 상태 가져옴
  
  const activeThemeColors = userMode === UserMode.CANDIDATE ? CANDIDATE_THEME_COLORS : RECRUITER_THEME_COLORS;
  const activeColor = theme === 'dark' ? activeThemeColors[1] : activeThemeColors[0];
  
  return (
    <div className="flex items-center justify-center bg-panel-sidebar border-b border-border mb-4">
      <div className="flex space-x-2 p-1 bg-panel-main rounded-lg">
        {TABS.map((tab) => {
          const isActive = activeView === tab.id;
          const isDisabled = tab.id === 'detail' && !detailAvailable;
          
          return (
            <button
              key={tab.id}
              onClick={() => onTabChange(tab.id)}
              disabled={isDisabled}
              className={`flex items-center gap-2 px-4 py-2 text-sm font-medium rounded-md transition-colors duration-200
                ${
                  isActive
                    ? 'text-white'
                    : 'text-text-secondary hover:bg-panel-2 hover:text-text-primary'
                }
                ${isDisabled ? 'opacity-50 cursor-not-allowed' : ''}
              `}
              style={{
                backgroundColor: isActive ? activeColor : 'var(--color-panel-2)',
              }}
            >
              <tab.icon size={16} />
              <span>{tab.label}</span>
            </button>
          );
        })}
      </div>
    </div>
  );
};
