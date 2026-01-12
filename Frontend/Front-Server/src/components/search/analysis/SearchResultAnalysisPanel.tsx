// src/components/search/SearchResultAnalysisPanel.tsx
'use client';

import React from 'react';
import { useQuery } from '@apollo/client/react';
import { UserMode, SkillFrequency, SkillCategory } from '@/types';
import { TopSkills } from './TopSkills';
import { SearchResultAnalysis } from './SearchResultAnalysis';
import { GET_SEARCH_STATISTICS } from '@/services/api/queries/stats';

interface SearchResultAnalysisPanelProps {
  activeColor: string;
  userMode: UserMode;
  searchedSkills: string[];
  skillCategories: SkillCategory[];
}

interface SearchStatisticsData {
  searchStatistics: {
    topSkills: SkillFrequency[];
    totalCount: number;
  };
}

interface SearchStatisticsVars {
  mode: UserMode;
  skills: string[];
}

export const SearchResultAnalysisPanel: React.FC<SearchResultAnalysisPanelProps> = ({
  activeColor,
  userMode,
  searchedSkills = [],
  skillCategories,
}) => {
  const { data: statsData, loading: statsLoading } = useQuery<SearchStatisticsData, SearchStatisticsVars>(
    GET_SEARCH_STATISTICS,
    {
      variables: { mode: userMode, skills: searchedSkills },
      skip: searchedSkills.length === 0,
    }
  );

  const totalCount = statsData?.searchStatistics?.totalCount;
  
  if (searchedSkills.length === 0) {
      return (
          <div className="text-center p-8 text-text-tertiary h-full flex flex-col justify-center items-center">
            <h3 className="text-xl font-bold mb-4">분석할 검색어가 없습니다.</h3>
            <p>좌측 패널에서 스킬을 선택하고 검색해주세요.</p>
          </div>
        );
  }

  return (
    <div className="w-full animate-fade-in h-full overflow-y-auto custom-scrollbar pr-2 space-y-6">
      <h2 className="text-2xl font-bold text-text-primary">검색 결과 분석</h2>
      <SearchResultAnalysis
        searchedSkills={searchedSkills}
        totalCount={totalCount}
        statsLoading={statsLoading}
        activeColor={activeColor}
      />
      <div className="bg-panel-main p-6 rounded-lg shadow-lg border border-border/30">
        <TopSkills 
          mode={userMode} 
          skills={searchedSkills} 
          limit={15} 
          skillCategories={skillCategories}
        />
      </div>
    </div>
  );
};
