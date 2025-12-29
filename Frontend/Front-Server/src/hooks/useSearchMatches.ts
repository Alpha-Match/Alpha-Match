import { useState, useEffect, useRef, useCallback } from 'react';
import { useLazyQuery } from '@apollo/client/react';
import { MatchItem, SkillMatch, UserMode } from '../types';
import { SEARCH_MATCHES_QUERY } from '../services/api/queries/search';
import { useAppSelector } from '../services/state/hooks'; // Import useAppSelector

interface SearchMatchesData {
  searchMatches: {
    matches: MatchItem[];
    vectorVisualization: SkillMatch[];
  };
}

export const useSearchMatches = () => {
  const currentUiMode = useAppSelector((state) => state.ui.userMode); // Get current mode from Redux

  const [modeMatches, setModeMatches] = useState<Record<UserMode, MatchItem[]>>({
    [UserMode.CANDIDATE]: [],
    [UserMode.RECRUITER]: [],
  });

  const [runSearchQuery, { loading, error, data }] = useLazyQuery<SearchMatchesData>(SEARCH_MATCHES_QUERY);

  // Keep track of the mode for which the last query was run
  const lastQueryMode = useRef<UserMode | null>(null);

  useEffect(() => {
    if (data && data.searchMatches) {
      console.log("Received data from API:", data.searchMatches);
      if (lastQueryMode.current) {
        setModeMatches(prev => ({
          ...prev,
          [lastQueryMode.current]: data.searchMatches.matches,
        }));
      }
    } else if (error) {
      console.error("GraphQL query error in useSearchMatches:", error);
      if (lastQueryMode.current) {
        setModeMatches(prev => ({
          ...prev,
          [lastQueryMode.current]: [], // Clear matches for the mode that encountered an error
        }));
      }
    }
  }, [data, error]);

  /**
   * @function runSearch
   * @description Executes the GraphQL query to initiate a search with sanitized variables.
   */
  const runSearch = useCallback((mode: UserMode, skills?: (string | null)[], experience?: string | null) => {
    lastQueryMode.current = mode; // Store the mode for the current query

    // Sanitize variables to prevent GraphQL type errors
    const sanitizedSkills = skills ? skills.filter((s): s is string => !!s) : [];
    const sanitizedExperience = experience || "";

    runSearchQuery({
      variables: {
        mode,
        skills: sanitizedSkills,
        experience: sanitizedExperience,
      },
    });
  }, [runSearchQuery]); // Dependency on runSearchQuery

  return {
    runSearch,
    loading,
    error: error || null,
    matches: modeMatches[currentUiMode], // Return matches for the currently active UI mode
  };
};
