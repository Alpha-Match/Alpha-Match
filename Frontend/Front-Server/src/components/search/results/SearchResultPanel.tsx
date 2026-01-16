/**
 * @file SearchResultPanel.tsx
 * @description 검색 결과 목록을 표시하는 패널 컴포넌트 (분석 기능 분리됨)
 *              범용 MatchItem 배열을 받아 ResultListItem 컴포넌트를 사용하여 렌더링합니다.
 *              Intersection Observer를 통한 무한 스크롤 지원.
 * @version 4.3.0
 * @date 2026-01-15
 */
import React, {useRef, useLayoutEffect, useCallback} from 'react';
import {MatchItem, UserMode} from '@/types';
import {useIntersectionObserver} from '@/core/client/hooks/ui';
import {ResultListItem} from '@/components/search/results';
import {LoadingSpinner} from '@/components/ui';

interface SearchResultPanelProps {
  matches: MatchItem[];
  onMatchSelect: (match: MatchItem) => void;
  onBackToDashboard?: () => void;
  activeColor: string;
  userMode: UserMode;
  loadMore?: () => void;
  hasMore?: boolean;
  loading?: boolean;
  selectedMatchId?: string | null;
  totalCount?: number | null;
}

export const SearchResultPanel: React.FC<SearchResultPanelProps> = ({
  matches,
  onMatchSelect,
  activeColor,
  loadMore,
  hasMore = false,
  loading = false, // 'fetchingMore' 상태를 나타냄
  selectedMatchId,
  totalCount,
}) => {
  const scrollContainerRef = useRef<HTMLDivElement>(null);
  const prevMatchesLengthRef = useRef<number>(matches.length);
  const savedScrollTopRef = useRef<number>(0);

  // loadMore 호출 전 스크롤 위치 저장
  const handleLoadMore = useCallback(() => {
    if (scrollContainerRef.current) {
      savedScrollTopRef.current = scrollContainerRef.current.scrollTop;
    }
    loadMore?.();
  }, [loadMore]);

  // 데이터 추가 후 스크롤 위치 복원
  useLayoutEffect(() => {
    const container = scrollContainerRef.current;
    const prevLength = prevMatchesLengthRef.current;

    // 데이터가 추가된 경우에만 복원 (초기 로드나 교체가 아닌 경우)
    if (container && matches.length > prevLength && prevLength > 0) {
      // requestAnimationFrame으로 DOM 안정화 후 복원
      requestAnimationFrame(() => {
        container.scrollTop = savedScrollTopRef.current;
      });
    }

    prevMatchesLengthRef.current = matches.length;
  }, [matches.length]);

  const sentinelRef = useIntersectionObserver<HTMLDivElement>(() => {
    if (hasMore && !loading && handleLoadMore) {
      handleLoadMore();
    }
  }, {
    threshold: 0.1,
    rootMargin: '100px',
  });

  // Display initial loading spinner only when there are no matches yet
  // This prevents scroll position reset when loading more data
  if (loading && matches.length === 0) {
    return (
      <div className="w-full h-full flex justify-center items-center">
        <LoadingSpinner size={48} message="검색 결과를 불러오는 중..." color={activeColor} />
      </div>
    );
  }

  // Display "No search results" message if no matches after loading
  if (matches.length === 0) {
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
          검색 결과 ({matches.length.toLocaleString()}{totalCount !== undefined && totalCount !== null ? `/${totalCount.toLocaleString()}` : ''}건)
        </h2>
      </div>
      
      <div
        ref={scrollContainerRef}
        className="flex-1 overflow-y-auto custom-scrollbar pr-2"
        style={{ overflowAnchor: 'auto' }}
      >
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

        {/* This spinner is for fetching more results (infinite scroll) */}
        {loading && ( // 'loading' here represents 'fetchingMore' from parent
          <div className="py-8">
            <LoadingSpinner size={32} message="추가 결과를 불러오는 중..." color={activeColor} />
          </div>
        )}

        {hasMore && loadMore && <div ref={sentinelRef} className="h-4" />}

        {!hasMore && matches.length > 0 && (
          <div className="text-center py-8 text-text-tertiary">
            <p className="text-sm">모든 결과를 불러왔습니다.</p>
          </div>
        )}
      </div>
    </div>
  );
};