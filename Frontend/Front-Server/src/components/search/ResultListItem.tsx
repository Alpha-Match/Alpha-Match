// Frontend/Front-Server/src/components/search/ResultListItem.tsx
/**
 * @file ResultListItem.tsx
 * @description 단일 검색 결과를 리스트 아이템 형태로 표시하는 컴포넌트
 * @version 1.2.1
 * @date 2026-01-11
 */
import React from 'react';
import { MatchItem } from '../../types';
import chroma from 'chroma-js';

interface ResultListItemProps {
  match: MatchItem;
  onSelect: () => void;
  activeColor: string;
  isSelected: boolean;
}

const ResultListItem: React.FC<ResultListItemProps> = ({ match, onSelect, activeColor, isSelected }) => {
  const scoreBgColor = chroma(activeColor).alpha(0.2).css();
  const scoreTextColor = chroma(activeColor).brighten(1).hex();

  const baseClasses = "w-full text-left p-4 shadow-sm rounded-md border transition-all duration-200 cursor-pointer focus:outline-none focus:ring-2 focus:ring-primary";
  const selectedClasses = 'bg-primary/10 border-primary';
  const unselectedClasses = 'bg-panel-main border-border/30 hover:bg-panel-2 hover:border-primary';

  return (
    <li
      onClick={onSelect}
      className={`${baseClasses} ${isSelected ? selectedClasses : unselectedClasses}`}
      style={{ '--active-color': activeColor } as React.CSSProperties}
    >
      <div className="flex justify-between items-center gap-4">
        <div className="flex-grow min-w-0">
          <div className="flex items-center gap-3">
            <h3 className="text-lg font-semibold text-text-primary truncate" title={match.title}>
              {match.title}
            </h3>
          </div>
          <div className="flex items-baseline gap-3 mt-1 text-text-tertiary">
            <span>{match.company}</span>
            <span className="text-text-quaternary">|</span>
            <span>
              경력: {match.experience !== null && match.experience !== undefined ? `${match.experience}년 이상` : '신입/경력무관'}
            </span>
          </div>
        </div>
        
        <div className="flex-shrink-0 flex items-center gap-4">
          <div className="flex flex-wrap gap-2 justify-end max-w-xs">
            {match.skills.slice(0, 3).map((skill, index) => (
              <span
                key={index}
                className="px-2 py-0.5 bg-panel-2 text-text-secondary rounded-full text-xs font-medium"
              >
                {skill}
              </span>
            ))}
            {match.skills.length > 3 && (
                <span className="px-2 py-0.5 bg-panel-2 text-text-tertiary rounded-full text-xs font-medium">
                    +{match.skills.length - 3}
                </span>
            )}
          </div>

          {match.score !== undefined && (
            <span 
              className="text-sm font-medium px-2.5 py-1 rounded-full w-28 text-center"
              style={{ backgroundColor: scoreBgColor, color: scoreTextColor }}
            >
              유사도 {(match.score * 100).toFixed(1)}%
            </span>
          )}
        </div>
      </div>
    </li>
  );
};

export default ResultListItem;
