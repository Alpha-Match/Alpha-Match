// src/components/SearchButton.tsx
import React from 'react';
import { useAppSelector } from '../../store/hooks';
import { UserMode } from '../../types';
import { Search } from 'lucide-react';

interface SearchButtonProps {
  onSearch: () => void;
  isLoading: boolean;
}

export const SearchButton: React.FC<SearchButtonProps> = ({
  onSearch,
  isLoading,
}) => {
  const { activeTab: mode, selectedSkills } = useAppSelector((state) => state.search);
  const isCandidate = mode === UserMode.CANDIDATE;

  return (
    <div className="p-6 border-t border-slate-100 bg-white">
      <button
        onClick={onSearch}
        disabled={isLoading || selectedSkills.length === 0}
        className={`w-full py-3 px-4 rounded-xl text-white font-bold text-lg shadow-lg transform transition hover:-translate-y-0.5 active:translate-y-0 disabled:opacity-50 disabled:cursor-not-allowed flex items-center justify-center gap-2 ${
          isCandidate 
            ? 'bg-gradient-to-r from-blue-600 to-indigo-600 hover:from-blue-700 hover:to-indigo-700 shadow-blue-200' 
            : 'bg-gradient-to-r from-purple-600 to-pink-600 hover:from-purple-700 hover:to-pink-700 shadow-purple-200'
        }`}
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
