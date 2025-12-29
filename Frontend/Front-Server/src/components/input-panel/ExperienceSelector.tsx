// src/components/input-panel/ExperienceSelector.tsx
import React from 'react';
import { useAppSelector, useAppDispatch } from '../../services/state/hooks';
import { setExperience } from '../../services/state/features/search/searchSlice';
import { UserMode } from '../../types';
import { EXPERIENCE_LEVELS } from '../../constants';
import { CheckCircle2 } from 'lucide-react';

export const ExperienceSelector: React.FC = () => {
  const dispatch = useAppDispatch();
  const mode = useAppSelector((state) => state.ui.userMode);
  const selectedExperience = useAppSelector((state) => state.search.selectedExperience);
  const isCandidate = mode === UserMode.CANDIDATE;

  const handleExperienceChange = (exp: string) => {
    dispatch(setExperience(exp));
  };

  return (
    <section className="bg-panel-main p-4 rounded-lg shadow-sm border border-border">
      <label className="block text-sm font-semibold text-text-secondary mb-3 uppercase tracking-wider">
        {isCandidate ? "구직자 경력" : "요구되는 후보자 경력"}
      </label>
      <div className="grid grid-cols-1 gap-3">
        {EXPERIENCE_LEVELS.map((exp) => {
          const isSelected = selectedExperience === exp;
          return (
            <button
              key={exp}
              onClick={() => handleExperienceChange(exp)}
              className={`flex items-center justify-between p-3 rounded-lg border transition-all duration-200 text-left ${
                isSelected 
                ? 'border-primary bg-primary/10 shadow-sm focus:ring-1 focus:ring-primary' 
                : 'border-border hover:border-border-light hover:bg-panel-2 focus:ring-1 focus:ring-ring'
              }`}
            >
              <span className={`text-sm font-medium ${isSelected ? 'text-primary' : 'text-text-secondary'}`}>
                {exp}
              </span>
              {isSelected && (
                <CheckCircle2 className="w-4 h-4 text-primary"/>
              )}
            </button>
          );
        })}
      </div>
    </section>
  );
};
