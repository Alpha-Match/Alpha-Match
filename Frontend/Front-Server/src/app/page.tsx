'use client';

import React, { useEffect, useRef } from 'react';
import { useAppSelector, useAppDispatch } from '../services/state/hooks';
import { setSearchPerformed } from '../services/state/features/search/searchSlice';
import { setPageViewMode, setSelectedMatchId } from '../services/state/features/ui/uiSlice';
import { InputPanel } from '../components/input-panel';
import { Header } from '../components/layout/Header';
import { useSearchMatches } from '../hooks/useSearchMatches';
import SearchResultPanel from '../components/search/SearchResultPanel';
import DefaultDashboard from '../components/dashboard/DefaultDashboard';
import MatchDetailPanel from '../components/search/MatchDetailPanel';
import { UserMode, MatchItem } from '../types';
import { CANDIDATE_THEME_COLORS, RECRUITER_THEME_COLORS } from '../constants';

/**
 * @file page.tsx
 * @description 애플리케이션의 메인 페이지 컴포넌트
 *              검색 입력 패널과 콘텐츠 패널(대시보드, 검색 결과, 상세 보기)을 통합하고,
 *              뷰 상태에 따라 적절한 컴포넌트를 렌더링합니다.
 *              운영체제: Windows
 */
const HomePage: React.FC = () => {
  const dispatch = useAppDispatch();
  
  // Redux 스토어에서 상태 가져오기
  const { selectedSkills, selectedExperience } = useAppSelector((state) => state.search);
  const { userMode, viewResetCounter, pageViewMode, selectedMatchId } = useAppSelector((state) => state.ui);
  
  const { runSearch, loading, error, matches } = useSearchMatches();
  
  const isInitialMount = useRef(true);

  // 검색 오류 발생 시 처리
  useEffect(() => {
    if (error) {
      console.error('검색 오류:', error);
    }
  }, [error]);

  // Header에서 resetView 액션이 dispatch 될 때 뷰를 대시보드로 초기화
  useEffect(() => {
    if (isInitialMount.current) {
      isInitialMount.current = false;
    } else {
      // The reset logic is now handled inside the resetView action in uiSlice.
      // This effect only tracks the counter to re-affirm the view if needed, but the state is already reset.
    }
  }, [viewResetCounter]);

  /**
   * 검색 실행 핸들러
   */
  const handleSearch = () => {
    dispatch(setSearchPerformed());
    runSearch(userMode, selectedSkills, selectedExperience);
    dispatch(setPageViewMode('results'));
    dispatch(setSelectedMatchId(null));
  };

  /**
   * 결과 카드 클릭 핸들러 (상세 뷰로 전환)
   */
  const handleMatchSelect = (match: MatchItem) => {
    dispatch(setSelectedMatchId(match.id));
    dispatch(setPageViewMode('detail'));
  };

  /**
   * 상세 뷰에서 목록으로 돌아가기 핸들러
   */
  const handleBackToList = () => {
    dispatch(setSelectedMatchId(null));
    dispatch(setPageViewMode('results'));
  };
  
  const themeColors = userMode === UserMode.CANDIDATE ? CANDIDATE_THEME_COLORS : RECRUITER_THEME_COLORS;
  const activeColor = themeColors[0];

  /**
   * 메인 콘텐츠 패널을 렌더링하는 함수
   */
  const renderMainPanel = () => {
    switch (pageViewMode) {
      case 'detail':
        const selectedMatch = matches.find(m => m.id === selectedMatchId);
        return selectedMatch && <MatchDetailPanel match={selectedMatch} onBack={handleBackToList} />;
      
      case 'results':
        return (
          <SearchResultPanel
            matches={matches}
            loading={loading}
            error={error}
            onMatchSelect={handleMatchSelect}
            activeColor={activeColor}
          />
        );

      case 'dashboard':
      default:
        return <DefaultDashboard />;
    }
  }

  return (
    <div className="h-screen w-screen flex flex-col bg-gray-100 dark:bg-slate-900 text-white overflow-hidden font-sans">
      <Header />

      <main className="flex-1 flex overflow-hidden">
        {/* 검색 입력 패널 */}
        <div className="w-1/3 min-w-[350px] max-w-[450px] h-full z-20 bg-slate-800/50 border-r border-slate-700">
          <InputPanel
            onSearch={handleSearch}
            isLoading={loading && pageViewMode === 'results'}
          />
        </div>

        {/* 메인 콘텐츠 패널 (조건부 렌더링) */}
        <div className="flex-1 h-full relative z-10 bg-slate-900 p-6 overflow-y-auto">
          {renderMainPanel()}
        </div>
      </main>
    </div>
  );
};

export default HomePage;
