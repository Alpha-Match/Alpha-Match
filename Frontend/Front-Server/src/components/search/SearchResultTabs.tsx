// src/components/search/SearchResultTabs.tsx
'use client';

import React from 'react';
import { BarChart3, List } from 'lucide-react';

export type SearchResultTab = 'analysis' | 'list';

interface SearchResultTabsProps {
  activeTab: SearchResultTab;
  onTabChange: (tab: SearchResultTab) => void;
  activeColor: string;
}

const TABS: { id: SearchResultTab; label: string; icon: React.ElementType }[] = [
  { id: 'analysis', label: '검색 분석', icon: BarChart3 },
  { id: 'list', label: '상세 목록', icon: List },
];

export const SearchResultTabs: React.FC<SearchResultTabsProps> = ({ activeTab, onTabChange, activeColor }) => {
  return (
    <div className="flex items-center space-x-1 rounded-lg bg-panel-main p-1 mb-4">
      {TABS.map((tab) => {
        const isActive = activeTab === tab.id;
        return (
          <button
            key={tab.id}
            onClick={() => onTabChange(tab.id)}
            className={`w-full flex items-center justify-center gap-2 px-3 py-1.5 text-sm font-semibold rounded-md transition-colors duration-200
              ${isActive ? 'text-white' : 'text-text-secondary hover:bg-panel-2'}
            `}
            style={{
              backgroundColor: isActive ? activeColor : 'transparent',
            }}
          >
            <tab.icon size={16} />
            <span>{tab.label}</span>
          </button>
        );
      })}
    </div>
  );
};
