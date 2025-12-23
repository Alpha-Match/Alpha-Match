import { useState, useEffect } from 'react';
import { useLazyQuery } from '@apollo/client/react';
import { MOCK_RECRUITS, MOCK_CANDIDATES } from '../constants/mockData';
import { Recruit, Candidate, UserMode, MatchItem, SkillMatch } from '../types/appTypes';
import { SEARCH_MATCHES_QUERY } from '../lib/graphql/queries';

interface SearchMatchesResult {
  searchMatches: {
    matches: {
        id: string;
        title: string;
        company: string;
        score: number;
        skills: string[];
    }[];
    vectorVisualization: SkillMatch[];
  };
}

// Recruit 데이터를 MatchItem으로 변환하는 맵핑 함수
const mapRecruitToMatchItem = (recruit: Recruit): MatchItem => ({
    id: recruit.recruit_id,
    title: recruit.position,
    company: recruit.company_name,
    score: recruit.similarity || 0,
    skills: recruit.skills,
    description: recruit.long_description,
    experience: recruit.experience_years,
});

// Candidate 데이터를 MatchItem으로 변환하는 맵핑 함수
const mapCandidateToMatchItem = (candidate: Candidate): MatchItem => ({
    id: candidate.candidate_id,
    title: candidate.name,
    company: candidate.position_category, // 부제로는 직무 카테고리를 사용
    score: candidate.similarity || 0,
    skills: candidate.skills,
    description: candidate.resume_summary,
    experience: candidate.experience_years,
});


export const useSearchMatches = () => {
  const [matches, setMatches] = useState<MatchItem[]>([]);
  // Apollo의 useLazyQuery 훅을 사용하여 API 호출 준비 (제네릭 타입 적용)
  const [runSearchQuery, { loading, error, data }] = useLazyQuery<SearchMatchesResult>(SEARCH_MATCHES_QUERY);
  
  // 현재 활성 모드를 추적하기 위한 state
  const [currentMode, setCurrentMode] = useState<UserMode>(UserMode.CANDIDATE);

  useEffect(() => {
    // API 호출 성공 시
    if (data && data.searchMatches) {
        console.log("API로부터 실제 데이터를 받았습니다:", data.searchMatches.matches);
        // TODO: 실제 API 응답 (data.searchMatches.matches)을 MatchItem[]으로 변환하는 로직 구현
        // 현재는 API 응답이 없으므로 Mock 데이터를 사용 (API로부터 실제 데이터가 오면 이 부분 수정 필요)
        let mappedData: MatchItem[];
        if (currentMode === UserMode.CANDIDATE) {
            mappedData = MOCK_RECRUITS.map(mapRecruitToMatchItem);
        } else {
            mappedData = MOCK_CANDIDATES.map(mapCandidateToMatchItem);
        }
        setMatches(mappedData);
    }
    // API 호출 실패 시
    else if (error) {
        console.error("API 호출에 실패하여 Mock 데이터를 사용합니다.", error);
        // 실패 시 Mock 데이터로 대체
        let mappedData: MatchItem[];
        if (currentMode === UserMode.CANDIDATE) {
            mappedData = MOCK_RECRUITS.map(mapRecruitToMatchItem);
        } else {
            mappedData = MOCK_CANDIDATES.map(mapCandidateToMatchItem);
        }
        setMatches(mappedData);
        // Mock 데이터를 사용하기로 했으므로, 에러 상태를 초기화하여 화면에 오류 메시지가 표시되지 않도록 함
        // error 상태는 useLazyQuery에서 관리하므로, 별도 setError(null) 필요 없음.
        // 다만, error 객체가 계속 존재하는 경우 SearchResultPanel에서 오류를 표시할 수 있으므로,
        // useLazyQuery의 error 객체를 다른 방식으로 처리하거나, SearchResultPanel 렌더링 로직을 수정해야 함.
        // 현재는 error 객체가 있으면 바로 오류 화면을 렌더링하므로, Mock 데이터를 보여주려면 error를 없애야 함.
        // useLazyQuery의 error는 직접 null로 만들 수 없으므로, SearchResultPanel에서 error가 있을 때만 렌더링하도록 수정하는 게 맞음.
        // 일단 Mock 데이터가 표시되게 하기 위해 SearchResultPanel의 error prop을 임시로 null로 가정하여 setMatches가 동작하게 함.
        // 이 부분은 실제 API 연동 시 data, loading, error 상태를 더 정교하게 관리해야 함.
    }
  }, [data, error, currentMode]);

  /**
   * @function runSearch
   * @description GraphQL 쿼리를 실행하여 검색을 시작합니다.
   */
  const runSearch = (mode: UserMode, skills: string[], experience: string) => {
    setMatches([]);
    setCurrentMode(mode); // 현재 모드 저장
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
