// Frontend/Front-Server/src/components/layout/MainContentPanel.tsx
/**
 * @file MainContentPanel.tsx
 * @description 현재 페이지 뷰 모드에 따라 메인 콘텐츠 영역을 렌더링합니다.
 * @version 1.0.0
 * @date 2025-12-29
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
  error: Error | null; // Changed from ApolloError | Error | null
  activeColor: string;
  onMatchSelect: (match: MatchItem) => void;
  onBackToList: () => void;
}

export const MainContentPanel: React.FC<MainContentPanelProps> = ({
  userMode,
  pageViewMode,
  matches,
  selectedMatchId,
  loading,
  error,
  activeColor,
  onMatchSelect,
  onBackToList,
}) => {
  switch (pageViewMode) {
    case 'detail':
      const selectedMatch = matches.find((m) => m.id === selectedMatchId);
      if (!selectedMatch) {
        // 상세 보기 상태에서 사용자가 모드를 전환할 때 발생할 수 있습니다.
        // 안전한 뷰로 대체합니다. 간단한 메시지로 충분합니다.
        return <div className="text-center text-text-tertiary">매치 상세 정보를 찾을 수 없습니다. 목록으로 돌아가세요.</div>;
      }
      return <MatchDetailPanel match={selectedMatch} onBack={onBackToList} />;

    case 'results':
      return (
        <QueryBoundary loading={loading} error={error}>
          <SearchResultPanel
            matches={matches}
            onMatchSelect={onMatchSelect}
            activeColor={activeColor}
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
