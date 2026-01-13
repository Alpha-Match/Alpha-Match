/**
 * @file SearchResultPanel.tsx
 * @description 검색 결과 목록을 표시하는 패널 컴포넌트 (분석 기능 분리됨)
 *              범용 MatchItem 배열을 받아 ResultListItem 컴포넌트를 사용하여 렌더링합니다.
 *              Intersection Observer를 통한 무한 스크롤 지원.
 * @version 4.0.0
 * @date 2026-01-11
 */
import React from 'react';
import {MatchItem, UserMode} from '@/types';
import {useIntersectionObserver} from '@/core/client/hooks/ui';
import {ResultListItem} from '@/components/search/results';
import {LoadingSpinner} from '@/components/ui';

interface SearchResultPanelProps {
  matches: MatchItem[];
  onMatchSelect: (match: MatchItem) => void;
  onBackToDashboard?: () => void; // Added this line
  activeColor: string;
  userMode: UserMode;
  loadMore?: () => void;
  hasMore?: boolean;
  loading?: boolean;
  selectedMatchId?: string | null;
}

export const SearchResultPanel: React.FC<SearchResultPanelProps> = ({
  matches,
  onMatchSelect,
  activeColor,
  loadMore,
  hasMore = false,
  loading = false,
  selectedMatchId,
}) => {
  const sentinelRef = useIntersectionObserver<HTMLDivElement>(() => {
    if (hasMore && !loading && loadMore) {
      loadMore();
    }
  }, {
    threshold: 0.1,
    rootMargin: '100px',
  });

  if (matches.length === 0 && !loading) {
    return (
      <div className="text-center p-8 text-text-tertiary h-full flex flex-col justify-center items-center">
        <h3 className="2xl font-bold mb-4">검색 결과가 없습니다.</h3>
        <p>선택하신 조건에 맞는 채용 공고 또는 지원자를 찾지 못했습니다. 다른 기술 스택이나 경력으로 시도해보세요.</p>
      </div>
    );
  }

  return (
    <div className="w-full animate-fade-in h-full flex flex-col">
      <div className="flex items-center justify-between mb-4">
        <h2 className="text-2xl font-bold text-text-primary">
          검색 결과 ({matches.length}건)
        </h2>
      </div>
      
      <div className="flex-1 overflow-y-auto custom-scrollbar pr-2">
        <ul className="space-y-3">
          {matches.map((match) => (
            <ResultListItem
              key={match.id}
              match={match}
              onSelect={() => onMatchSelect(match)}
              activeColor={activeColor}
              isSelected={match.id === selectedMatchId}
            />
          ))}
        </ul>

        {loading && (
          <div className="py-8">
            <LoadingSpinner size={32} message="추가 결과를 불러오는 중..." color={activeColor} />
          </div>
        )}

        {hasMore && !loading && <div ref={sentinelRef} className="h-4" />}

        {!hasMore && matches.length > 0 && (
          <div className="text-center py-8 text-text-tertiary">
            <p className="text-sm">모든 결과를 불러왔습니다.</p>
          </div>
        )}
      </div>
    </div>
  );
};