// src/components/input-panel/InputPanelHeader.tsx
import React from 'react';
import { useAppSelector } from '../../services/state/hooks';
import { UserMode } from '../../types';
import { Search, Briefcase } from 'lucide-react';
import { CANDIDATE_THEME_COLORS, RECRUITER_THEME_COLORS } from '../../constants';

export const InputPanelHeader: React.FC = () => {
  const mode = useAppSelector((state) => state.ui.userMode);
  const isCandidate = mode === UserMode.CANDIDATE;

  return (
    <div className="p-6 pb-4 border-b border-border/30">
      <h2 
        className="text-xl font-bold flex items-center gap-2 text-primary"
      >
        {isCandidate ? <Briefcase className="w-6 h-6" /> : <Search className="w-6 h-6" />}
        {isCandidate ? "Configure Job Seeker Profile" : "Define Ideal Candidate Profile"}
      </h2>
      <p className="text-text-secondary text-sm mt-1">
        {isCandidate 
          ? "Select skills to find matching job positions." 
          : "Select required skills to find best-fit candidate profiles."}
      </p>
    </div>
  );
};
