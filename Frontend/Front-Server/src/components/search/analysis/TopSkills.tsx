'use client';

import React, {useMemo} from 'react';
import chroma from 'chroma-js';
import {PieData, SkillCategory, SkillFrequency, UserMode} from '@/types';
import {useAppSelector} from '@/core/client/services/state/hooks';
import {CANDIDATE_THEME_COLORS, RECRUITER_THEME_COLORS} from "@/constants";
import {TwoLevelPieChart} from '@/components/charts';
import {LoadingSpinner} from '@/components/ui';

interface TopSkillsProps {
  mode: UserMode;
  skills: string[];
  skillCategories: SkillCategory[];
  topSkills?: SkillFrequency[];
  limit?: number;
  loading?: boolean; // Add loading prop
}

type ChartDataType = {
  innerData: PieData[];
  outerData: PieData[];
  categoryColorMap: Map<string, string>;
};

export const TopSkills: React.FC<TopSkillsProps> = ({ mode, skills, skillCategories, topSkills = [], limit = 15, loading = false }) => {
  const userMode = useAppSelector((state) => state.ui.userMode);
  const themeColors = userMode === UserMode.RECRUITER
    ? RECRUITER_THEME_COLORS
    : CANDIDATE_THEME_COLORS;

  const chartData = useMemo<ChartDataType>(() => {
    if (!skillCategories || !topSkills || topSkills.length === 0) {
      return { innerData: [], outerData: [], categoryColorMap: new Map() };
    }

    const skillToCategoryMap = new Map<string, string>();
    skillCategories.forEach(category => {
      category.skills.forEach(skill => {
        skillToCategoryMap.set(skill.toLowerCase().trim(), category.category);
      });
    });
    const categories = new Map<string, { value: number; skills: SkillFrequency[] }>();

    topSkills.forEach(skill => {
      const normalizedSkillName = skill.skill.toLowerCase().trim();
      const categoryName = skillToCategoryMap.get(normalizedSkillName) || '기타';
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

    const orderedOuterData: PieData[] = [];
    sortedCategories.forEach(([categoryName, categoryData]) => {
      categoryData.skills.sort((a, b) => b.count - a.count).forEach(skill => {
        orderedOuterData.push({
          name: skill.skill,
          value: skill.count,
          percentage: skill.percentage,
          category: skillToCategoryMap.get(skill.skill.toLowerCase().trim()) || '기타',
        });
      });
    });
    const outerData = orderedOuterData;
    
    const categoryColorsPalette = chroma.scale(themeColors).mode('lch').colors(Math.max(innerData.length, themeColors.length));
    const categoryColorMap = new Map(innerData.map((c, i) => [c.name, categoryColorsPalette[i % categoryColorsPalette.length]]));

    return { innerData, outerData, categoryColorMap };
  }, [topSkills, skillCategories, themeColors]);

  const { innerData, outerData, categoryColorMap } = chartData;

  // Show loading spinner if loading is true and no data to show yet
  if (loading && topSkills.length === 0) {
    return (
      <div className="flex justify-center items-center h-48">
        <LoadingSpinner size={32} message="기술 스택 분석 로딩 중..." />
      </div>
    );
  }

  if (skills.length === 0) {
    return (
      <div className="text-gray-500 dark:text-gray-400 text-center p-6 text-sm">
        분석할 기술 스택을 선택해주세요.
      </div>
    );
  }

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
