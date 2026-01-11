import { useLazyQuery } from '@apollo/client/react';
import { UserMode, RecruitDetail, CandidateDetail } from '../types';
import { GET_RECRUIT_DETAIL, GET_CANDIDATE_DETAIL } from '../services/api/queries/search';

interface RecruitDetailData {
  getRecruit: RecruitDetail;
}

interface CandidateDetailData {
  getCandidate: CandidateDetail;
}

/**
 * useMatchDetail Hook
 * 선택된 매칭 아이템의 상세 정보를 조회하는 Hook
 *
 * @returns {object} - fetchDetail, loading, detail, error
 */
export const useMatchDetail = () => {
  const [getRecruitDetail, { loading: recruitLoading, data: recruitData, error: recruitError }] =
    useLazyQuery<RecruitDetailData>(GET_RECRUIT_DETAIL, {
      fetchPolicy: 'cache-first', // Detail은 캐싱 활용
    });

  const [getCandidateDetail, { loading: candidateLoading, data: candidateData, error: candidateError }] =
    useLazyQuery<CandidateDetailData>(GET_CANDIDATE_DETAIL, {
      fetchPolicy: 'cache-first',
    });

  /**
   * Detail 정보 조회
   * @param mode - CANDIDATE (채용공고 상세) or RECRUITER (후보자 상세)
   * @param id - 아이템 ID
   */
  const fetchDetail = async (mode: UserMode, id: string) => {
    try {
      if (mode === UserMode.CANDIDATE) {
        // CANDIDATE 모드: 채용 공고 상세 조회
        await getRecruitDetail({ variables: { id } });
      } else {
        // RECRUITER 모드: 후보자 상세 조회
        await getCandidateDetail({ variables: { id } });
      }
    } catch (error: any) { // Type 'any' for error to access 'name' property
      if (error.name === 'AbortError') {
        // This is an expected error when a request is cancelled (e.g., component unmounts).
        // Log it as info or debug, or just return silently.
        console.info('Fetch detail aborted:', error.message);
      } else {
        // For other unexpected errors, log as an error.
        console.error('Failed to fetch detail:', error);
      }
      // Error is also exposed via recruitError or candidateError from useLazyQuery
    }
  };

  return {
    fetchDetail,
    loading: recruitLoading || candidateLoading,
    recruitDetail: recruitData?.getRecruit || null,
    candidateDetail: candidateData?.getCandidate || null,
    error: recruitError || candidateError || null,
  };
};
