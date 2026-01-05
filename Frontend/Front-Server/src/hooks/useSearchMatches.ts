import { useState, useCallback, useEffect, useRef } from 'react';
import { useQuery } from '@apollo/client/react';
import { NetworkStatus } from '@apollo/client';
import { CombinedGraphQLErrors, ServerError } from '@apollo/client/errors';
import { MatchItem, SkillMatch, UserMode } from '../types';
import { SEARCH_MATCHES_QUERY } from '../services/api/queries/search';
import { useAppSelector, useAppDispatch } from '../services/state/hooks';
import { setMatches } from '../services/state/features/search/searchSlice';

const PAGE_SIZE = 20; // 한 번에 로드할 개수
const LOAD_MORE_THROTTLE_MS = 300; // 무한 스크롤 요청 간 최소 간격 (ms)

interface SearchMatchesData {
  searchMatches: {
    matches: MatchItem[];
    vectorVisualization: SkillMatch[];
  };
}

interface SearchMatchesVars {
  mode: UserMode;
  skills: string[];
  experience: string;
  limit: number;
  offset: number;
}

export const useSearchMatches = () => {
  const dispatch = useAppDispatch();
  const currentUiMode = useAppSelector((state) => state.ui.userMode);
  const matches = useAppSelector((state) => state.search[currentUiMode].matches);

  const [searchParams, setSearchParams] = useState<{
    mode: UserMode;
    skills: string[];
    experience: string;
  } | null>(null);
  const [hasMore, setHasMore] = useState(true);
  const [error, setError] = useState<Error | null>(null);

  // Throttle ref for loadMore to prevent rapid consecutive requests
  const lastLoadMoreTime = useRef<number>(0);

  // useQuery: 검색 파라미터가 있을 때만 실행
  const { data, loading, fetchMore, error: queryError, networkStatus } = useQuery<SearchMatchesData, SearchMatchesVars>(
    SEARCH_MATCHES_QUERY,
    {
      skip: !searchParams, // 검색 파라미터 없으면 실행 안 함
      variables: {
        mode: searchParams?.mode || UserMode.CANDIDATE,
        skills: searchParams?.skills || [],
        experience: searchParams?.experience || '',
        limit: PAGE_SIZE,
        offset: 0,
      },
      notifyOnNetworkStatusChange: true, // fetchMore 시 loading 상태 업데이트
    }
  );

  // 초기 로딩과 fetchMore 로딩 구분
  const isInitialLoading = networkStatus === NetworkStatus.loading && matches.length === 0;
  const isFetchingMore = networkStatus === NetworkStatus.fetchMore;

  // Handle query completion
  useEffect(() => {
    if (data && data.searchMatches && searchParams) {
      console.log('[Search] Query completed:', data);
      console.log('[Search] Matches count:', data.searchMatches.matches.length);

      // Redux ViewModel에 저장 (첫 검색)
      console.log('[Search] Dispatching setMatches for mode:', searchParams.mode, 'with', data.searchMatches.matches.length, 'matches');
      dispatch(
        setMatches({
          userMode: searchParams.mode,
          matches: data.searchMatches.matches,
        })
      );

      // 더 이상 데이터가 없으면 hasMore = false
      if (data.searchMatches.matches.length < PAGE_SIZE) {
        setHasMore(false);
      } else {
        setHasMore(true);
      }
    }
  }, [data, searchParams, dispatch]);

  // Handle query error
  useEffect(() => {
    if (queryError) {
      setError(queryError);
      console.error('[Search] Query error:', queryError);

      let userMessage = 'An unexpected error occurred during the search.';
      if (CombinedGraphQLErrors.is(queryError)) {
        userMessage = queryError.errors.map((e) => e.message).join(' ');
      } else if (ServerError.is(queryError)) {
        userMessage = 'Server is not responding. Please try again later.';
      } else {
        userMessage = 'A network error occurred. Please check your connection.';
      }

      document.dispatchEvent(
        new CustomEvent('show-notification', {
          detail: { message: userMessage, type: 'error' },
        })
      );

      // 에러 시 빈 배열로 초기화
      if (searchParams) {
        dispatch(
          setMatches({
            userMode: searchParams.mode,
            matches: [],
          })
        );
      }
    }
  }, [queryError, searchParams, dispatch]);

  // 첫 검색 실행
  const runSearch = useCallback(
    async (mode: UserMode, skills?: (string | null)[], experience?: string | null) => {
      const sanitizedSkills = skills ? skills.filter((s): s is string => !!s) : [];
      const sanitizedExperience = experience || '';

      // 유효성 검사
      if (sanitizedSkills.length === 0) {
        console.warn('[Search] No skills selected');
        document.dispatchEvent(
          new CustomEvent('show-notification', {
            detail: { message: '최소 1개 이상의 기술 스택을 선택해주세요.', type: 'warning' },
          })
        );
        return;
      }

      // Sort skills for consistent backend caching
      const sortedSkills = [...sanitizedSkills].sort();

      setError(null);
      setHasMore(true);

      console.log('[Search] Starting new search:', {
        mode,
        skills: sortedSkills,
        experience: sanitizedExperience,
      });

      // 검색 파라미터 설정 → useQuery 자동 실행
      setSearchParams({
        mode,
        skills: sortedSkills,
        experience: sanitizedExperience,
      });
    },
    [dispatch]
  );

  // 무한 스크롤: 다음 페이지 로드
  const loadMore = useCallback(async () => {
    if (!hasMore || isFetchingMore || !searchParams) return;

    // Throttle: 이전 요청으로부터 최소 간격 유지
    const now = Date.now();
    const timeSinceLastLoad = now - lastLoadMoreTime.current;
    if (timeSinceLastLoad < LOAD_MORE_THROTTLE_MS) {
      console.log('[Search] Throttled - too soon since last load:', timeSinceLastLoad, 'ms');
      return;
    }
    lastLoadMoreTime.current = now;

    const currentLength = matches.length;

    console.log('[Search] Loading more:', { currentLength, hasMore });

    try {
      const result = await fetchMore({
        variables: {
          offset: currentLength, // 현재까지 로드된 개수
        },
      });

      console.log('[Search] FetchMore result:', result);

      // 더 이상 데이터가 없으면 hasMore = false
      const newMatches = result.data?.searchMatches?.matches || [];
      if (newMatches.length < PAGE_SIZE) {
        setHasMore(false);
      }
    } catch (err) {
      console.error('[Search] FetchMore error:', err);
      setError(err as Error);
    }
  }, [hasMore, isFetchingMore, searchParams, matches.length, fetchMore]);

  return {
    runSearch,
    loadMore,
    loading: isInitialLoading, // 초기 로딩만 전체 화면 로딩으로 처리
    fetchingMore: isFetchingMore, // fetchMore 로딩은 하단 스피너로 처리
    error,
    matches, // Redux에서 읽어온 현재 모드의 matches
    hasMore,
  };
};
