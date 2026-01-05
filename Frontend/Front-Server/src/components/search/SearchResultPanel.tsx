// Frontend/Front-Server/src/components/SearchResultPanel.tsx
/**
 * @file SearchResultPanel.tsx
 * @description 검색 결과를 목록 형태로 표시하는 패널 컴포넌트
 *              범용 MatchItem 배열을 받아 ResultListItem 컴포넌트를 사용하여 렌더링합니다.
 *              Intersection Observer를 통한 무한 스크롤 지원
 *              운영체제: Windows
 * @version 1.3.0
 * @date 2025-12-31
 */
import React, { useEffect, useRef } from 'react';
import { ChevronLeft } from 'lucide-react';
import { MatchItem } from '../../types';
import ResultListItem from './ResultListItem'; // ResultList -> ResultListItem
import { LoadingSpinner } from '../common/LoadingSpinner';
import CategoryPieChart from './CategoryPieChart';

interface SearchResultPanelProps {
  matches: MatchItem[];
  onMatchSelect: (match: MatchItem) => void;
  onBackToDashboard?: () => void;
  activeColor: string;
  // Infinite scroll props
  loadMore?: () => void;
  hasMore?: boolean;
  loading?: boolean;
  // Category distribution
  selectedSkills?: string[];
}

const SearchResultPanel: React.FC<SearchResultPanelProps> = ({
  matches,
  onMatchSelect,
  onBackToDashboard,
  activeColor,
  loadMore,
  hasMore = false,
  loading = false,
  selectedSkills = []
}) => {
  const sentinelRef = useRef<HTMLDivElement>(null);

  // Debug: Log matches to diagnose "no results" issue
  console.log('[SearchResultPanel] Received matches:', matches.length, matches);

  // Intersection Observer for infinite scroll
  useEffect(() => {
    if (!loadMore || !hasMore || loading) return;

    const observer = new IntersectionObserver(
      (entries) => {
        // When sentinel element is visible, load more results
        if (entries[0].isIntersecting) {
          loadMore();
        }
      },
      {
        root: null, // viewport
        rootMargin: '100px', // trigger 100px before reaching the sentinel
        threshold: 0.1,
      }
    );

    const currentSentinel = sentinelRef.current;
    if (currentSentinel) {
      observer.observe(currentSentinel);
    }

    return () => {
      if (currentSentinel) {
        observer.unobserve(currentSentinel);
      }
    };
  }, [loadMore, hasMore, loading]);

  if (matches.length === 0 && !loading) {
    return (
      <div className="text-center p-8 text-text-tertiary h-full flex flex-col justify-center items-center">
        <h3 className="text-2xl font-bold mb-4">검색 결과가 없습니다.</h3>
        <p>선택하신 조건에 맞는 채용 공고 또는 지원자를 찾지 못했습니다. 다른 기술 스택이나 경력으로 시도해보세요.</p>
      </div>
    );
  }

  return (
    <div className="w-full animate-fade-in">
      <div className="flex items-center justify-between mb-6">
        <h2 className="text-2xl font-bold text-text-primary">검색 결과 ({matches.length}건)</h2>
        {onBackToDashboard && (
          <button
            onClick={onBackToDashboard}
            className="flex items-center gap-2 px-4 py-2 text-sm font-medium text-text-secondary hover:text-text-primary bg-panel-main hover:bg-panel-2 rounded-lg border border-border transition-all duration-200"
          >
            <ChevronLeft size={20} />
            대시보드로 돌아가기
          </button>
        )}
      </div>

      {/* Category Distribution Pie Chart */}
      {selectedSkills && selectedSkills.length > 0 && (
        <CategoryPieChart skills={selectedSkills} activeColor={activeColor} />
      )}

      <ul className="space-y-3">
        {matches.map((match) => (
          <ResultListItem
            key={match.id}
            match={match}
            onSelect={() => onMatchSelect(match)}
            activeColor={activeColor}
          />
        ))}
      </ul>

      {/* Infinite scroll: Loading state */}
      {loading && (
        <div className="py-8">
          <LoadingSpinner size={32} message="추가 결과를 불러오는 중..." color={activeColor} />
        </div>
      )}

      {/* Infinite scroll: Sentinel element for Intersection Observer */}
      {hasMore && !loading && <div ref={sentinelRef} className="h-4" />}

      {/* Infinite scroll: No more results message */}
      {!hasMore && matches.length > 0 && (
        <div className="text-center py-8 text-text-tertiary">
          <p className="text-sm">모든 결과를 불러왔습니다.</p>
        </div>
      )}
    </div>
  );
};

export default SearchResultPanel;
