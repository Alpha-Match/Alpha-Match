'use client';

import React, { useEffect, useTransition } from 'react';
import { useAppDispatch, useAppSelector } from '../../services/state/hooks';
import { setSearchPerformed, setSkillCategories, setDashboardData } from '../../services/state/features/search/searchSlice';
import { setPageViewMode, setSelectedMatchId } from '../../services/state/features/ui/uiSlice';
import { InputPanel } from '../../components/input-panel';
import { Header } from '../../components/layout/Header';
import { useSearchMatches } from '../../hooks/useSearchMatches';
import { MainContentPanel } from '../../components/layout/MainContentPanel';
import { MatchItem, UserMode, SkillCategory, DashboardCategory } from '../../types';
import { CANDIDATE_THEME_COLORS, RECRUITER_THEME_COLORS } from '../../constants';

/**
 * @file HomePage.client.tsx
 * @description 클라이언트 사이드 홈 페이지 컴포넌트
 *              사용자 인터랙션과 상태 관리를 담당합니다.
 *              Server Component에서 전달받은 초기 데이터를 사용합니다.
 * @version 2.1.0
 * @date 2025-12-30
 */

interface InitialDashboardData {
  [UserMode.CANDIDATE]: DashboardCategory[];
  [UserMode.RECRUITER]: DashboardCategory[];
}

interface HomePageClientProps {
  initialSkillCategories: SkillCategory[];
  initialDashboardData: InitialDashboardData;
}

export function HomePageClient({ initialSkillCategories, initialDashboardData }: HomePageClientProps) {
  const dispatch = useAppDispatch();
  const [isPending, startTransition] = useTransition();

  // 서버에서 전달받은 초기 데이터를 Redux에 한 번에 로드
  useEffect(() => {
    if (initialSkillCategories?.length > 0) {
      const allSkills = initialSkillCategories.flatMap(category => category.skills);
      dispatch(setSkillCategories(allSkills));
    }
    if (initialDashboardData) {
      dispatch(setDashboardData({ userMode: UserMode.CANDIDATE, data: initialDashboardData[UserMode.CANDIDATE] }));
      dispatch(setDashboardData({ userMode: UserMode.RECRUITER, data: initialDashboardData[UserMode.RECRUITER] }));
    }
  }, [initialSkillCategories, initialDashboardData, dispatch]);

  const userMode = useAppSelector((state) => state.ui.userMode);
  const { pageViewMode, selectedMatchId } = useAppSelector((state) => state.ui[userMode]);
  const { selectedSkills, selectedExperience, isInitial, matches } = useAppSelector((state) => state.search[userMode]);

  // Debug: Log Redux state
  console.log('[HomePage] Redux matches:', matches.length, 'pageViewMode:', pageViewMode);

  const { runSearch, loadMore, loading, fetchingMore, error, hasMore } = useSearchMatches();

  // userMode 변경 시에만 이전 검색을 복원합니다.
  useEffect(() => {
    if (pageViewMode === 'results' && selectedSkills.length > 0 && !isInitial && matches.length === 0) {
      const sortedSkills = [...selectedSkills].sort();
      runSearch(userMode, sortedSkills, ''); // Empty experience - no filter
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [userMode, pageViewMode, isInitial]);

  const handleSearch = () => {
    startTransition(() => {
      // Sort skills for consistent backend caching
      const sortedSkills = [...selectedSkills].sort();
      dispatch(setSearchPerformed(userMode));
      runSearch(userMode, sortedSkills, ''); // Empty experience - no filter
      dispatch(setPageViewMode({ userMode, pageViewMode: 'results' }));
      dispatch(setSelectedMatchId({ userMode, selectedMatchId: null }));
    });
  };

  const handleMatchSelect = (match: MatchItem) => {
    dispatch(setSelectedMatchId({ userMode, selectedMatchId: match.id }));
    dispatch(setPageViewMode({ userMode, pageViewMode: 'detail' }));
  };

  const handleBackToList = () => {
    dispatch(setSelectedMatchId({ userMode, selectedMatchId: null }));
    dispatch(setPageViewMode({ userMode, pageViewMode: 'results' }));
  };

  const handleBackToDashboard = () => {
    dispatch(setPageViewMode({ userMode, pageViewMode: 'dashboard' }));
  };

  const themeColors = userMode === UserMode.CANDIDATE ? CANDIDATE_THEME_COLORS : RECRUITER_THEME_COLORS;
  const activeColor = themeColors[0];

  return (
    <div className="h-screen w-screen flex flex-col bg-background text-text-primary overflow-hidden font-sans">
      <Header />

      <main className="flex-1 flex overflow-hidden">
        <div className="w-1/3 min-w-[350px] max-w-[450px] h-full z-20 bg-panel-sidebar border-r border-border/30">
          <InputPanel
            onSearch={handleSearch}
            isLoading={isPending && pageViewMode === 'results'}
          />
        </div>

        <div className="flex-1 h-full relative z-10 bg-background p-6 overflow-y-auto custom-scrollbar">
          <MainContentPanel
            userMode={userMode}
            pageViewMode={pageViewMode as "dashboard" | "results" | "detail"}
            matches={matches}
            selectedMatchId={selectedMatchId}
            loading={loading}
            fetchingMore={fetchingMore}
            error={error}
            activeColor={activeColor}
            onMatchSelect={handleMatchSelect}
            onBackToList={handleBackToList}
            onBackToDashboard={handleBackToDashboard}
            loadMore={loadMore}
            hasMore={hasMore}
            selectedSkills={selectedSkills}
          />
        </div>
      </main>
    </div>
  );
}
