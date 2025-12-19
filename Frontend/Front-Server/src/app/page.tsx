'use client';

import React, { useEffect } from 'react';
import { useAppSelector, useAppDispatch } from '../store/hooks';
import { setSearchPerformed } from '../store/features/search/searchSlice';
import { InputPanel } from '../components/input-panel';
import { VisualizationPanel } from '../components/VisualizationPanel';
import { Header } from '../components/Header';
import { useSearchMatches } from '../hooks/useSearchMatches';

const HomePage: React.FC = () => {
  const dispatch = useAppDispatch();
  const { activeTab, selectedSkills, selectedExperience } = useAppSelector((state) => state.search);
  const { runSearch, loading, error, matches, vectorData } = useSearchMatches();

  useEffect(() => {
    if (error) {
      console.error('GraphQL Query Error:', error);
    }
  }, [error]);

  const handleSearch = () => {
    dispatch(setSearchPerformed());
    runSearch(activeTab, selectedSkills, selectedExperience);
  };

  return (
    <div className="h-screen w-screen flex flex-col bg-slate-900 text-white overflow-hidden font-sans">
      <Header />

      <main className="flex-1 flex overflow-hidden">
        <div className="w-1/3 min-w-[350px] max-w-[450px] h-full z-20 bg-slate-800/50 border-r border-slate-700">
          <InputPanel
            onSearch={handleSearch}
            isLoading={loading}
          />
        </div>

        <div className="flex-1 h-full relative z-10 bg-slate-900">
          <VisualizationPanel matches={matches} vectorData={vectorData} />
        </div>
      </main>
    </div>
  );
};

export default HomePage;

