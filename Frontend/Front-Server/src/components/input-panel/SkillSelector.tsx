// src/components/input-panel/SkillSelector.tsx
import React, { useState } from 'react';
import { useAppSelector, useAppDispatch } from '../../services/state/hooks';
import { toggleSkill } from '../../services/state/features/search/searchSlice';
import { UserMode } from '../../types';
import { Code, Search } from 'lucide-react'; // Added Search icon
import { CANDIDATE_THEME_COLORS, RECRUITER_THEME_COLORS } from '../../constants';
import chroma from 'chroma-js';

export const SkillSelector: React.FC = () => {
  const dispatch = useAppDispatch();
  const mode = useAppSelector((state) => state.ui.userMode);
  const { 
    selectedSkills, 
    skillCategories, 
    skillsLoaded 
  } = useAppSelector((state) => state.search);

  const [searchTerm, setSearchTerm] = useState(''); // New state for search term

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

  // Filter skills based on search term
  const filteredSkills = skillCategories.filter(skill =>
    skill.toLowerCase().includes(searchTerm.toLowerCase())
  );

  return (
    <section>
      <label className="block text-sm font-semibold text-slate-700 mb-3 uppercase tracking-wider flex items-center gap-2">
        <Code className="w-4 h-4" />
        Tech Stack (Select Multiple)
      </label>
      {/* Search Input Field */}
      <div className="relative mb-4">
        <Search className="absolute left-3 top-1/2 -translate-y-1/2 w-4 h-4 text-slate-400" />
        <input
          type="text"
          placeholder="Search skills..."
          value={searchTerm}
          onChange={(e) => setSearchTerm(e.target.value)}
          className="w-full pl-9 pr-3 py-2 rounded-lg border border-slate-300 focus:outline-none focus:ring-1 focus:border-blue-400 text-slate-800"
        />
      </div>

      <div className="bg-slate-50 rounded-xl p-4 border border-slate-200 h-96 overflow-y-auto custom-scrollbar">
        <div className="grid grid-cols-1 gap-2">
          {!skillsLoaded && (
            <div className="text-center text-slate-500 text-sm">Loading skills...</div>
          )}
          {skillsLoaded && filteredSkills.length === 0 && searchTerm !== '' && (
            <div className="text-center text-slate-500 text-sm">No matching skills found.</div>
          )}
          {skillsLoaded && filteredSkills.map((skill, idx) => {
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
