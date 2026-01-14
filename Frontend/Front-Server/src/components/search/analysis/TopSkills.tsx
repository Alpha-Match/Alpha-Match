'use client';

import React, {useMemo} from 'react';
import {useQuery} from '@apollo/client/react';
import chroma from 'chroma-js';
import {GET_SEARCH_STATISTICS} from '@/core/client/services/api/queries/stats';
import {PieData, SkillCategory, SkillFrequency, UserMode} from '@/types';
import {LoadingSpinner} from '@/components/ui/LoadingSpinner';
import {useAppSelector} from '@/core/client/services/state/hooks';
import {CANDIDATE_THEME_COLORS, RECRUITER_THEME_COLORS} from "@/constants";
import {TwoLevelPieChart} from '@/components/charts';

interface TopSkillsProps {
  mode: UserMode;
  skills: string[];
  skillCategories: SkillCategory[]; // Prop for server-side fetched categories
  limit?: number;
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
  limit?: number;
}

export const TopSkills: React.FC<TopSkillsProps> = ({ mode, skills, skillCategories, limit = 15 }) => {
  const { data, loading, error } = useQuery<SearchStatisticsData, SearchStatisticsVars>(
    GET_SEARCH_STATISTICS,
    {
      variables: { mode, skills, limit },
      skip: skills.length === 0,
      fetchPolicy: 'cache-first', // Use cached data first
    }
  );

  const userMode = useAppSelector((state) => state.ui.userMode);
  const themeColors = userMode === UserMode.RECRUITER
    ? RECRUITER_THEME_COLORS
    : CANDIDATE_THEME_COLORS;

  const chartData = useMemo(() => {
    if (!skillCategories || !data?.searchStatistics?.topSkills) {
      return { innerData: [], outerData: [], categoryColorMap: new Map() };
    }
    
    const skillToCategoryMap = new Map<string, string>();
    skillCategories.forEach(category => {
      category.skills.forEach(skill => {
        skillToCategoryMap.set(skill.toLowerCase().trim(), category.category); // Normalize key
      });
    });

    const topSkills = data.searchStatistics.topSkills;
    const categories = new Map<string, { value: number; skills: SkillFrequency[] }>();

    topSkills.forEach(skill => {
      const normalizedSkillName = skill.skill.toLowerCase().trim(); // Normalize lookup key
      const categoryName = skillToCategoryMap.get(normalizedSkillName) || '기타'; // Use normalized key
      if (!categories.has(categoryName)) {
        categories.set(categoryName, { value: 0, skills: [] });
      }
      const categoryData = categories.get(categoryName)!;
      categoryData.value += skill.count;
      categoryData.skills.push(skill);
    });

    const sortedCategories = Array.from(categories.entries()).sort((a, b) => b[1].value - a[1].value);

    const innerData = sortedCategories.map(([name, data]) => ({
      name,
      value: data.value,
    }));

    // Build outerData in a hierarchical order to match innerData
    const orderedOuterData: PieData[] = [];
    sortedCategories.forEach(([categoryName, categoryData]) => {
      // Sort skills within each category by value for consistent outer ring order
      categoryData.skills.sort((a, b) => b.count - a.count).forEach(skill => {
        orderedOuterData.push({
          name: skill.skill,
          value: skill.count,
          percentage: skill.percentage,
          category: skillToCategoryMap.get(skill.skill.toLowerCase().trim()) || '기타',
        });
      });
    });
    const outerData = orderedOuterData; // Assign the newly ordered data
    
    const categoryColorsPalette = chroma.scale(themeColors).mode('lch').colors(Math.max(innerData.length, themeColors.length));
    const categoryColorMap = new Map(innerData.map((c, i) => [c.name, categoryColorsPalette[i % categoryColorsPalette.length]]));

    return { innerData, outerData, categoryColorMap };
  }, [data, skillCategories, themeColors]);

  const { innerData, outerData, categoryColorMap } = chartData;

  if (skills.length === 0) {
    return (
      <div className="text-gray-500 dark:text-gray-400 text-center p-6 text-sm">
        분석할 기술 스택을 선택해주세요.
      </div>
    );
  }

  if (loading) {
    return (
      <div className="flex justify-center items-center h-full min-h-[450px]">
        <LoadingSpinner />
      </div>
    );
  }

  if (error) {
    return (
      <div className="text-red-500 p-4 rounded-lg border border-red-300 bg-red-50 dark:bg-red-900/20">
        <p className="font-semibold text-sm">요구 기술 통계 로딩 중 오류</p>
        <p className="text-xs mt-1">{error.message}</p>
      </div>
    );
  }

  const topSkills = data?.searchStatistics?.topSkills || [];
  if (topSkills.length === 0) {
    return (
      <div className="text-gray-500 dark:text-gray-400 text-center p-6 text-sm">
        일치하는 검색 결과가 없습니다.
      </div>
    );
  }

    const title = mode === UserMode.CANDIDATE
        ? `채용 시장 수요 기술 Top ${limit}`
        : `인재 역량 현황 Top ${limit}`;

  return (
    <div>
      <h3 className="text-base font-semibold text-text-primary mb-3">
        {title}
      </h3>
      <TwoLevelPieChart 
        innerData={innerData}
        outerData={outerData}
        categoryColorMap={categoryColorMap}
      />
      <div className="mt-4 pt-3 border-t border-border text-xs text-text-secondary">
        <p>
          검색 결과에 대한 상위 {topSkills.length}개 기술의 카테고리별 분포입니다.
        </p>
      </div>
    </div>
  );
};
