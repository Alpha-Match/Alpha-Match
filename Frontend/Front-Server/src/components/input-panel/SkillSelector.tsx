// src/components/input-panel/SkillSelector.tsx
import React from 'react';
import { useAppSelector, useAppDispatch } from '../../store/hooks';
import { toggleSkill } from '../../store/features/search/searchSlice';
import { UserMode } from '../../types/appTypes';
import { Code } from 'lucide-react';
import { CANDIDATE_THEME_COLORS, RECRUITER_THEME_COLORS } from '../../constants/appConstants';
import chroma from 'chroma-js';

export const SkillSelector: React.FC = () => {
  const dispatch = useAppDispatch();
  const mode = useAppSelector((state) => state.ui.userMode);
  const { 
    selectedSkills, 
    skillCategories, 
    skillsLoaded 
  } = useAppSelector((state) => state.search);

  const handleSkillToggle = (skill: string) => {
    dispatch(toggleSkill(skill));
  };
  
  const themeColors = mode === UserMode.CANDIDATE ? CANDIDATE_THEME_COLORS : RECRUITER_THEME_COLORS;
  const primaryColor = themeColors[0];
  const ringColor = chroma(primaryColor).alpha(0.4).hex();

  const activeButtonStyle = {
    boxShadow: `0 0 0 1px ${ringColor}`,
  };

  const activeCheckboxStyle = {
    backgroundColor: primaryColor,
    borderColor: primaryColor,
  };

  return (
    <section>
      <label className="block text-sm font-semibold text-slate-700 mb-3 uppercase tracking-wider flex items-center gap-2">
        <Code className="w-4 h-4" />
        Tech Stack (Select Multiple)
      </label>
      <div className="bg-slate-50 rounded-xl p-4 border border-slate-200 h-96 overflow-y-auto custom-scrollbar">
        <div className="grid grid-cols-1 gap-2">
          {!skillsLoaded && (
            <div className="text-center text-slate-500 text-sm">Loading skills...</div>
          )}
          {skillsLoaded && skillCategories.map((skill, idx) => {
            const isSelected = selectedSkills.includes(skill);
            return (
              <button
                key={`${skill}-${idx}`}
                onClick={() => handleSkillToggle(skill)}
                className={`group flex items-center p-2 rounded-md transition-all duration-150 ${
                  isSelected ? 'bg-white shadow-sm' : 'hover:bg-slate-200/50'
                }`}
                style={isSelected ? activeButtonStyle : {}}
              >
                <div 
                  className="w-5 h-5 rounded border flex items-center justify-center mr-3 transition-colors"
                  style={isSelected ? activeCheckboxStyle : {}}
                >
                  {isSelected && <svg className="w-3.5 h-3.5 text-white" fill="none" viewBox="0 0 24 24" stroke="currentColor"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth={3} d="M5 13l4 4L19 7" /></svg>}
                </div>
                <span className={`text-sm ${isSelected ? 'font-semibold text-slate-800' : 'text-slate-600'}`}>
                  {skill}
                </span>
              </button>
            );
          })}
        </div>
      </div>
      <p className="text-xs text-slate-400 mt-2 text-right">
        {selectedSkills.length} skills selected
      </p>
    </section>
  );
};
