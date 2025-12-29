// src/components/input-panel/SearchButton.tsx
import React from 'react';
import { useAppSelector } from '../../services/state/hooks';
import { UserMode } from '../../types';
import { Search } from 'lucide-react';
import { CANDIDATE_THEME_COLORS, RECRUITER_THEME_COLORS } from '../../constants';
import chroma from 'chroma-js';

interface SearchButtonProps {
  onSearch: () => void;
  isLoading: boolean;
}

export const SearchButton: React.FC<SearchButtonProps> = ({
  onSearch,
  isLoading,
}) => {
  const mode = useAppSelector((state) => state.ui.userMode);
  const { selectedSkills } = useAppSelector((state) => state.search[mode]);
  const isCandidate = mode === UserMode.CANDIDATE;

  const themeColors = isCandidate ? CANDIDATE_THEME_COLORS : RECRUITER_THEME_COLORS;
  const primaryColor = themeColors[0];
  
  // 원본 그라데이션과 더 잘 일치시키기 위해 끝 색상의 색조를 변경합니다.
  // 파란색(지원자) 테마의 경우 보라색으로 이동합니다.
  // 보라색(리크루터) 테마의 경우 분홍색으로 이동합니다.
  const gradientEndColor = isCandidate
    ? chroma(primaryColor).set('hsl.h', '+40').hex()
    : chroma(primaryColor).set('hsl.h', '+30').hex();

  const shadowColor = chroma(primaryColor).alpha(0.3).hex();

  const gradientStyle = {
    background: `linear-gradient(to right, ${primaryColor}, ${gradientEndColor})`,
    boxShadow: `0 10px 15px -3px ${shadowColor}, 0 4px 6px -4px ${shadowColor}`,
  };

  return (
    <div className="p-6 border-t border-border">
      <button
        onClick={onSearch}
        disabled={isLoading || selectedSkills.length === 0}
        className="w-full py-3 px-4 rounded-xl text-white text-lg font-bold flex items-center justify-center gap-2 transition-all duration-200 focus:outline-none focus:ring-2 transform hover:-translate-y-0.5 active:translate-y-0 disabled:opacity-50 disabled:cursor-not-allowed"
        style={gradientStyle}
      >
        {isLoading ? (
          <>
            <svg className="animate-spin -ml-1 mr-3 h-5 w-5 text-white" xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24">
              <circle className="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" strokeWidth="4"></circle>
              <path className="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z"></path>
            </svg>
            Calculating Vectors...
          </>
        ) : (
          <>
            {isCandidate ? 'Find Matched Jobs' : 'Find Candidate Profiles'}
            <Search className="w-5 h-5" />
          </>
        )}
      </button>
    </div>
  );
};

