'use client';

import React, {useEffect, useState} from 'react';
import {useQuery} from '@apollo/client/react';
import {
    Legend,
    PolarAngleAxis,
    PolarGrid,
    PolarRadiusAxis,
    Radar,
    RadarChart,
    ResponsiveContainer,
    Tooltip
} from 'recharts';
import {GET_SKILL_COMPETENCY_MATCH} from '@/services/api/queries/search';
import {SkillCompetencyMatch, UserMode} from '@/types';
import {LoadingSpinner} from '@/components/ui';
import {useAppSelector} from '@/services/state/hooks';

interface SkillRadarChartProps {
  mode: UserMode;
  targetId: string;
  searchedSkills: string[];
}

interface SkillCompetencyData {
  getSkillCompetencyMatch: SkillCompetencyMatch;
}

interface SkillCompetencyVars {
  mode: UserMode;
  targetId: string;
  searchedSkills: string[];
}

interface RadarDataPoint {
  category: string;
  searched: number;    // Number of searched skills in this category
  target: number;      // Number of target skills in this category
  matched: number;     // Number of matched skills in this category
}

/**
 * SkillComparisonRadarChart Component
 * - Displays radar chart comparing searched skills vs target skills by category
 * - Maps to dashboard_request.txt #3: "Specified Domain Radar Chart를 활용하여 선택한 기술 스택 vs 공고/이력서의 기술스택 비교 분석"
 * - Used in MatchDetailPanel (detail page)
 */
export const SkillComparisonRadarChart: React.FC<SkillRadarChartProps> = ({ mode, targetId, searchedSkills }) => {
  const theme = useAppSelector((state) => state.ui.theme);
  const [radarData, setRadarData] = useState<RadarDataPoint[]>([]);

  const { data, loading, error } = useQuery<SkillCompetencyData, SkillCompetencyVars>(
    GET_SKILL_COMPETENCY_MATCH,
    {
      variables: { mode, targetId, searchedSkills },
      skip: !targetId || searchedSkills.length === 0,
    }
  );

  // Get skill categories from Redux state (loaded by AppInitializer)
  const skillCategories = useAppSelector((state) => state.search.skillCategories);

  useEffect(() => {
    if (!data?.getSkillCompetencyMatch || skillCategories.length === 0) return;

    const competency = data.getSkillCompetencyMatch;

    // Create a map of skill -> category
    const skillToCategoryMap = new Map<string, string>();
    skillCategories.forEach((cat:{category:string, skills:string[]}) => {
      cat.skills.forEach((skill:string) => {
        skillToCategoryMap.set(skill.toLowerCase(), cat.category);
      });
    });

    // Group skills by category
    const categoryData = new Map<string, { searched: number; target: number; matched: number }>();

    // Count searched skills by category
    searchedSkills.forEach((skill) => {
      const category = skillToCategoryMap.get(skill.toLowerCase()) || 'Other';
      if (!categoryData.has(category)) {
        categoryData.set(category, { searched: 0, target: 0, matched: 0 });
      }
      categoryData.get(category)!.searched++;
    });

    // Count target skills by category (matched + missing)
    const allTargetSkills = [...competency.matchedSkills, ...competency.missingSkills];
    allTargetSkills.forEach((skill) => {
      const category = skillToCategoryMap.get(skill.toLowerCase()) || 'Other';
      if (!categoryData.has(category)) {
        categoryData.set(category, { searched: 0, target: 0, matched: 0 });
      }
      categoryData.get(category)!.target++;
    });

    // Count matched skills by category
    competency.matchedSkills.forEach((skill) => {
      const category = skillToCategoryMap.get(skill.toLowerCase()) || 'Other';
      if (!categoryData.has(category)) {
        categoryData.set(category, { searched: 0, target: 0, matched: 0 });
      }
      categoryData.get(category)!.matched++;
    });

    // Convert to array for radar chart (only categories with data)
    const radarPoints: RadarDataPoint[] = Array.from(categoryData.entries())
      .filter(([_, counts]) => counts.searched > 0 || counts.target > 0)
      .map(([category, counts]) => ({
        category,
        searched: counts.searched,
        target: counts.target,
        matched: counts.matched,
      }))
      .sort((a, b) => a.category.localeCompare(b.category));

    setRadarData(radarPoints);
  }, [data, skillCategories, searchedSkills]);

  if (searchedSkills.length === 0 || !targetId) {
    return (
      <div className="text-gray-500 dark:text-gray-400 text-center p-6 text-sm">
        Select a result to see skill comparison
      </div>
    );
  }

  if (loading) {
    return (
      <div className="flex justify-center items-center h-64">
        <LoadingSpinner />
      </div>
    );
  }

  if (error) {
    return (
      <div className="text-red-500 p-4 rounded-lg border border-red-300 bg-red-50 dark:bg-red-900/20">
        <p className="font-semibold text-sm">Error loading skill comparison</p>
        <p className="text-xs mt-1">{error.message}</p>
      </div>
    );
  }

  const competency = data?.getSkillCompetencyMatch;

  if (!competency) {
    return (
      <div className="text-gray-500 dark:text-gray-400 text-center p-6 text-sm">
        No skill data available
      </div>
    );
  }

  // Determine colors based on theme
  const searchedColor = theme === 'dark' ? '#60a5fa' : '#3b82f6'; // Blue
  const targetColor = theme === 'dark' ? '#f87171' : '#ef4444';   // Red
  const matchedColor = theme === 'dark' ? '#34d399' : '#10b981';  // Green
  const textColor = theme === 'dark' ? '#e5e7eb' : '#374151';

  return (
    <div className="space-y-4">
      {/* Competency Summary */}
      <div className="grid grid-cols-3 gap-3">
        <div className="p-3 rounded-lg bg-green-50 dark:bg-green-900/20 border border-green-200 dark:border-green-800">
          <div className="text-xs text-green-700 dark:text-green-300 mb-1">Matched Skills</div>
          <div className="text-lg font-bold text-green-900 dark:text-green-100">
            {competency.matchedSkills.length}
          </div>
        </div>
        <div className="p-3 rounded-lg bg-red-50 dark:bg-red-900/20 border border-red-200 dark:border-red-800">
          <div className="text-xs text-red-700 dark:text-red-300 mb-1">Missing Skills</div>
          <div className="text-lg font-bold text-red-900 dark:text-red-100">
            {competency.missingSkills.length}
          </div>
        </div>
        <div className="p-3 rounded-lg bg-blue-50 dark:bg-blue-900/20 border border-blue-200 dark:border-blue-800">
          <div className="text-xs text-blue-700 dark:text-blue-300 mb-1">Extra Skills</div>
          <div className="text-lg font-bold text-blue-900 dark:text-blue-100">
            {competency.extraSkills.length}
          </div>
        </div>
      </div>

      {/* Competency Level Badge */}
      <div className="flex items-center justify-center gap-2">
        <span className="text-sm text-text-secondary">Competency Level:</span>
        <span
          className={`px-3 py-1 rounded-full text-sm font-semibold ${
            competency.competencyLevel === 'High'
              ? 'bg-green-100 text-green-800 dark:bg-green-900 dark:text-green-200'
              : competency.competencyLevel === 'Medium'
              ? 'bg-yellow-100 text-yellow-800 dark:bg-yellow-900 dark:text-yellow-200'
              : 'bg-red-100 text-red-800 dark:bg-red-900 dark:text-red-200'
          }`}
        >
          {competency.competencyLevel} ({competency.matchingPercentage.toFixed(1)}%)
        </span>
      </div>

      {/* Radar Chart */}
      {radarData.length > 0 ? (
        <div className="h-[400px]">
          <ResponsiveContainer width="100%" height="100%">
            <RadarChart data={radarData}>
              <PolarGrid stroke={theme === 'dark' ? '#4b5563' : '#d1d5db'} />
              <PolarAngleAxis
                dataKey="category"
                tick={{ fill: textColor, fontSize: 12 }}
              />
              <PolarRadiusAxis
                angle={90}
                domain={[0, 'auto']}
                tick={{ fill: textColor, fontSize: 10 }}
              />
              <Radar
                name="Your Skills"
                dataKey="searched"
                stroke={searchedColor}
                fill={searchedColor}
                fillOpacity={0.3}
              />
              <Radar
                name="Required Skills"
                dataKey="target"
                stroke={targetColor}
                fill={targetColor}
                fillOpacity={0.3}
              />
              <Radar
                name="Matched"
                dataKey="matched"
                stroke={matchedColor}
                fill={matchedColor}
                fillOpacity={0.5}
              />
              <Tooltip
                contentStyle={{
                  backgroundColor: theme === 'dark' ? '#1f2937' : '#ffffff',
                  border: `1px solid ${theme === 'dark' ? '#374151' : '#e5e7eb'}`,
                  borderRadius: '8px',
                  color: textColor,
                }}
              />
              <Legend
                wrapperStyle={{ fontSize: '12px', color: textColor }}
              />
            </RadarChart>
          </ResponsiveContainer>
        </div>
      ) : (
        <div className="text-gray-500 dark:text-gray-400 text-center p-6 text-sm">
          No category data available for radar chart
        </div>
      )}

      {/* Skill Lists */}
      <div className="grid grid-cols-1 md:grid-cols-3 gap-4 mt-4">
        {/* Matched Skills */}
        <div className="p-3 rounded-lg bg-background-secondary border border-border">
          <h4 className="text-xs font-semibold text-green-600 dark:text-green-400 mb-2">
            ✓ Matched ({competency.matchedSkills.length})
          </h4>
          <div className="flex flex-wrap gap-1">
            {competency.matchedSkills.map((skill) => (
              <span
                key={skill}
                className="px-2 py-1 text-xs bg-green-50 dark:bg-green-900/30 text-green-700 dark:text-green-300 rounded"
              >
                {skill}
              </span>
            ))}
          </div>
        </div>

        {/* Missing Skills */}
        <div className="p-3 rounded-lg bg-background-secondary border border-border">
          <h4 className="text-xs font-semibold text-red-600 dark:text-red-400 mb-2">
            ✗ Missing ({competency.missingSkills.length})
          </h4>
          <div className="flex flex-wrap gap-1">
            {competency.missingSkills.map((skill) => (
              <span
                key={skill}
                className="px-2 py-1 text-xs bg-red-50 dark:bg-red-900/30 text-red-700 dark:text-red-300 rounded"
              >
                {skill}
              </span>
            ))}
          </div>
        </div>

        {/* Extra Skills */}
        <div className="p-3 rounded-lg bg-background-secondary border border-border">
          <h4 className="text-xs font-semibold text-blue-600 dark:text-blue-400 mb-2">
            + Extra ({competency.extraSkills.length})
          </h4>
          <div className="flex flex-wrap gap-1">
            {competency.extraSkills.map((skill) => (
              <span
                key={skill}
                className="px-2 py-1 text-xs bg-blue-50 dark:bg-blue-900/30 text-blue-700 dark:text-blue-300 rounded"
              >
                {skill}
              </span>
            ))}
          </div>
        </div>
      </div>
    </div>
  );
};
