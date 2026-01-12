/**
 * @file SearchedSkillsCategoryDistributionChart.tsx
 * @description 선택한 기술 스택의 카테고리별 분포를 원 그래프로 표시
 *              GET_CATEGORY_DISTRIBUTION API 사용
 * @version 1.0.0
 * @date 2026-01-04
 */
import React from 'react';
import { useQuery } from '@apollo/client/react';
import { GET_CATEGORY_DISTRIBUTION } from '@/services/api/queries/search';
import { CategoryMatchDistribution } from '@/types';
import { LoadingSpinner } from '@/components/ui/LoadingSpinner';

interface CategoryPieChartProps {
  skills: string[];
  activeColor: string;
}

interface CategoryDistributionData {
  getCategoryDistribution: CategoryMatchDistribution[];
}

interface CategoryDistributionVars {
  skills: string[];
}

// 카테고리별 색상 매핑
const CATEGORY_COLORS: Record<string, string> = {
  'Backend': '#3B82F6',        // blue-500
  'Frontend': '#10B981',       // green-500
  'Database': '#8B5CF6',       // purple-500
  'DevOps / Cloud': '#F59E0B', // amber-500
  'Machine Learning': '#EF4444', // red-500
  'Mobile': '#06B6D4',         // cyan-500
  'Game': '#EC4899',           // pink-500
  'Blockchain': '#6366F1',     // indigo-500
  'Collaboration / Project Management': '#84CC16', // lime-500
  'Others': '#6B7280',         // gray-500
};

const SearchedSkillsCategoryDistributionChart: React.FC<CategoryPieChartProps> = ({ skills, activeColor }) => {
  const { data, loading, error } = useQuery<CategoryDistributionData, CategoryDistributionVars>(
    GET_CATEGORY_DISTRIBUTION,
    {
      variables: { skills },
      skip: !skills || skills.length === 0,
    }
  );

  if (!skills || skills.length === 0) {
    return null;
  }

  if (loading) {
    return (
      <div className="bg-panel-main p-4 rounded-lg border border-border/30 mb-6">
        <h3 className="text-sm font-semibold mb-3 text-text-secondary">검색 스킬 카테고리 분포</h3>
        <div className="flex justify-center py-4">
          <LoadingSpinner size={24} color={activeColor} />
        </div>
      </div>
    );
  }

  if (error || !data?.getCategoryDistribution || data.getCategoryDistribution.length === 0) {
    return null;
  }

  const distributions = data.getCategoryDistribution;

  // SVG 원 그래프를 위한 계산
  const size = 120;
  const strokeWidth = 20;
  const radius = (size - strokeWidth) / 2;
  const circumference = 2 * Math.PI * radius;

  let currentOffset = 0;

  return (
    <div className="bg-panel-main p-4 rounded-lg border border-border/30 mb-6">
      <h3 className="text-sm font-semibold mb-3 text-text-secondary">검색 스킬 카테고리 분포</h3>

      <div className="flex items-center gap-6">
        {/* SVG 원 그래프 */}
        <div className="relative" style={{ width: size, height: size }}>
          <svg width={size} height={size} className="transform -rotate-90">
            {distributions.map((dist, index) => {
              const percentage = dist.percentage / 100;
              const strokeDasharray = circumference * percentage;
              const strokeDashoffset = -currentOffset;
              currentOffset += strokeDasharray;

              const color = CATEGORY_COLORS[dist.category] || CATEGORY_COLORS['Others'];

              return (
                <circle
                  key={index}
                  cx={size / 2}
                  cy={size / 2}
                  r={radius}
                  fill="transparent"
                  stroke={color}
                  strokeWidth={strokeWidth}
                  strokeDasharray={`${strokeDasharray} ${circumference - strokeDasharray}`}
                  strokeDashoffset={strokeDashoffset}
                  className="transition-all duration-300"
                />
              );
            })}
          </svg>
          {/* 중앙 텍스트 */}
          <div className="absolute inset-0 flex items-center justify-center">
            <div className="text-center">
              <div className="text-2xl font-bold text-text-primary">{distributions.length}</div>
              <div className="text-xs text-text-tertiary">카테고리</div>
            </div>
          </div>
        </div>

        {/* 범례 */}
        <div className="flex-1 space-y-1.5">
          {distributions.map((dist, index) => {
            const color = CATEGORY_COLORS[dist.category] || CATEGORY_COLORS['Others'];

            return (
              <div key={index} className="flex items-center justify-between text-sm">
                <div className="flex items-center gap-2 flex-1 min-w-0">
                  <div
                    className="w-3 h-3 rounded-full flex-shrink-0"
                    style={{ backgroundColor: color }}
                  />
                  <span className="text-text-secondary truncate" title={dist.category}>
                    {dist.category}
                  </span>
                </div>
                <div className="flex items-center gap-2 ml-2">
                  <span className="text-text-tertiary text-xs">
                    ({dist.skillCount}개)
                  </span>
                  <span className="text-text-primary font-medium min-w-[45px] text-right">
                    {dist.percentage.toFixed(1)}%
                  </span>
                </div>
              </div>
            );
          })}
        </div>
      </div>

      {/* 매칭된 스킬 상세 (작은 글씨로) */}
      <div className="mt-3 pt-3 border-t border-border/20">
        <div className="text-xs text-text-tertiary">
          {distributions.map((dist, index) => (
            <span key={index}>
              <span className="font-medium" style={{ color: CATEGORY_COLORS[dist.category] || CATEGORY_COLORS['Others'] }}>
                {dist.category}
              </span>
              : {dist.matchedSkills.join(', ')}
              {index < distributions.length - 1 && ' | '}
            </span>
          ))}
        </div>
      </div>
    </div>
  );
};

export default SearchedSkillsCategoryDistributionChart;
