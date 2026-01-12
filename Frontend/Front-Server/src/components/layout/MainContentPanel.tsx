// Frontend/Front-Server/src/components/layout/MainContentPanel.tsx
/**
 * @file MainContentPanel.tsx
 * @description 현재 페이지 뷰 모드에 따라 메인 콘텐츠 영역을 렌더링합니다.
 *              반응형 레이아웃 지원: 넓은 화면에서는 2단 분할, 좁은 화면에서는 단일 뷰.
 * @version 2.1.0
 * @date 2026-01-11
 */
import React, { Suspense } from 'react';
import {MatchItem, SkillCategory, UserMode} from '@/types';
import QueryBoundary from '@/components/utils/QueryBoundary';
import { SearchResultPanel } from '@/components/search/results';
import MainDashboard from '@/app/_components/MainDashboard';
import { MatchDetailPanel } from '@/components/search/detail';
import { LoadingSpinner } from '@/components/ui/LoadingSpinner';

type PageViewMode = 'dashboard' | 'results' | 'detail';

interface MainContentPanelProps {
  userMode: UserMode;
  pageViewMode: PageViewMode;
  matches: MatchItem[];
  selectedMatchId: string | null;
  loading: boolean;
  fetchingMore?: boolean;
  error: Error | null;
  activeColor: string;
  onMatchSelect: (match: MatchItem) => void;
  onBackToList: () => void;
  onBackToDashboard: () => void;
  loadMore?: () => void;
  hasMore?: boolean;
  searchedSkills?: string[];
  skillCategories?: SkillCategory[];
}

export const MainContentPanel: React.FC<MainContentPanelProps> = ({
  userMode,
  pageViewMode,
  matches,
  selectedMatchId,
  loading,
  fetchingMore = false,
  error,
  activeColor,
  onMatchSelect,
  onBackToList,
  onBackToDashboard,
  loadMore,
  hasMore,
  searchedSkills = [],
  skillCategories = [],
}) => {
  if (pageViewMode === 'dashboard') {
    return (
      <QueryBoundary>
        <Suspense fallback={<LoadingSpinner message="대시보드 로딩 중..." />}>
          <MainDashboard key={userMode} activeColor={activeColor} userMode={userMode} />
        </Suspense>
      </QueryBoundary>
    );
  }

  // 'results'와 'detail' 뷰를 위한 반응형 레이아웃
  return (
    <div className="flex w-full h-full lg:gap-4">
      {/* --- 왼쪽 패널: 검색 결과 --- */}
      <div
        className={`w-full lg:w-2/5 flex-shrink-0 h-full ${
          pageViewMode === 'detail' ? 'hidden lg:block' : 'block'
        }`}
      >
        <QueryBoundary loading={loading && matches.length === 0} error={error}>
          <SearchResultPanel
            matches={matches}
            onMatchSelect={onMatchSelect}
            onBackToDashboard={onBackToDashboard}
            activeColor={activeColor}
            userMode={userMode}
            loadMore={loadMore}
            hasMore={hasMore}
            loading={fetchingMore}
            selectedMatchId={selectedMatchId}
          />
        </QueryBoundary>
      </div>

      {/* --- 오른쪽 패널: 상세 정보 또는 플레이스홀더 --- */}
      <div
        className={`w-full lg:w-3/5 flex-grow h-full ${
          pageViewMode === 'detail' ? 'block' : 'hidden lg:block'
        }`}
      >
        {pageViewMode === 'detail' && selectedMatchId ? (
          <MatchDetailPanel
            matchId={selectedMatchId}
            userMode={userMode}
            onBack={onBackToList}
            activeColor={activeColor}
            searchedSkills={searchedSkills}
          />
        ) : (
          <div className="h-full items-center justify-center bg-panel-main rounded-lg border border-border/20 hidden lg:flex">
            <p className="text-text-tertiary px-4 text-center">
              왼쪽 목록에서 항목을 선택하여 상세 정보를 확인하세요.
            </p>
          </div>
        )}
      </div>
    </div>
  );
};
