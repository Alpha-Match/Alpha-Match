// Frontend/Front-Server/src/components/search/ResultList.tsx
/**
 * @file ResultList.tsx
 * @description 단일 검색 결과(채용 공고 또는 지원자)를 표시하는 카드 컴포넌트
 *              범용적인 MatchItem 인터페이스를 기반으로 데이터를 받아 렌더링합니다.
 *              클릭 시 상세 보기로 이동합니다.
 *              운영체제: Windows
 */
import React from 'react';
import { MatchItem } from '../../types';
import chroma from 'chroma-js';

interface ResultListProps {
  match: MatchItem;
  onSelect: () => void;
  activeColor: string;
}

const ResultList: React.FC<ResultListProps> = ({ match, onSelect, activeColor }) => {
  const scoreBgColor = chroma(activeColor).alpha(0.2).css();
  const scoreTextColor = chroma(activeColor).brighten(1).hex();

  return (
    <button
      onClick={onSelect}
      className="w-full text-left bg-panel-main shadow-md rounded-lg p-6 border border-border/30 hover:bg-panel-2 hover:border-primary transition-all duration-200 focus:outline-none focus:ring-2 focus:ring-primary"
      style={{ '--active-color': activeColor } as React.CSSProperties}
    >
      <div className="flex justify-between items-start mb-2">
        <h3 className="text-xl font-semibold text-text-primary">{match.title}</h3>
        {match.score !== undefined && (
          <span 
            className="text-sm font-medium px-2.5 py-0.5 rounded-full"
            style={{ backgroundColor: scoreBgColor, color: scoreTextColor }}
          >
            유사도: {(match.score * 100).toFixed(1)}%
          </span>
        )}
      </div>
      <p className="text-text-secondary text-lg mb-1">{match.company}</p>
      <p className="text-text-tertiary text-sm mb-3">
        경력: {match.experience !== null && match.experience !== undefined ? `${match.experience}년 이상` : '신입/경력무관'}
      </p>
      <div className="flex flex-wrap gap-2 mt-4">
        {match.skills.slice(0, 5).map((skill, index) => (
          <span
            key={index}
            className="px-3 py-1 bg-panel-2 text-text-secondary rounded-full text-xs font-medium"
          >
            {skill}
          </span>
        ))}
        {match.skills.length > 5 && (
            <span className="px-3 py-1 bg-panel-2 text-text-tertiary rounded-full text-xs font-medium">
                +{match.skills.length - 5}
            </span>
        )}
      </div>
    </button>
  );
};

export default ResultList;
