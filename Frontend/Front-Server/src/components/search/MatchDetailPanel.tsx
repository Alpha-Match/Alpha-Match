// Frontend/Front-Server/src/components/MatchDetailPanel.tsx
/**
 * @file MatchDetailPanel.tsx
 * @description 선택된 검색 결과(채용 공고 또는 지원자)의 상세 정보를 표시하는 패널 컴포넌트
 *              운영체제: Windows
 */
import React from 'react';
import { MatchItem } from '../types/appTypes';
import { ChevronLeft } from 'lucide-react';

interface MatchDetailPanelProps {
  match: MatchItem;
  onBack: () => void;
}

const MatchDetailPanel: React.FC<MatchDetailPanelProps> = ({ match, onBack }) => {
  // 간단한 마크다운 형식의 설명을 HTML로 렌더링
  const renderDescription = (text: string = '') => {
    return text
      .split('\n')
      .map(line => line.trim())
      .map((line, index) => {
        if (line.startsWith('### ')) {
          return <h3 key={index} className="text-xl font-semibold mt-6 mb-2 text-gray-300">{line.substring(4)}</h3>;
        }
        if (line.startsWith('- ')) {
          return <li key={index} className="ml-5 list-disc text-gray-400">{line.substring(2)}</li>;
        }
        return <p key={index} className="text-gray-400 mb-2">{line}</p>;
      });
  };

  return (
    <div className="p-6 h-full text-white animate-fade-in">
      <button
        onClick={onBack}
        className="flex items-center text-blue-400 hover:text-blue-300 transition-colors duration-200 mb-6"
      >
        <ChevronLeft size={20} className="mr-1" />
        뒤로가기
      </button>

      <div className="bg-slate-800/50 p-8 rounded-lg">
        {/* 헤더 */}
        <div className="flex justify-between items-start mb-4">
          <h2 className="text-3xl font-bold text-gray-200">{match.title}</h2>
          {match.score !== undefined && (
            <span className="bg-green-500/20 text-green-300 text-lg font-medium px-4 py-1 rounded-full">
              적합도: {(match.score * 100).toFixed(1)}%
            </span>
          )}
        </div>
        <p className="text-gray-400 text-xl mb-4">{match.company}</p>
        <p className="text-gray-500 mb-6">
          경력: {match.experience !== null && match.experience !== undefined ? `${match.experience}년 이상` : '신입/경력무관'}
        </p>

        {/* 기술 스택 */}
        <div className="mb-8">
          <h3 className="text-lg font-semibold mb-3 text-gray-300">보유/요구 기술 스택</h3>
          <div className="flex flex-wrap gap-2">
            {match.skills.map((skill, index) => (
              <span
                key={index}
                className="px-3 py-1 bg-gray-600/50 text-gray-300 rounded-full text-sm font-medium"
              >
                {skill}
              </span>
            ))}
          </div>
        </div>

        {/* 상세 설명 */}
        <div className="prose prose-invert max-w-none">
          {renderDescription(match.description)}
        </div>

        {/* 직무 적합도 대시보드 (Placeholder) */}
        <div className="mt-12">
            <h3 className="text-2xl font-bold text-gray-200 mb-4">직무 적합도 분석</h3>
            <div className="bg-slate-700/50 p-6 rounded-lg text-center">
                <p className="text-gray-400">여기에 지원자의 기술 스택과 해당 공고의 적합도를 분석하는 대시보드가 표시됩니다.</p>
            </div>
        </div>
      </div>
    </div>
  );
};

export default MatchDetailPanel;
