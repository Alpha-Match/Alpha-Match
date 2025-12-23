// src/components/input-panel/ExperienceSelector.tsx
import React from 'react';
import { useAppSelector, useAppDispatch } from '../../store/hooks';
import { setExperience } from '../../store/features/search/searchSlice';
import { UserMode } from '../../types/appTypes';
import { EXPERIENCE_LEVELS, CANDIDATE_THEME_COLORS, RECRUITER_THEME_COLORS } from '../../constants/appConstants';
import { CheckCircle2 } from 'lucide-react';
import chroma from 'chroma-js';

export const ExperienceSelector: React.FC = () => {
  const dispatch = useAppDispatch();
  const mode = useAppSelector((state) => state.ui.userMode);
  const selectedExperience = useAppSelector((state) => state.search.selectedExperience);
  const isCandidate = mode === UserMode.CANDIDATE;

  const handleExperienceChange = (exp: string) => {
    dispatch(setExperience(exp));
  };

  const themeColors = isCandidate ? CANDIDATE_THEME_COLORS : RECRUITER_THEME_COLORS;
  const primaryColor = themeColors[0];
  const lightBgColor = chroma(primaryColor).alpha(0.1).css();

  const activeStyle = {
    borderColor: primaryColor,
    backgroundColor: lightBgColor,
    boxShadow: `0 0 0 1px ${primaryColor}`,
  };

  return (
    <section>
      <label className="block text-sm font-semibold text-slate-700 mb-3 uppercase tracking-wider">
        {isCandidate ? "Job Seeker Experience" : "Candidate Required Experience"}
      </label>
      <div className="grid grid-cols-1 gap-3">
        {EXPERIENCE_LEVELS.map((exp) => {
          const isSelected = selectedExperience === exp;
          return (
            <button
              key={exp}
              onClick={() => handleExperienceChange(exp)}
              className={`flex items-center justify-between p-3 rounded-lg border transition-all duration-200 text-left ${
                !isSelected && 'border-slate-200 hover:border-slate-300 hover:bg-slate-50'
              }`}
              style={isSelected ? activeStyle : {}}
            >
              <span className={`text-sm font-medium ${isSelected ? 'text-slate-900' : 'text-slate-600'}`}>
                {exp}
              </span>
              {isSelected && (
                <CheckCircle2 className="w-4 h-4" style={{ color: primaryColor }}/>
              )}
            </button>
          );
        })}
      </div>
    </section>
  );
};
