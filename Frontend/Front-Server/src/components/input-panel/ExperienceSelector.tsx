// src/components/ExperienceSelector.tsx
import React from 'react';
import { useAppSelector, useAppDispatch } from '../../store/hooks';
import { setExperience } from '../../store/features/search/searchSlice';
import { EXPERIENCE_LEVELS } from '../../constants';
import { UserMode } from '../../types';
import { CheckCircle2 } from 'lucide-react';

export const ExperienceSelector: React.FC = () => {
  const dispatch = useAppDispatch();
  const { activeTab: mode, selectedExperience } = useAppSelector((state) => state.search);
  const isCandidate = mode === UserMode.CANDIDATE;

  const handleExperienceChange = (exp: string) => {
    dispatch(setExperience(exp));
  };

  return (
    <section>
      <label className="block text-sm font-semibold text-slate-700 mb-3 uppercase tracking-wider">
        {isCandidate ? "Job Seeker Experience" : "Candidate Required Experience"}
      </label>
      <div className="grid grid-cols-1 gap-3">
        {EXPERIENCE_LEVELS.map((exp) => (
          <button
            key={exp}
            onClick={() => handleExperienceChange(exp)}
            className={`flex items-center justify-between p-3 rounded-lg border transition-all duration-200 text-left ${
              selectedExperience === exp
                ? `border-${isCandidate ? 'blue' : 'purple'}-500 bg-${isCandidate ? 'blue' : 'purple'}-50 shadow-sm ring-1 ring-${isCandidate ? 'blue' : 'purple'}-500`
                : 'border-slate-200 hover:border-slate-300 hover:bg-slate-50'
            }`}
          >
            <span className={`text-sm font-medium ${selectedExperience === exp ? 'text-slate-900' : 'text-slate-600'}`}>
              {exp}
            </span>
            {selectedExperience === exp && (
              <CheckCircle2 className={`w-4 h-4 text-${isCandidate ? 'blue' : 'purple'}-600`} />
            )}
          </button>
        ))}
      </div>
    </section>
  );
};
