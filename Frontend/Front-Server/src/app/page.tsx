'use client';

import React, { useEffect, useTransition } from 'react';
import { useAppDispatch, useAppSelector } from '../services/state/hooks';
import { setSearchPerformed } from '../services/state/features/search/searchSlice';
import { setPageViewMode, setSelectedMatchId } from '../services/state/features/ui/uiSlice';
import { InputPanel } from '../components/input-panel';
import { Header } from '../components/layout/Header';
import { useSearchMatches } from '../hooks/useSearchMatches';
import { MainContentPanel } from '../components/layout/MainContentPanel';
import { MatchItem, UserMode } from '../types';
import { CANDIDATE_THEME_COLORS, RECRUITER_THEME_COLORS } from '../constants';

/**
 * @file page.tsx
 * @description 애플리케이션의 메인 페이지 컴포넌트
 *              검색 입력 패널과 콘텐츠 패널을 통합하고, 뷰 상태에 따라 렌더링을 조정합니다.
 * @version 1.1.0
 * @date 2025-12-29
 */
const HomePage: React.FC = () => {
  const dispatch = useAppDispatch();
  const [isPending, startTransition] = useTransition();

  const userMode = useAppSelector((state) => state.ui.userMode);
  const { pageViewMode, selectedMatchId } = useAppSelector((state) => state.ui[userMode]);
  const { selectedSkills, selectedExperience, isInitial } = useAppSelector((state) => state.search[userMode]);
  
  const { runSearch, loading, error, matches } = useSearchMatches();

  // userMode 변경 시 자동으로 이전 검색을 복원합니다.
  useEffect(() => {
    if (pageViewMode === 'results' && selectedSkills.length > 0 && !isInitial) {
      runSearch(userMode, selectedSkills, selectedExperience);
    }
  }, [userMode, pageViewMode, selectedSkills, selectedExperience, isInitial, runSearch]);

  const handleSearch = () => {
    startTransition(() => {
      dispatch(setSearchPerformed(userMode));
      runSearch(userMode, selectedSkills, selectedExperience);
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

        <div className="flex-1 h-full relative z-10 bg-background p-6 overflow-y-auto">
          <MainContentPanel
            userMode={userMode}
            pageViewMode={pageViewMode as "dashboard" | "results" | "detail"}
            matches={matches}
            selectedMatchId={selectedMatchId}
            loading={loading}
            error={error}
            activeColor={activeColor}
            onMatchSelect={handleMatchSelect}
            onBackToList={handleBackToList}
          />
        </div>
      </main>
    </div>
  );
};

export default HomePage;

