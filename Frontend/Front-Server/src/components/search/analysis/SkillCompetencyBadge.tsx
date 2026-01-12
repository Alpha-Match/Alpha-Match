/**
 * @file SkillCompetencyBadge.tsx
 * @description 검색한 기술 스택 vs 대상(공고/이력서) 기술 스택 역량 매칭도 표시
 *              GET_SKILL_COMPETENCY_MATCH API 사용
 * @version 1.0.0
 * @date 2026-01-04
 */
import React from 'react';
import { useQuery } from '@apollo/client/react';
import { GET_SKILL_COMPETENCY_MATCH } from '../../../services/api/queries/search';
import { SkillCompetencyMatch, UserMode } from '../../../types';
import { LoadingSpinner } from '@/components/ui';
import { CheckCircle2, AlertCircle, XCircle, TrendingUp } from 'lucide-react';

interface SkillCompetencyBadgeProps {
  mode: UserMode;
  targetId: string;
  searchedSkills: string[];
  activeColor: string;
}

interface SkillCompetencyMatchData {
  getSkillCompetencyMatch: SkillCompetencyMatch;
}

interface SkillCompetencyMatchVars {
  mode: UserMode;
  targetId: string;
  searchedSkills: string[];
}

export const SkillCompetencyBadge: React.FC<SkillCompetencyBadgeProps> = ({
  mode,
  targetId,
  searchedSkills,
  activeColor,
}) => {
  const { data, loading, error } = useQuery<SkillCompetencyMatchData, SkillCompetencyMatchVars>(
    GET_SKILL_COMPETENCY_MATCH,
    {
      variables: { mode, targetId, searchedSkills },
      skip: !targetId || !searchedSkills || searchedSkills.length === 0,
    }
  );

  if (!targetId || !searchedSkills || searchedSkills.length === 0) {
    return null;
  }

  if (loading) {
    return (
      <div className="bg-panel-main p-6 rounded-lg border border-border/30 mb-6">
        <h3 className="text-lg font-semibold mb-4 text-text-primary">역량 매칭도</h3>
        <div className="flex justify-center py-4">
          <LoadingSpinner size={32} color={activeColor} />
        </div>
      </div>
    );
  }

  if (error || !data?.getSkillCompetencyMatch) {
    return null;
  }

  const match = data.getSkillCompetencyMatch;

  // 역량 레벨에 따른 색상 및 아이콘
  const getLevelConfig = (level: string) => {
    switch (level) {
      case 'High':
        return {
          color: '#10B981', // green-500
          bgColor: 'bg-green-500/10',
          borderColor: 'border-green-500/30',
          icon: CheckCircle2,
          label: '우수',
        };
      case 'Medium':
        return {
          color: '#F59E0B', // amber-500
          bgColor: 'bg-amber-500/10',
          borderColor: 'border-amber-500/30',
          icon: AlertCircle,
          label: '보통',
        };
      case 'Low':
        return {
          color: '#EF4444', // red-500
          bgColor: 'bg-red-500/10',
          borderColor: 'border-red-500/30',
          icon: XCircle,
          label: '부족',
        };
      default:
        return {
          color: '#6B7280', // gray-500
          bgColor: 'bg-gray-500/10',
          borderColor: 'border-gray-500/30',
          icon: TrendingUp,
          label: '평가 중',
        };
    }
  };

  const levelConfig = getLevelConfig(match.competencyLevel);
  const LevelIcon = levelConfig.icon;

  return (
    <div className="bg-panel-main p-6 rounded-lg border border-border/30 mb-6">
      <h3 className="text-lg font-semibold mb-4 text-text-primary flex items-center gap-2">
        <TrendingUp size={20} className="text-primary" />
        역량 매칭도 분석
      </h3>

      {/* 매칭 비율 및 레벨 */}
      <div className={`p-4 rounded-lg border ${levelConfig.bgColor} ${levelConfig.borderColor} mb-4`}>
        <div className="flex items-center justify-between">
          <div className="flex items-center gap-3">
            <LevelIcon size={32} style={{ color: levelConfig.color }} />
            <div>
              <div className="text-2xl font-bold" style={{ color: levelConfig.color }}>
                {match.matchingPercentage.toFixed(1)}%
              </div>
              <div className="text-sm text-text-tertiary">
                매칭 역량: <span className="font-medium" style={{ color: levelConfig.color }}>{levelConfig.label}</span>
              </div>
            </div>
          </div>
          <div className="text-right text-sm text-text-tertiary">
            <div>총 요구 스킬: {match.totalTargetSkills}개</div>
            <div>검색 스킬: {match.totalSearchedSkills}개</div>
          </div>
        </div>
      </div>

      {/* 매칭 스킬 (교집합) */}
      {match.matchedSkills.length > 0 && (
        <div className="mb-4">
          <h4 className="text-sm font-medium text-text-secondary mb-2 flex items-center gap-2">
            <CheckCircle2 size={16} className="text-green-500" />
            보유 스킬 ({match.matchedSkills.length}개)
          </h4>
          <div className="flex flex-wrap gap-2">
            {match.matchedSkills.map((skill, index) => (
              <span
                key={index}
                className="px-3 py-1.5 bg-green-500/10 text-green-400 rounded-full text-sm font-medium border border-green-500/30"
              >
                {skill}
              </span>
            ))}
          </div>
        </div>
      )}

      {/* 부족한 스킬 (target에만 있음) */}
      {match.missingSkills.length > 0 && (
        <div className="mb-4">
          <h4 className="text-sm font-medium text-text-secondary mb-2 flex items-center gap-2">
            <AlertCircle size={16} className="text-amber-500" />
            부족한 스킬 ({match.missingSkills.length}개)
          </h4>
          <div className="flex flex-wrap gap-2">
            {match.missingSkills.map((skill, index) => (
              <span
                key={index}
                className="px-3 py-1.5 bg-amber-500/10 text-amber-400 rounded-full text-sm font-medium border border-amber-500/30"
              >
                {skill}
              </span>
            ))}
          </div>
          <p className="text-xs text-text-tertiary mt-2">
            {mode === UserMode.CANDIDATE
              ? '이 공고에서 요구하지만 검색하지 않은 기술입니다.'
              : '이 후보자가 보유했지만 검색하지 않은 기술입니다.'}
          </p>
        </div>
      )}

      {/* 추가 스킬 (searched에만 있음) */}
      {match.extraSkills.length > 0 && (
        <div>
          <h4 className="text-sm font-medium text-text-secondary mb-2 flex items-center gap-2">
            <TrendingUp size={16} className="text-blue-500" />
            추가 스킬 ({match.extraSkills.length}개)
          </h4>
          <div className="flex flex-wrap gap-2">
            {match.extraSkills.map((skill, index) => (
              <span
                key={index}
                className="px-3 py-1.5 bg-blue-500/10 text-blue-400 rounded-full text-sm font-medium border border-blue-500/30"
              >
                {skill}
              </span>
            ))}
          </div>
          <p className="text-xs text-text-tertiary mt-2">
            {mode === UserMode.CANDIDATE
              ? '검색했지만 이 공고에서 요구하지 않는 기술입니다.'
              : '검색했지만 이 후보자가 보유하지 않은 기술입니다.'}
          </p>
        </div>
      )}
    </div>
  );
};