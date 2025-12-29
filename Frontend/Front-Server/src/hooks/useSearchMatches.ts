import { useState, useCallback } from 'react';
import { useLazyQuery } from '@apollo/client/react';
import { CombinedGraphQLErrors, ServerError } from '@apollo/client/errors';
import { MatchItem, SkillMatch, UserMode } from '../types';
import { SEARCH_MATCHES_QUERY } from '../services/api/queries/search';
import { useAppSelector } from '../services/state/hooks';

interface SearchMatchesData {
  searchMatches: {
    matches: MatchItem[];
    vectorVisualization: SkillMatch[];
  };
}

export const useSearchMatches = () => {
  const currentUiMode = useAppSelector((state) => state.ui.userMode);

  const [modeMatches, setModeMatches] = useState<Record<UserMode, MatchItem[]>>({
    [UserMode.CANDIDATE]: [],
    [UserMode.RECRUITER]: [],
  });

  const [runSearchQuery, { loading }] = useLazyQuery<SearchMatchesData>(SEARCH_MATCHES_QUERY);
  const [error, setError] = useState<Error | null>(null);

  const runSearch = useCallback(async (mode: UserMode, skills?: (string | null)[], experience?: string | null) => {
    const sanitizedSkills = skills ? skills.filter((s): s is string => !!s) : [];
    const sanitizedExperience = experience || "";
    
    setError(null);

    try {
      const { data } = await runSearchQuery({
        variables: {
          mode,
          skills: sanitizedSkills,
          experience: sanitizedExperience,
        },
      });

      if (data && data.searchMatches) {
        setModeMatches(prev => ({
          ...prev,
          [mode]: data.searchMatches.matches,
        }));
      }
    } catch (e: unknown) {
        const err = e as Error;
        setError(err);
        
        let userMessage = "An unexpected error occurred during the search.";
        if (CombinedGraphQLErrors.is(err)) {
            userMessage = err.errors.map(e => e.message).join(' ');
        } else if (ServerError.is(err)) {
            userMessage = 'Server is not responding. Please try again later.';
        } else {
            userMessage = 'A network error occurred. Please check your connection.';
        }
        
        document.dispatchEvent(new CustomEvent('show-notification', {
            detail: { message: userMessage, type: 'error' }
        }));

        setModeMatches(prev => ({
          ...prev,
          [mode]: [],
        }));
    }
  }, [runSearchQuery]);

  return {
    runSearch,
    loading,
    error,
    matches: modeMatches[currentUiMode],
  };
};
