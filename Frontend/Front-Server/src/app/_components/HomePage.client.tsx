'use client';

import React, { useEffect, useTransition } from 'react';
import { useAppDispatch, useAppSelector } from '../../services/state/hooks';
import { setSearchPerformed, setSkillCategories, setDashboardData } from '../../services/state/features/search/searchSlice';
import { useAppNavigation } from '../../hooks/useAppNavigation';
import { useMediaQuery } from '../../hooks/useMediaQuery';
import { InputPanel } from '../../components/input-panel';
import { Header } from '../../components/layout/Header';
import { useSearchMatches } from '../../hooks/useSearchMatches';
import { UserMode, SkillCategory, DashboardCategory } from '../../types';
import { CANDIDATE_THEME_COLORS, RECRUITER_THEME_COLORS } from '../../constants';
import { TabController } from '@/components/layout/TabController';
import SearchResultPanel from '@/components/search/SearchResultPanel';
import MatchDetailPanel from '@/components/search/MatchDetailPanel';
import DefaultDashboard from '@/components/dashboard/DefaultDashboard';
import QueryBoundary from '@/components/common/QueryBoundary';
import { Pickaxe, Search, Home } from 'lucide-react';
import { SearchResultAnalysisPanel } from '@/components/search/SearchResultAnalysisPanel'; // Import the new analysis panel

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

  const renderDesktopLayout = () => {
    if (pageViewMode === 'dashboard') {
      return (
        <main className="flex-1 flex flex-col overflow-y-auto custom-scrollbar p-6">
            <DefaultDashboard userMode={userMode} activeColor={activeColor} />
            <div className="absolute bottom-10 right-10 z-30">
                <button
                    onClick={navigateToInput}
                    className="px-6 py-4 bg-primary text-white rounded-full shadow-lg flex items-center gap-2 transform hover:-translate-y-1 transition-transform"
                    aria-label="검색 시작하기"
                >
                    <Search size={20} />
                    검색 시작하기
                </button>
            </div>
        </main>
      );
    }

    // 3-column layout for input, analysis, and list/detail
    return (
      <main className="flex-1 flex overflow-hidden">
        {/* Column 1: Input Panel */}
        <div className="w-[380px] flex-shrink-0 h-full bg-panel-sidebar border-r border-border/30">
          <InputPanel
            onSearch={handleSearch}
            isLoading={isPending && pageViewMode === 'results'}
          />
        </div>

        {/* Column 2: Search Result Analysis Panel */}
        <div className="w-[450px] flex-shrink-0 h-full overflow-y-auto custom-scrollbar p-6 border-r border-border/30">
          <SearchResultAnalysisPanel
            activeColor={activeColor}
            userMode={userMode}
            searchedSkills={searchedSkills}
            skillCategories={initialSkillCategories}
          />
        </div>

        {/* Column 3: Search Result List or Detail Panel */}
        <div className="flex-1 h-full overflow-y-auto custom-scrollbar p-6">
          {pageViewMode === 'detail' && selectedMatchId ? (
            <MatchDetailPanel
              matchId={selectedMatchId}
              userMode={userMode}
              onBack={goBack}
              activeColor={activeColor}
              searchedSkills={searchedSkills}
            />
          ) : (
            <QueryBoundary loading={loading && matches.length === 0} error={error}>
              <SearchResultPanel
                matches={matches}
                onMatchSelect={navigateToDetail}
                activeColor={activeColor}
                userMode={userMode}
                loadMore={loadMore}
                hasMore={hasMore}
                loading={fetchingMore}
                searchedSkills={searchedSkills} // Not used internally now
                skillCategories={initialSkillCategories} // Not used internally now
                selectedMatchId={selectedMatchId}

              />
            </QueryBoundary>
          )}
        </div>
      </main>
    );
  };

  const renderMobileLayout = () => {
    const renderContent = () => {
      switch (pageViewMode) {
        case 'dashboard':
          return <DefaultDashboard userMode={userMode} activeColor={activeColor} />;
        case 'input':
          return <InputPanel onSearch={handleSearch} isLoading={isPending} />;
        case 'results':
          return (
            <QueryBoundary loading={loading && matches.length === 0} error={error}>
              <SearchResultPanel
                matches={matches}
                onMatchSelect={navigateToDetail}
                activeColor={activeColor}
                userMode={userMode}
                loadMore={loadMore}
                hasMore={hasMore}
                loading={fetchingMore}
                searchedSkills={searchedSkills}
                skillCategories={initialSkillCategories}
                selectedMatchId={selectedMatchId}
              />
            </QueryBoundary>
          );
        case 'detail':
          if (!selectedMatchId) return null;
          return (
            <MatchDetailPanel
              matchId={selectedMatchId}
              userMode={userMode}
              onBack={goBack}
              activeColor={activeColor}
              searchedSkills={searchedSkills}
            />
          );
        default:
          return null;
      }
    };

    return (
      <main className="flex-1 flex flex-col overflow-hidden">
        <TabController
          activeView={pageViewMode}
          onTabChange={navigateToView}
          userMode={userMode}
          detailAvailable={!!selectedMatchId}
        />
        <div className="flex-1 p-6 overflow-y-auto custom-scrollbar">
          {renderContent()}
        </div>
      </main>
    );
  };

  return (
    <div className="h-screen w-screen flex flex-col bg-background text-text-primary overflow-hidden font-sans">
      <Header />
      {isDesktop ? renderDesktopLayout() : renderMobileLayout()}
    </div>
  );
}
