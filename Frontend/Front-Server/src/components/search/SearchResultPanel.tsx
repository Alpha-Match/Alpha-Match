// Frontend/Front-Server/src/components/SearchResultPanel.tsx
/**
 * @file SearchResultPanel.tsx
 * @description 검색 결과를 목록 형태로 표시하는 패널 컴포넌트
 *              범용 MatchItem 배열을 받아 ResultCard 컴포넌트를 사용하여 렌더링합니다.
 *              운영체제: Windows
 */
import React from 'react';
import { MatchItem } from '../../types';
import ResultCard from './ResultCard';

interface SearchResultPanelProps {
  matches: MatchItem[];
  loading: boolean;
  error: Error | null;
  onMatchSelect: (match: MatchItem) => void;
  activeColor: string;
}

const SearchResultPanel: React.FC<SearchResultPanelProps> = ({ matches, loading, error, onMatchSelect, activeColor }) => {
  if (loading) {
    return (
      <div className="flex justify-center items-center p-8 h-full">
        <div className="animate-spin rounded-full h-12 w-12 border-b-2 border-blue-500"></div>
        <p className="ml-4 text-lg text-gray-500">검색 결과 로딩 중...</p>
      </div>
    );
  }

  if (error && matches.length === 0) {
    return (
      <div className="text-center p-8 text-red-500">
        <p className="text-lg">오류 발생</p>
        <p className="text-sm">{error.message}</p>
      </div>
    );
  }

  if (matches.length === 0) {
    return (
      <div className="text-center p-8 text-gray-400 h-full flex flex-col justify-center items-center">
        <h3 className="text-2xl font-bold mb-4">검색 결과가 없습니다.</h3>
        <p>선택하신 조건에 맞는 채용 공고 또는 지원자를 찾지 못했습니다. 다른 기술 스택이나 경력으로 시도해보세요.</p>
      </div>
    );
  }

  return (
    <div className="w-full animate-fade-in">
      <h2 className="text-2xl font-bold text-gray-200 mb-6">검색 결과 ({matches.length}건)</h2>
      <div className="grid grid-cols-1 lg:grid-cols-2 2xl:grid-cols-3 gap-6">
        {matches.map((match) => (
          <ResultCard
            key={match.id}
            match={match}
            onSelect={() => onMatchSelect(match)}
            activeColor={activeColor}
          />
        ))}
      </div>
    </div>
  );
};

export default SearchResultPanel;
