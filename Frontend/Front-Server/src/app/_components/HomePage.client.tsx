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
import { Pickaxe, Search } from 'lucide-react';

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
        <main className="flex-1 flex flex-col overflow-hidden p-6">
            <DefaultDashboard userMode={userMode} activeColor={activeColor} />
            <div className="absolute bottom-10 right-10">
                <button
                    onClick={navigateToInput}
                    className="px-6 py-4 bg-primary text-white rounded-full shadow-lg flex items-center gap-2 transform hover:-translate-y-1 transition-transform"
                >
                    <Search size={20} />
                    검색 시작하기
                </button>
            </div>
        </main>
      );
    }

    // 3-column layout for input, results, and detail views
    return (
      <main className="flex-1 flex overflow-hidden">
        <div className="w-[380px] flex-shrink-0 h-full overflow-y-auto custom-scrollbar p-6 bg-panel-sidebar border-r border-border/30">
          <InputPanel
            onSearch={handleSearch}
            isLoading={isPending && pageViewMode === 'results'}
          />
        </div>
        <div className="w-[450px] flex-shrink-0 h-full overflow-y-auto custom-scrollbar p-6 border-r border-border/30">
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
        </div>
        <div className="flex-1 h-full overflow-y-auto custom-scrollbar p-6">
          {selectedMatchId ? (
            <MatchDetailPanel
              matchId={selectedMatchId}
              userMode={userMode}
              onBack={goBack}
              activeColor={activeColor}
              searchedSkills={searchedSkills}
            />
          ) : (
            <div className="h-full flex flex-col items-center justify-center text-center text-text-tertiary">
              <Pickaxe size={48} className="mb-4" />
              <h3 className="text-xl font-semibold">정보를 확인하세요</h3>
              <p>좌측 목록에서 항목을 선택하여 상세 정보를 볼 수 있습니다.</p>
            </div>
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
