/**
 * @file MatchDetailPanel.tsx
 * @description 선택된 검색 결과의 상세 정보를 표시하는 패널 컴포넌트
 *              useMatchDetail Hook을 사용하여 서버에서 상세 데이터를 가져옵니다.
 *              Recruit/Candidate 도메인별로 다른 UI를 렌더링합니다.
 * @version 2.0.0
 * @date 2025-12-30
 */
import React, {useEffect} from 'react';
import {Award, Briefcase, Calendar, ChevronLeft, Globe, User} from 'lucide-react';
import {useMatchDetail} from '../../../hooks/data/useMatchDetail';
import {UserMode} from '../../../types';
import { LoadingSpinner } from '@/components/ui';
import { RatioPieChart } from '@/components/charts';
import { SkillCompetencyBadge, SkillComparisonRadarChart } from '@/components/search/analysis';
import { MarkdownContentBlock } from '@/components/utils';

interface MatchDetailPanelProps {
  matchId: string;
  userMode: UserMode;
  onBack: () => void;
  activeColor: string;
  searchedSkills?: string[];
}

export const MatchDetailPanel: React.FC<MatchDetailPanelProps> = ({
  matchId,
  userMode,
  onBack,
  activeColor,
  searchedSkills = []
}) => {
  const { fetchDetail, loading, recruitDetail, candidateDetail, error } = useMatchDetail();

  useEffect(() => {
    fetchDetail(userMode, matchId);
  }, [matchId, userMode, fetchDetail]);

  const targetSkills = userMode === UserMode.CANDIDATE ? recruitDetail?.skills : candidateDetail?.skills;

  const matchedSkills = targetSkills?.filter(skill => searchedSkills.includes(skill)) || [];

  const overlapPercentage = searchedSkills.length > 0
    ? (matchedSkills.length / searchedSkills.length) * 100
    : 0;

  const coveragePercentage = targetSkills && targetSkills.length > 0
    ? (matchedSkills.length / targetSkills.length) * 100
    : 0;

  if (loading) {
    return (
      <div className="h-full flex items-center justify-center">
        <LoadingSpinner size={48} color={activeColor} />
      </div>
    );
  }

  if (error) {
    return (
      <div className="h-full flex flex-col items-center justify-center text-text-tertiary">
        <p className="text-lg mb-4">상세 정보를 불러올 수 없습니다.</p>
        <button
          onClick={onBack}
          className="px-4 py-2 bg-panel-main border border-border rounded-lg hover:bg-panel-2 transition-colors"
        >
          목록으로 돌아가기
        </button>
      </div>
    );
  }

  // Recruit Detail (CANDIDATE 모드: 채용 공고 상세)
  if (recruitDetail && userMode === UserMode.CANDIDATE) {
    return (
      <div className="p-6 h-full overflow-y-auto custom-scrollbar animate-fade-in">
        <button
          onClick={onBack}
          className="flex items-center text-text-interactive hover:text-primary-light transition-colors duration-200 mb-6"
        >
          <ChevronLeft size={20} className="mr-1" />
          뒤로가기
        </button>

        <div className="bg-panel-main p-8 rounded-lg border border-border/30 mb-8">
          {/* 헤더 */}
          <div className="flex items-start gap-4 mb-6">
            <div className="p-3 bg-panel-2 rounded-lg">
              <Briefcase size={32} className="text-primary" />
            </div>
            <div className="flex-1">
              <h2 className="text-3xl font-bold text-text-primary mb-2">{recruitDetail.position}</h2>
              <p className="text-text-secondary text-xl">{recruitDetail.companyName}</p>
            </div>
          </div>

          {/* 메타 정보 */}
          <div className="grid grid-cols-2 gap-4 mb-8 p-4 bg-panel-2 rounded-lg">
            <div className="flex items-center gap-2">
              <Award className="text-text-tertiary" size={18} />
              <span className="text-text-secondary">
                경력: {recruitDetail.experienceYears ? `${recruitDetail.experienceYears}년 이상` : '신입/경력무관'}
              </span>
            </div>
            {recruitDetail.englishLevel && (
              <div className="flex items-center gap-2">
                <Globe className="text-text-tertiary" size={18} />
                <span className="text-text-secondary">영어: {recruitDetail.englishLevel}</span>
              </div>
            )}
            {recruitDetail.publishedAt && (
              <div className="flex items-center gap-2">
                <Calendar className="text-text-tertiary" size={18} />
                <span className="text-text-secondary">
                  게시일: {new Date(recruitDetail.publishedAt).toLocaleDateString('ko-KR')}
                </span>
              </div>
            )}
            {recruitDetail.primaryKeyword && (
              <div className="flex items-center gap-2">
                <span className="text-text-tertiary text-sm">주요 키워드:</span>
                <span className="text-text-secondary">{recruitDetail.primaryKeyword}</span>
              </div>
            )}
          </div>

          {/* 기술 스택 */}
          <div className="mb-8">
            <h3 className="text-lg font-semibold mb-3 text-text-primary">요구 기술 스택</h3>
            <div className="flex flex-wrap gap-2">
              {recruitDetail.skills.map((skill, index) => (
                <span
                  key={index}
                  className="px-3 py-1.5 bg-panel-2 text-text-secondary rounded-full text-sm font-medium border border-border/20"
                >
                  {skill}
                </span>
              ))}
            </div>
          </div>
        </div>

        {/* 역량 매칭도 분석 */}
        {searchedSkills && searchedSkills.length > 0 && (
          <SkillCompetencyBadge
            mode={userMode}
            targetId={matchId}
            searchedSkills={searchedSkills}
            activeColor={activeColor}
          />
        )}
        
        {/* Ratio Charts */}
        {searchedSkills && searchedSkills.length > 0 && (
          <div className="bg-panel-main p-6 rounded-lg shadow-lg border border-border/30 my-6">
            <h3 className="text-lg font-semibold mb-6 text-text-primary text-center">기술 스택 일치도</h3>
            <div className="flex justify-around">
              <RatioPieChart
                title="겹침 비율"
                percentage={overlapPercentage}
                color={activeColor}
              />
              <RatioPieChart
                title="포함 비율"
                percentage={coveragePercentage}
                color={activeColor}
              />
            </div>
            <div className="text-center text-xs text-text-tertiary mt-4">
              <p>Overlap: 매칭된 기술 / 내가 선택한 기술</p>
              <p>Coverage: 매칭된 기술 / 공고의 전체 기술</p>
            </div>
          </div>
        )}

        {/* Skill Radar Chart - Tech Stack Comparison */}
        {searchedSkills && searchedSkills.length > 0 && (
          <div className="bg-panel-main p-6 rounded-lg shadow-lg border border-border/30 mb-6">
            <h3 className="text-lg font-semibold mb-4 text-text-primary">기술 스택 비교 분석</h3>
            <SkillComparisonRadarChart
              mode={userMode}
              targetId={matchId}
              searchedSkills={searchedSkills}
            />
          </div>
        )}

        <div className="bg-panel-main p-8 rounded-lg border border-border/30">
          {/* 상세 설명 */}
          <MarkdownContentBlock title="상세 정보" content={recruitDetail.description} />
        </div>
      </div>
    );
  }

  // Candidate Detail (RECRUITER 모드: 후보자 상세)
  if (userMode === UserMode.RECRUITER && candidateDetail) {
    return (
      <div className="p-6 h-full overflow-y-auto custom-scrollbar animate-fade-in">
        <button
          onClick={onBack}
          className="flex items-center text-text-interactive hover:text-primary-light transition-colors duration-200 mb-6"
        >
          <ChevronLeft size={20} className="mr-1" />
          뒤로가기
        </button>

        <div className="bg-panel-main p-8 rounded-lg border border-border/30 mb-8">
          {/* 헤더 */}
          <div className="flex items-start gap-4 mb-6">
            <div className="p-3 bg-panel-2 rounded-lg">
              <User size={32} className="text-primary" />
            </div>
            <div className="flex-1">
              <h2 className="text-3xl font-bold text-text-primary mb-2">후보자 프로필</h2>
              <p className="text-text-secondary text-xl">{candidateDetail.positionCategory}</p>
            </div>
          </div>

          {/* 메타 정보 */}
          <div className="grid grid-cols-2 gap-4 mb-8 p-4 bg-panel-2 rounded-lg">
            <div className="flex items-center gap-2">
              <Award className="text-text-tertiary" size={18} />
              <span className="text-text-secondary">
                경력: {candidateDetail.experienceYears ? `${candidateDetail.experienceYears}년` : '신입'}
              </span>
            </div>
            {candidateDetail.resumeLang && (
              <div className="flex items-center gap-2">
                <Globe className="text-text-tertiary" size={18} />
                <span className="text-text-secondary">언어: {candidateDetail.resumeLang}</span>
              </div>
            )}
            {candidateDetail.createdAt && (
              <div className="flex items-center gap-2">
                <Calendar className="text-text-tertiary" size={18} />
                <span className="text-text-secondary">
                  생성일: {new Date(candidateDetail.createdAt).toLocaleDateString('ko-KR')}
                </span>
              </div>
            )}
            {candidateDetail.updatedAt && (
              <div className="flex items-center gap-2">
                <Calendar className="text-text-tertiary" size={18} />
                <span className="text-text-secondary">
                  수정일: {new Date(candidateDetail.updatedAt).toLocaleDateString('ko-KR')}
                </span>
              </div>
            )}
          </div>

          {/* 기술 스택 */}
          <div className="mb-8">
            <h3 className="text-lg font-semibold mb-3 text-text-primary">보유 기술 스택</h3>
            <div className="flex flex-wrap gap-2">
              {candidateDetail.skills.map((skill, index) => (
                <span
                  key={index}
                  className="px-3 py-1.5 bg-panel-2 text-text-secondary rounded-full text-sm font-medium border border-border/20"
                >
                  {skill}
                </span>
              ))}
            </div>
          </div>
        </div>

        {/* 역량 매칭도 분석 */}
        {searchedSkills && searchedSkills.length > 0 && (
            <div className="mb-8">
            <SkillCompetencyBadge
                mode={userMode}
                targetId={matchId}
                searchedSkills={searchedSkills}
                activeColor={activeColor}
            />
            </div>
            )}

        {/* Ratio Charts */}
        {searchedSkills && searchedSkills.length > 0 && (
          <div className="bg-panel-main p-6 rounded-lg shadow-lg border border-border/30 my-6">
            <h3 className="text-lg font-semibold mb-6 text-text-primary text-center">기술 스택 일치도</h3>
            <div className="flex justify-around">
              <RatioPieChart
                title="겹침 비율"
                percentage={overlapPercentage}
                color={activeColor}
              />
              <RatioPieChart
                title="포함 비율"
                percentage={coveragePercentage}
                color={activeColor}
              />
            </div>
            <div className="text-center text-xs text-text-tertiary mt-4">
              <p>Overlap: 매칭된 기술 / 내가 선택한 기술</p>
              <p>Coverage: 매칭된 기술 / 후보자의 전체 기술</p>
            </div>
          </div>
        )}

        {/* Skill Radar Chart - Tech Stack Comparison */}
        {searchedSkills && searchedSkills.length > 0 && (
          <div className="bg-panel-main p-6 rounded-lg shadow-lg border border-border/30 mb-6">
            <h3 className="text-lg font-semibold mb-4 text-text-primary">기술 스택 비교 분석</h3>
            <SkillComparisonRadarChart
              mode={userMode}
              targetId={matchId}
              searchedSkills={searchedSkills}
            />
          </div>
        )}

        <div className="bg-panel-main p-8 rounded-lg border border-border/30 mt-6">
          {/* 원본 이력서 */}
          <MarkdownContentBlock title="원본 이력서" content={candidateDetail.originalResume} className="mb-8" />
          {/* 상세 정보 (구직 희망사항, 추가 정보) */}
          <MarkdownContentBlock title="추가 정보 (프로젝트, 성과)" content={candidateDetail.moreinfo} className="mb-8" />
          <MarkdownContentBlock title="구직 희망사항" content={candidateDetail.lookingFor} className="mb-8" />
        </div>
      </div>
    );
  }

  return (
    <div className="h-full flex items-center justify-center text-text-tertiary">
      <p>데이터를 불러오는 중...</p>
    </div>
  );
};