// src/components/InputPanelHeader.tsx
import React from 'react';
import { useAppSelector } from '../../store/hooks';
import { UserMode } from '../../types';
import { Search, Briefcase } from 'lucide-react';

export const InputPanelHeader: React.FC = () => {
  const mode = useAppSelector((state) => state.search.activeTab);
  const isCandidate = mode === UserMode.CANDIDATE;

  return (
    <div className={`p-6 border-b border-slate-100 ${isCandidate ? 'bg-blue-50' : 'bg-purple-50'}`}>
      <h2 className={`text-xl font-bold flex items-center gap-2 ${isCandidate ? 'text-blue-700' : 'text-purple-700'}`}>
        {isCandidate ? <Briefcase className="w-6 h-6" /> : <Search className="w-6 h-6" />}
        {isCandidate ? "Configure Job Seeker Profile" : "Define Ideal Candidate Profile"}
      </h2>
      <p className="text-slate-500 text-sm mt-1">
        {isCandidate 
          ? "Select skills to find matching job positions." 
          : "Select required skills to find best-fit candidate profiles."}
      </p>
    </div>
  );
};
