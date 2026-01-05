// Frontend/Front-Server/src/components/layout/MainContentPanel.tsx
/**
 * @file MainContentPanel.tsx
 * @description 현재 페이지 뷰 모드에 따라 메인 콘텐츠 영역을 렌더링합니다.
 *              무한 스크롤 지원 추가
 * @version 1.2.0
 * @date 2025-12-31
 */
import React, { Suspense } from 'react';
import { MatchItem, UserMode } from '../../types';
import QueryBoundary from '../common/QueryBoundary';
import SearchResultPanel from '../search/SearchResultPanel';
import DefaultDashboard from '../dashboard/DefaultDashboard';
import MatchDetailPanel from '../search/MatchDetailPanel';
import { LoadingSpinner } from '../common/LoadingSpinner';

type PageViewMode = 'dashboard' | 'results' | 'detail';

interface MainContentPanelProps {
  userMode: UserMode;
  pageViewMode: PageViewMode;
  matches: MatchItem[];
  selectedMatchId: string | null;
  loading: boolean;
  fetchingMore?: boolean;
  error: Error | null; // Changed from ApolloError | Error | null
  activeColor: string;
  onMatchSelect: (match: MatchItem) => void;
  onBackToList: () => void;
  onBackToDashboard: () => void;
  // Infinite scroll props
  loadMore?: () => void;
  hasMore?: boolean;
  // Selected skills for category distribution and competency match
  selectedSkills?: string[];
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
  selectedSkills = [],
}) => {
  switch (pageViewMode) {
    case 'detail':
      if (!selectedMatchId) {
        return <div className="text-center text-text-tertiary">매치 상세 정보를 찾을 수 없습니다. 목록으로 돌아가세요.</div>;
      }
      return (
        <MatchDetailPanel
          matchId={selectedMatchId}
          userMode={userMode}
          onBack={onBackToList}
          activeColor={activeColor}
          searchedSkills={selectedSkills}
        />
      );

    case 'results':
      return (
        <QueryBoundary loading={loading} error={error}>
          <SearchResultPanel
            matches={matches}
            onMatchSelect={onMatchSelect}
            onBackToDashboard={onBackToDashboard}
            activeColor={activeColor}
            loadMore={loadMore}
            hasMore={hasMore}
            loading={fetchingMore}
            selectedSkills={selectedSkills}
          />
        </QueryBoundary>
      );

    case 'dashboard':
    default:
      return (
        <QueryBoundary>
          <Suspense fallback={<LoadingSpinner message="대시보드 로딩 중..." />}>
            <DefaultDashboard key={userMode} />
          </Suspense>
        </QueryBoundary>
      );
  }
};
