// Frontend/Front-Server/src/components/layout/MainContentPanel.tsx
/**
 * @file MainContentPanel.tsx
 * @description 현재 페이지 뷰 모드에 따라 메인 콘텐츠 영역을 렌더링합니다.
 *              반응형 레이아웃 지원: 넓은 화면에서는 2단 분할, 좁은 화면에서는 단일 뷰.
 * @version 2.1.0
 * @date 2026-01-11
 */
import React, {Suspense, useState} from 'react';
import {MatchItem, SkillCategory, UserMode} from '@/types';
import QueryBoundary from '@/components/utils/QueryBoundary';
import {SearchResultPanel}from '@/components/search/results';
import MainDashboard from '@/app/_components/MainDashboard';
import {MatchDetailPanel}from '@/components/search/detail';
import {LoadingSpinner}from '@/components/ui/LoadingSpinner';
import {SearchResultTabs, SearchResultTab}from '@/components/search/navigation'; // Added
import {SearchResultAnalysisPanel}from '@/components/search/analysis'; // Added
import { InputPanel } from '@/components/input-panel'; // Added for mobile layout
import { TabController } from '@/components/layout/TabController'; // Added for mobile layout
import { Search } from 'lucide-react'; // Added for the search button
import {SearchResultTabs, SearchResultTab} from '@/components/search/navigation'; // Added
import {SearchResultAnalysisPanel}from '@/components/search/analysis'; // Added

type PageViewMode = 'dashboard' | 'results' | 'detail' | 'input' | 'analysis'; // Updated PageViewMode

interface MainContentPanelProps {
  userMode: UserMode;
  pageViewMode: PageViewMode;
  matches: MatchItem[];
  selectedMatchId: string | null;
  loading: boolean;
  fetchingMore?: boolean;
  error: Error | null;
  activeColor: string;
  onMatchSelect: (match: MatchItem) => void;
  onBackToList: () => void;
  onBackToDashboard: () => void;
  loadMore?: () => void;
  hasMore?: boolean;
  searchedSkills?: string[];
  skillCategories?: SkillCategory[];
  isDesktop: boolean; // Added
  navigateToInput: () => void; // Added
  navigateToView: (view: PageViewMode) => void; // Added
}

export const MainContentPanel: React.FC<MainContentPanelProps> = ({
  userMode,
  pageViewMode,
  matches,
  selectedMatchId,
  loading,
  fetchingMore = false,
  error,
  activeColor,
  onMatchSelect,
  onBackToList,
  onBackToDashboard,
  loadMore,
  hasMore,
  searchedSkills = [],
  skillCategories = [],
}) => {
  const [activeSearchResultTab, setActiveSearchResultTab] = useState<SearchResultTab>('analysis'); // Added state

  if (pageViewMode === 'dashboard') {
    return (
      <QueryBoundary>
        <Suspense fallback={<LoadingSpinner message="대시보드 로딩 중..." />}>
          <MainDashboard key={userMode} activeColor={activeColor} userMode={userMode} />
        </Suspense>
      </QueryBoundary>
    );
  }

  if (isDesktop) {
    // Desktop layout (3 columns for search, 2 columns for dashboard/detail)
    if (pageViewMode === 'dashboard') {
      return (
        <main className="flex-1 flex flex-col overflow-y-auto custom-scrollbar">
          <MainDashboard userMode={userMode} activeColor={activeColor} />
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
    return (
      <main className="flex-1 flex overflow-hidden">
        {/* Column 1: Input Panel */}
        <div className="w-[380px] flex-shrink-0 h-full bg-panel-sidebar border-r border-border/30">
          <InputPanel
            onSearch={navigateToDashboard} // Changed onSearch to navigate to dashboard after search
            isLoading={loading && pageViewMode === 'results'}
          />
        </div>

        {/* Column 2: Search Result Analysis Panel */}
        <div className="w-[450px] flex-shrink-0 h-full overflow-y-auto custom-scrollbar p-6 border-r border-border/30">
          <SearchResultAnalysisPanel
            activeColor={activeColor}
            userMode={userMode}
            searchedSkills={searchedSkills}
            skillCategories={skillCategories}
          />
        </div>

        {/* Column 3: Search Result List or Detail Panel */}
        <div className="flex-1 h-full overflow-y-auto custom-scrollbar p-6">
          {pageViewMode === 'detail' && selectedMatchId ? (
            <MatchDetailPanel
              matchId={selectedMatchId}
              userMode={userMode}
              onBack={onBackToList}
              activeColor={activeColor}
              searchedSkills={searchedSkills}
            />
          ) : (
            <QueryBoundary loading={loading && matches.length === 0} error={error}>
              <SearchResultPanel
                matches={matches}
                onMatchSelect={onMatchSelect}
                onBackToDashboard={onBackToDashboard}
                activeColor={activeColor}
                userMode={userMode}
                loadMore={loadMore}
                hasMore={hasMore}
                loading={fetchingMore}
                selectedMatchId={selectedMatchId}
              />
            </QueryBoundary>
          )}
        </div>
      </main>
    );
  } else {
    // Mobile layout
    const renderContent = () => {
      switch (pageViewMode) {
        case 'dashboard':
          return (
            <>
              <MainDashboard userMode={userMode} activeColor={activeColor} />
              <div className="absolute bottom-10 right-6 z-30">
                <button
                  onClick={navigateToInput}
                  className="px-6 py-4 bg-primary text-white rounded-full shadow-lg flex items-center gap-2 transform hover:-translate-y-1 transition-transform"
                  aria-label="검색 시작하기"
                >
                  <Search size={20} />
                  검색 시작하기
                </button>
              </div>
            </>
          );
        case 'input':
          return <InputPanel onSearch={navigateToDashboard} isLoading={loading} />; // onSearch should probably navigate to results or dashboard
        case 'results':
          return (
            <QueryBoundary loading={loading && matches.length === 0} error={error}>
              <SearchResultPanel
                matches={matches}
                onMatchSelect={onMatchSelect}
                onBackToDashboard={onBackToDashboard}
                activeColor={activeColor}
                userMode={userMode}
                loadMore={loadMore}
                hasMore={hasMore}
                loading={fetchingMore}
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
              onBack={onBackToList}
              activeColor={activeColor}
              searchedSkills={searchedSkills}
            />
          );
        case 'analysis':
          return (
            <SearchResultAnalysisPanel
              activeColor={activeColor}
              userMode={userMode}
              searchedSkills={searchedSkills}
              skillCategories={skillCategories}
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
  }
};
