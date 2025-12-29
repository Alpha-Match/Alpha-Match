// Frontend/Front-Server/src/components/SearchResultPanel.tsx
/**
 * @file SearchResultPanel.tsx
 * @description 검색 결과를 목록 형태로 표시하는 패널 컴포넌트
 *              범용 MatchItem 배열을 받아 ResultListItem 컴포넌트를 사용하여 렌더링합니다.
 *              운영체제: Windows
 * @version 1.1.0
 * @date 2025-12-29
 */
import React from 'react';
import { MatchItem } from '../../types';
import ResultListItem from './ResultListItem'; // ResultCard -> ResultListItem

interface SearchResultPanelProps {
  matches: MatchItem[];
  onMatchSelect: (match: MatchItem) => void;
  activeColor: string;
}

const SearchResultPanel: React.FC<SearchResultPanelProps> = ({ matches, onMatchSelect, activeColor }) => {
  if (matches.length === 0) {
    return (
      <div className="text-center p-8 text-text-tertiary h-full flex flex-col justify-center items-center">
        <h3 className="text-2xl font-bold mb-4">검색 결과가 없습니다.</h3>
        <p>선택하신 조건에 맞는 채용 공고 또는 지원자를 찾지 못했습니다. 다른 기술 스택이나 경력으로 시도해보세요.</p>
      </div>
    );
  }

  return (
    <div className="w-full animate-fade-in">
      <h2 className="text-2xl font-bold text-text-primary mb-6">검색 결과 ({matches.length}건)</h2>
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
    </div>
  );
};

export default SearchResultPanel;
