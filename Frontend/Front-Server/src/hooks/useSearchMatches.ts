import { useState, useEffect } from 'react';
import { useLazyQuery } from '@apollo/client/react';
import { CombinedGraphQLErrors, ServerError } from '@apollo/client/errors'; // Added import
import { MOCK_RECRUITS, MOCK_CANDIDATES } from '../constants/mockData';
import { Recruit, Candidate, UserMode, MatchItem, SkillMatch } from '../types';
import { SEARCH_MATCHES_QUERY } from '../services/api/queries/search';

interface SearchMatchesData {
  searchMatches: {
    matches: MatchItem[];
    vectorVisualization: SkillMatch[];
  };
}

// Mapping functions to convert mock data to MatchItem
const mapRecruitToMatchItem = (recruit: Recruit): MatchItem => ({
    id: recruit.recruit_id,
    title: recruit.position,
    company: recruit.company_name,
    score: recruit.similarity || 0,
    skills: recruit.skills,
    description: recruit.long_description,
    experience: recruit.experience_years,
});

const mapCandidateToMatchItem = (candidate: Candidate): MatchItem => ({
    id: candidate.candidate_id,
    title: candidate.name,
    company: candidate.position_category,
    score: candidate.similarity || 0,
    skills: candidate.skills,
    description: candidate.resume_summary,
    experience: candidate.experience_years,
});


export const useSearchMatches = () => {
  const [matches, setMatches] = useState<MatchItem[]>([]);
  const [currentMode, setCurrentMode] = useState<UserMode>(UserMode.CANDIDATE);
  const [runSearchQuery, { loading, error, data }] = useLazyQuery<SearchMatchesData>(SEARCH_MATCHES_QUERY);

  const showNotification = (message: string, type: 'error' | 'info' | 'success') => {
    document.dispatchEvent(new CustomEvent('show-notification', {
      detail: { message, type }
    }));
  };
  
  useEffect(() => {
    if (error) {
      if (CombinedGraphQLErrors.is(error)) {
        console.error("GraphQL error:", error.errors);
        setMatches([]); // Clear matches on GraphQL error
      } else if (ServerError.is(error)) { // Specific server error (e.g., HTTP error)
        showNotification('네트워크를 확인해주세요.\n임시 데이터를 활용하겠습니다.', 'error');
        console.error("Server error, falling back to mock data:", error);
        const mockData = currentMode === UserMode.CANDIDATE
          ? MOCK_RECRUITS.map(mapRecruitToMatchItem)
          : MOCK_CANDIDATES.map(mapCandidateToMatchItem);
        setMatches(mockData);
      } else { // Other errors including generic network issues not covered by ServerError.is()
        showNotification('네트워크를 확인해주세요.\n임시 데이터를 활용하겠습니다.', 'error');
        console.error("Network or unknown error, falling back to mock data:", error);
        const mockData = currentMode === UserMode.CANDIDATE
          ? MOCK_RECRUITS.map(mapRecruitToMatchItem)
          : MOCK_CANDIDATES.map(mapCandidateToMatchItem);
        setMatches(mockData);
      }
    } else if (data && data.searchMatches) {
      console.log("Received data from API:", data.searchMatches);
      setMatches(data.searchMatches.matches);
    }
  }, [data, error, currentMode]);

  /**
   * @function runSearch
   * @description Executes the GraphQL query to initiate a search.
   */
  const runSearch = (mode: UserMode, skills: string[], experience: string) => {
    setMatches([]); // Clear previous results
    setCurrentMode(mode); // Set current mode for potential fallback
    runSearchQuery({
      variables: {
        mode,
        skills,
        experience,
      },
    });
  };

  return {
    runSearch,
    loading,
    error: error ? error : null,
    matches,
  };
};

