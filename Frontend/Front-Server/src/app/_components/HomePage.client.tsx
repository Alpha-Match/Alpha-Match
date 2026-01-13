'use client';

import React, {useEffect, useTransition} from 'react';
import {useAppDispatch, useAppSelector} from '@/core/client/services/state/hooks';
import {
    setDashboardData,
    setSearchPerformed,
    setSkillCategories
} from '@/core/client/services/state/features/search/searchSlice';
import {useAppNavigation} from '@/core/client/hooks/navigation';
import {useMediaQuery} from '@/core/client/hooks/ui';

import {Header} from '@/components/layout/Header';
import {useSearchMatches} from '@/core/client/hooks/data/useSearchMatches';
import {DashboardCategory, SkillCategory, UserMode} from '@/types';
import {CANDIDATE_THEME_COLORS, RECRUITER_THEME_COLORS} from '@/constants';

import {MainContentPanel} from '@/components/layout/MainContentPanel'; // Added this line


/**
 * @file HomePage.client.tsx
 * @description 클라이언트 사이드 홈 페이지 컴포넌트.
 *              화면 크기에 따라 데스크탑 또는 모바일 레이아웃을 동적으로 렌더링합니다.
 * @version 5.0.0
 * @date 2026-01-11
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
  const isDesktop = useMediaQuery('(min-width: 1024px)');
  const theme = useAppSelector((state) => state.theme.theme);

  const {
    userMode,
    pageViewMode,
    selectedMatchId,
    navigateToResults,
    navigateToDetail,
    goBack,
    navigateToDashboard,
    navigateToInput,
    navigateToView,
  } = useAppNavigation();

  useEffect(() => {
    if (initialSkillCategories?.length > 0) {
      dispatch(setSkillCategories(initialSkillCategories));
    }
    if (initialDashboardData) {
      dispatch(setDashboardData({ userMode: UserMode.CANDIDATE, data: initialDashboardData[UserMode.CANDIDATE] }));
      dispatch(setDashboardData({ userMode: UserMode.RECRUITER, data: initialDashboardData[UserMode.RECRUITER] }));
    }
  }, [initialSkillCategories, initialDashboardData, dispatch]);

  const { selectedSkills, searchedSkills, isInitial, matches } = useAppSelector((state) => state.search[userMode]);
  
  const { runSearch, loadMore, loading, fetchingMore, error, hasMore } = useSearchMatches();

  useEffect(() => {
    if (pageViewMode === 'results' && searchedSkills.length > 0 && !isInitial && matches.length === 0) {
      runSearch(userMode, searchedSkills);
    }
  }, [userMode, pageViewMode, isInitial, searchedSkills, matches.length, runSearch]);

  const handleSearch = () => {
    startTransition(() => {
      const sortedSkills = [...selectedSkills].sort();
      dispatch(setSearchPerformed(userMode));
      runSearch(userMode, sortedSkills);
      navigateToResults();
    });
  };

  const themeColors = userMode === UserMode.CANDIDATE ? CANDIDATE_THEME_COLORS : RECRUITER_THEME_COLORS;
  const activeColor = themeColors[0];



  return (
    <div className="h-screen w-screen flex flex-col bg-background text-text-primary overflow-hidden font-sans">
      <Header />
      <MainContentPanel
        userMode={userMode}
        pageViewMode={pageViewMode}
        matches={matches}
        selectedMatchId={selectedMatchId}
        loading={loading}
        fetchingMore={fetchingMore}
        error={error}
        activeColor={activeColor}
        onMatchSelect={navigateToDetail}
        onBackToList={goBack}
        onBackToDashboard={navigateToDashboard}
        loadMore={loadMore}
        hasMore={hasMore}
        searchedSkills={searchedSkills}
        skillCategories={initialSkillCategories}
        isDesktop={isDesktop}
        navigateToInput={navigateToInput}
        navigateToView={navigateToView}
        onSearch={handleSearch}
      />
    </div>
  );
}
