import { useState, useEffect } from 'react';
import { useLazyQuery } from '@apollo/client/react';
import { MatchItem, SkillMatch, UserMode } from '../types';
import { SEARCH_MATCHES_QUERY } from '../services/api/queries/search';

interface SearchMatchesData {
  searchMatches: {
    matches: MatchItem[];
    vectorVisualization: SkillMatch[];
  };
}

export const useSearchMatches = () => {
  const [matches, setMatches] = useState<MatchItem[]>([]);
  const [runSearchQuery, { loading, error, data }] = useLazyQuery<SearchMatchesData>(SEARCH_MATCHES_QUERY);

  useEffect(() => {
    if (data && data.searchMatches) {
      console.log("Received data from API:", data.searchMatches);
      setMatches(data.searchMatches.matches);
    } else if (error) {
      // When an error occurs, we should clear previous results.
      // The QueryBoundary will display the error state to the user.
      setMatches([]);
      console.error("GraphQL query error in useSearchMatches:", error);
    }
  }, [data, error]);

  /**
   * @function runSearch
   * @description Executes the GraphQL query to initiate a search with sanitized variables.
   */
  const runSearch = (mode: UserMode, skills?: (string | null)[], experience?: string | null) => {
    setMatches([]); // Clear previous results immediately on new search

    // Sanitize variables to prevent GraphQL type errors
    const sanitizedSkills = skills ? skills.filter((s): s is string => !!s) : [];
    const sanitizedExperience = experience || null;

    runSearchQuery({
      variables: {
        mode,
        skills: sanitizedSkills,
        experience: sanitizedExperience,
      },
    });
  };

  return {
    runSearch,
    loading,
    error: error || null,
    matches,
  };
};

