import {MatchItem, SkillMatch, UserMode} from '@/types';
import {useCallback, useEffect, useRef, useState} from 'react';
import {useQuery} from '@apollo/client/react';
import {NetworkStatus} from '@apollo/client';
import {CombinedGraphQLErrors, ServerError} from '@apollo/client/errors';
import {SEARCH_MATCHES_QUERY} from '@/core/client/services/api/queries/search';
import {useAppDispatch, useAppSelector} from '@/core/client/services/state/hooks';
import {setMatches, setSearchedSkills, clearMatches} from '@/core/client/services/state/features/search/searchSlice';

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
  sortBy?: string;
}

export const useSearchMatches = () => {
  const dispatch = useAppDispatch();
  const currentUiMode = useAppSelector((state) => state.ui.userMode);
  const matches = useAppSelector((state) => state.search[currentUiMode].matches);

  const [searchParams, setSearchParams] = useState<{
    mode: UserMode;
    skills: string[];
    experience: string;
    sortBy?: string;
  } | null>(null);
  const [hasMore, setHasMore] = useState(true);
  const [error, setError] = useState<Error | null>(null);
  const [isSearching, setIsSearching] = useState(false); // 즉각적인 로딩 상태를 위한 로컬 상태

  const lastLoadMoreTime = useRef<number>(0);

  const getSortByString = useCallback((mode: UserMode): string => {
    let sortString = 'score DESC';
    if (mode === UserMode.CANDIDATE) {
      sortString += ', publishedAt DESC';
    } else if (mode === UserMode.RECRUITER) {
      sortString += ', createdAt DESC';
    }
    return sortString;
  }, []);

  const { data, loading: apolloLoading, fetchMore, error: queryError, networkStatus } = useQuery<SearchMatchesData, SearchMatchesVars>(
    SEARCH_MATCHES_QUERY,
    {
      skip: !searchParams,
      variables: {
        mode: searchParams?.mode || UserMode.CANDIDATE,
        skills: searchParams?.skills || [],
        experience: searchParams?.experience || '',
        limit: PAGE_SIZE,
        offset: 0,
        sortBy: searchParams?.sortBy || getSortByString(currentUiMode),
      },
      notifyOnNetworkStatusChange: true,
      fetchPolicy: 'cache-first', // Use cached data first
    }
  );

  const isInitialLoading = (isSearching || (apolloLoading && networkStatus === NetworkStatus.loading)) && matches.length === 0;
  const isFetchingMore = networkStatus === NetworkStatus.fetchMore;

  useEffect(() => {
    if (data && data.searchMatches && searchParams) {
      setIsSearching(false); // 데이터 도착 시 로딩 상태 해제
      console.log('[Search] Query completed:', data);
      console.log('[Search] Matches count:', data.searchMatches.matches.length);

      dispatch(
        setMatches({
          userMode: searchParams.mode,
          matches: data.searchMatches.matches,
        })
      );
      // setSearchedSkills is now dispatched in HomePage.client.tsx before runSearch
      // This ensures that the analysis panel can start fetching concurrently.
      // We keep this here in case the backend returns filtered skills or for robustness.
      dispatch(
        setSearchedSkills({
          userMode: searchParams.mode,
          skills: searchParams.skills,
        })
      );

      if (data.searchMatches.matches.length < PAGE_SIZE) {
        setHasMore(false);
      } else {
        setHasMore(true);
      }
    }
  }, [data, searchParams, dispatch]);

  useEffect(() => {
    if (queryError) {
      setIsSearching(false); // 에러 발생 시 로딩 상태 해제
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

      if (searchParams) {
        dispatch(
          setMatches({
            userMode: searchParams.mode,
            matches: [],
          })
        );
        dispatch(
          setSearchedSkills({
            userMode: searchParams.mode,
            skills: [],
          })
        );
      }
    }
  }, [queryError, searchParams, dispatch]);

  const runSearch = useCallback(
    async (mode: UserMode, skills?: (string | null)[], experience?: string | null, sortBy?: string) => {
      console.log('--- runSearch CALLED ---'); // 디버깅 로그 추가
      const sanitizedSkills = skills ? skills.filter((s): s is string => !!s) : [];
      const sanitizedExperience = experience || '';

      if (sanitizedSkills.length === 0) {
        console.warn('[Search] No skills selected');
        document.dispatchEvent(
          new CustomEvent('show-notification', {
            detail: { message: '최소 1개 이상의 기술 스택을 선택해주세요.', type: 'warning' },
          })
        );
        return;
      }

      const sortedSkills = [...sanitizedSkills].sort();
      const effectiveSortBy = sortBy || getSortByString(mode);

      setError(null);
      setHasMore(true);
      setIsSearching(true); // 검색 시작 시 즉시 로딩 상태로 설정
      dispatch(clearMatches(mode)); // Clear previous matches to show spinner immediately

      console.log('[Search] Starting new search:', {
        mode,
        skills: sortedSkills,
        experience: sanitizedExperience,
        sortBy: effectiveSortBy,
      });

      setSearchParams({
        mode,
        skills: sortedSkills,
        experience: sanitizedExperience,
        sortBy: effectiveSortBy,
      });
    },
    [dispatch, getSortByString]
  );

  const loadMore = useCallback(async () => {
    if (!hasMore || isFetchingMore || !searchParams) return;

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
          offset: currentLength,
          sortBy: searchParams.sortBy || getSortByString(searchParams.mode),
        },
      });

      console.log('[Search] FetchMore result:', result);

      const newMatches = result.data?.searchMatches?.matches || [];
      if (newMatches.length < PAGE_SIZE) {
        setHasMore(false);
      }
    } catch (err) {
      console.error('[Search] FetchMore error:', err);
      setError(err as Error);
    }
  }, [hasMore, isFetchingMore, searchParams, matches.length, fetchMore, getSortByString]);

  return {
    runSearch,
    loadMore,
    loading: isSearching || (apolloLoading && networkStatus !== NetworkStatus.fetchMore),
    fetchingMore: isFetchingMore,
    error,
    matches,
    hasMore,
  };
};
