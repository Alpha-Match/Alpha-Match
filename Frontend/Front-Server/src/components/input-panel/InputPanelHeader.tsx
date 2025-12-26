// src/components/input-panel/InputPanelHeader.tsx
import React from 'react';
import { useAppSelector } from '../../services/state/hooks';
import { UserMode } from '../../types';
import { Search, Briefcase } from 'lucide-react';
import { CANDIDATE_THEME_COLORS, RECRUITER_THEME_COLORS } from '../../constants';
import chroma from 'chroma-js';

export const InputPanelHeader: React.FC = () => {
  const mode = useAppSelector((state) => state.ui.userMode);
  const isCandidate = mode === UserMode.CANDIDATE;

  const themeColors = isCandidate ? CANDIDATE_THEME_COLORS : RECRUITER_THEME_COLORS;
  const primaryColor = themeColors[0];
  const backgroundColor = chroma(primaryColor).alpha(0.1).css();

  return (
    <div 
      className="p-6 border-b border-slate-100"
      style={{ backgroundColor }}
    >
      <h2 
        className="text-xl font-bold flex items-center gap-2"
        style={{ color: primaryColor }}
      >
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
