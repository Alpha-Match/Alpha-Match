// src/components/Header.tsx
'use client';

import React from 'react';
import { useAppSelector, useAppDispatch } from '../store/hooks';
import { setActiveTab } from '../store/features/search/searchSlice';
import { UserMode } from '../types';
import { Briefcase, UserSearch } from 'lucide-react';

export const Header: React.FC = () => {
  const dispatch = useAppDispatch();
  const activeTab = useAppSelector((state) => state.search.activeTab);

  const handleTabChange = (mode: UserMode) => {
    dispatch(setActiveTab(mode));
  };

  return (
    <header className="bg-slate-800 border-b border-slate-700 z-50 flex-none h-16 flex items-center justify-between px-6">
      <div className="flex items-center gap-2">
        <div className="w-8 h-8 bg-blue-600 rounded-lg flex items-center justify-center">
          <svg className="w-5 h-5 text-white" fill="none" viewBox="0 0 24 24" stroke="currentColor">
            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2.5} d="M13 10V3L4 14h7v7l9-11h-7z" />
          </svg>
        </div>
        <h1 className="text-xl font-black tracking-tight text-slate-100">
          Alpha<span className="text-blue-500">Match</span>
        </h1>
      </div>

      <div className="flex bg-slate-700 p-1 rounded-xl">
        <button
          onClick={() => handleTabChange(UserMode.CANDIDATE)}
          className={`flex items-center gap-2 px-6 py-2 rounded-lg text-sm font-bold transition-all ${
            activeTab === UserMode.CANDIDATE ? 'bg-slate-800 text-blue-400 shadow-md' : 'text-slate-400 hover:text-slate-200'
          }`}
        >
          <Briefcase className="w-4 h-4" />
          Job Seeker View
        </button>
        <button
          onClick={() => handleTabChange(UserMode.RECRUITER)}
          className={`flex items-center gap-2 px-6 py-2 rounded-lg text-sm font-bold transition-all ${
            activeTab === UserMode.RECRUITER ? 'bg-slate-800 text-purple-400 shadow-md' : 'text-slate-400 hover:text-slate-200'
          }`}
        >
          <UserSearch className="w-4 h-4" />
          Recruiter View
        </button>
      </div>
      <div className="w-32"></div>
    </header>
  );
};
