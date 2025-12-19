// src/hooks/useSearchMatches.ts
'use client';

import { useLazyQuery } from '@apollo/client/react';
import { SEARCH_MATCHES_QUERY } from '../lib/graphql/queries';
import { MatchItem, SkillMatch, UserMode } from '../types';

interface SearchMatchesData {
  searchMatches: {
    __typename?: 'SimulationResponse';
    matches: MatchItem[];
    vectorVisualization: SkillMatch[];
  };
}

export const useSearchMatches = () => {
  const [runSearch, result] = useLazyQuery<SearchMatchesData>(SEARCH_MATCHES_QUERY);
  const { loading, data, error } = result;

  const handleSearch = (mode: UserMode, skills: string[], experience: string) => {
    if (skills.length === 0) return;

    runSearch({
      variables: {
        mode,
        skills,
        experience,
      },
    });
  };

  const matches = data?.searchMatches?.matches || [];
  const vectorData = data?.searchMatches?.vectorVisualization || [];

  return {
    runSearch: handleSearch,
    loading,
    error,
    matches,
    vectorData,
  };
};
