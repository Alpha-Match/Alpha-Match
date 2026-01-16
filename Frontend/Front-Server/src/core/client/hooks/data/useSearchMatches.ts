import { MatchItem, SkillMatch, UserMode } from '@/types';
import { useCallback, useEffect, useRef, useState } from 'react';
import { useQuery } from '@apollo/client/react';
import { NetworkStatus } from '@apollo/client';
import { SEARCH_MATCHES_QUERY } from '@/core/client/services/api/queries/search';
import { useAppDispatch, useAppSelector } from '@/core/client/services/state/hooks';
import { setMatches, appendMatches, clearMatches } from '@/core/client/services/state/features/search/searchSlice';

const PAGE_SIZE = 20;
const LOAD_MORE_THROTTLE_MS = 300;

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
  
  const [hasMore, setHasMore] = useState(true);
  const [error, setError] = useState<Error | null>(null);
  const [isSearching, setIsSearching] = useState(false); // Use local state for synchronous loading status

  const lastLoadMoreTime = useRef<number>(0);
  const currentSearchVars = useRef<Omit<SearchMatchesVars, 'limit' | 'offset'> | undefined>(undefined);

  const getSortByString = useCallback((mode: UserMode): string => {
    let sortString = 'score DESC';
    if (mode === UserMode.CANDIDATE) sortString += ', publishedAt DESC';
    else if (mode === UserMode.RECRUITER) sortString += ', createdAt DESC';
    return sortString;
  }, []);

  const { error: queryError, fetchMore, networkStatus, refetch } = useQuery<
    SearchMatchesData,
    SearchMatchesVars
  >(SEARCH_MATCHES_QUERY, {
    variables: {
      mode: currentUiMode,
      skills: [],
      experience: '',
      limit: PAGE_SIZE,
      offset: 0,
      sortBy: getSortByString(currentUiMode),
    },
    skip: true,
    notifyOnNetworkStatusChange: true,
    fetchPolicy: 'network-only',
  });

  const isFetchingMore = networkStatus === NetworkStatus.fetchMore;

  useEffect(() => {
    if (queryError) {
      const errorAny = queryError as any;
      if (errorAny.name !== 'AbortError' && !errorAny.message?.includes('aborted')) {
        setError(queryError);
        console.error('[Search] Query error:', queryError);
      }
    }
  }, [queryError]);

  const runSearch = useCallback(
    async (mode: UserMode, skills?: (string | null)[], experience?: string | null, sortBy?: string) => {
      if (isSearching) {
        console.warn('[Search] Search is already in progress.');
        return;
      }

      const sanitizedSkills = skills ? skills.filter((s): s is string => !!s) : [];
      if (sanitizedSkills.length === 0) {
        // Handle no skills selected
        return;
      }

      const newVars = {
        mode,
        skills: [...sanitizedSkills].sort(),
        experience: experience || '',
        sortBy: sortBy || getSortByString(mode),
      };
      currentSearchVars.current = newVars;

      setError(null);
      setHasMore(true);
      setIsSearching(true); // Set loading state immediately
      dispatch(clearMatches(mode));

      try {
        const result = await refetch({ ...newVars, limit: PAGE_SIZE, offset: 0 });
        if (result.error) throw result.error;

        const newMatches = result.data?.searchMatches?.matches || [];
        dispatch(setMatches({ userMode: mode, matches: newMatches }));
        // Note: setSearchedSkills is called by the caller (HomePage.client.tsx) before runSearch
        // to allow concurrent fetching. Do not call it here to avoid triggering ref reset loops.
        setHasMore(newMatches.length >= PAGE_SIZE);

      } catch (e: any) {
        if (e.name !== 'AbortError') {
          console.error('[Search] RunSearch failed:', e);
          setError(e);
        }
      } finally {
        setIsSearching(false); // Unset loading state
      }
    },
    [dispatch, getSortByString, refetch, isSearching]
  );

  const loadMore = useCallback(async () => {
    if (!hasMore || isFetchingMore || !currentSearchVars.current) return;
    const now = Date.now();
    if (now - lastLoadMoreTime.current < LOAD_MORE_THROTTLE_MS) return;
    lastLoadMoreTime.current = now;

    try {
      const result = await fetchMore({ variables: { offset: matches.length } });
      const newMatches = result.data?.searchMatches?.matches || [];
      if (newMatches.length > 0) {
        dispatch(appendMatches({ userMode: currentSearchVars.current.mode, matches: newMatches }));
      }
      setHasMore(newMatches.length >= PAGE_SIZE);
    } catch (err) {
      if ((err as Error).name !== 'AbortError') {
        console.error('[Search] FetchMore error:', err);
        setError(err as Error);
      }
    }
  }, [hasMore, isFetchingMore, matches.length, fetchMore, dispatch]);

  return {
    runSearch,
    loadMore,
    loading: isSearching,
    fetchingMore: isFetchingMore,
    error,
    matches,
    hasMore,
  };
};
