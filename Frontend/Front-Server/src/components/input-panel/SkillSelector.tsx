import React, { useState } from 'react';
import { useAppSelector, useAppDispatch } from '../../services/state/hooks';
import { toggleSkill, resetSearch } from '../../services/state/features/search/searchSlice';
import { Code, Search, X } from 'lucide-react';
import { ClearButton } from '../../components/common/ClearButton';

export const SkillSelector: React.FC = () => {
  const dispatch = useAppDispatch();
  const mode = useAppSelector((state) => state.ui.userMode);
  const { skillCategories, skillsLoaded } = useAppSelector((state) => state.search);
  const { selectedSkills } = useAppSelector((state) => state.search[mode]);

  console.log('[Debug] SkillSelector rendering. skillsLoaded:', skillsLoaded, 'skillCategories count:', skillCategories.length);

  const [searchTerm, setSearchTerm] = useState('');

  const handleSkillToggle = (skill: string) => {
    dispatch(toggleSkill({ userMode: mode, skill }));
  };
  
  const handleClearSkills = () => {
    dispatch(resetSearch(mode)); // 현재 모드의 스킬만 리셋
    setSearchTerm(''); // 로컬 검색어도 지움
  };

  // 검색어로 스킬 필터링
  const filteredCategories = skillCategories
    .map(category => ({
      ...category,
      skills: (category.skills ?? []).filter(skill =>
        skill.toLowerCase().includes(searchTerm.toLowerCase())
      ),
    }))
    .filter(category => category.skills.length > 0);

  return (
    <section className="bg-panel-main p-4 rounded-lg shadow-sm border border-border space-y-3">
      <div className="flex items-center justify-between text-sm font-semibold text-text-secondary uppercase tracking-wider">
        <label className="flex items-center gap-2">
          <Code className="w-4 h-4" />
          기술 스택 (다중 선택)
        </label>
        <ClearButton onClear={handleClearSkills} label="초기화" />
      </div>
      
      {/* 검색 입력 필드 */}
      <div className="relative pb-3 border-b border-border/30">
        <Search className="absolute left-0 top-1/2 -translate-y-1/2 w-4 h-4 text-text-tertiary" />
        <input
          type="text"
          placeholder="기술 스택 검색..."
          value={searchTerm}
          onChange={(e) => setSearchTerm(e.target.value)}
          className="w-full pl-7 pr-3 py-2 bg-transparent focus:outline-none focus:ring-2 focus:ring-primary text-text-primary"
        />
      </div>

      {/* 선택된 기술 스택 표시 */}
      <div className={`flex flex-wrap gap-2 overflow-hidden transition-all duration-300 ease-in-out py-3 border-b border-border/30 ${
        selectedSkills.length > 0 ? 'max-h-96 opacity-100' : 'max-h-0 opacity-0 p-0 border-transparent'
      }`}>
        {selectedSkills.map(skill => (
          <span
            key={skill}
            className="inline-flex items-center bg-primary/10 text-primary text-xs font-medium px-2.5 py-1 rounded-full border border-primary-light"
          >
              {skill}
              <button
                onClick={() => handleSkillToggle(skill)}
                className="ml-1 -mr-1 flex items-center justify-center p-0.5 rounded-full hover:bg-primary/20 transition-colors"
                aria-label={`Remove ${skill}`}
              >
                <X className="w-3 h-3 text-primary" />
              </button>
            </span>
          ))}
        </div>

      <div className="bg-transparent h-96 overflow-y-auto custom-scrollbar py-3">
        <div className="grid grid-cols-1 gap-1">
          {!skillsLoaded && (
            <div className="text-center text-text-tertiary text-sm py-4">기술 스택 로딩 중...</div>
          )}
          {skillsLoaded && filteredCategories.length === 0 && searchTerm !== '' && (
            <div className="text-center text-text-tertiary text-sm py-4">일치하는 기술 스택이 없습니다.</div>
          )}
          {skillsLoaded && filteredCategories.map((category) => (
            <div key={category.category}>
              <h3 className="text-xs font-bold text-text-tertiary uppercase tracking-wider px-2 py-3">
                {category.category}
              </h3>
              <div className="grid grid-cols-1 gap-1">
                {category.skills.map((skill, idx) => {
                  const isSelected = selectedSkills.includes(skill);
                  return (
                    <button
                      key={`${skill}-${idx}`}
                      onClick={() => handleSkillToggle(skill)}
                      className={`group flex items-center w-full p-2 rounded-md transition-colors duration-150 ${
                        isSelected ? 'bg-primary/10 border border-primary' : 'border border-transparent hover:bg-panel-main'
                      }`}
                    >
                      <div 
                        className={`w-5 h-5 rounded border-2 flex items-center justify-center mr-3 transition-colors ${
                          isSelected ? 'bg-primary border-primary' : 'border-text-tertiary group-hover:border-primary'
                        }`}
                      >
                        {isSelected && <svg className="w-3.5 h-3.5 text-white" fill="none" viewBox="0 0 24 24" stroke="currentColor"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth={3} d="M5 13l4 4L19 7" /></svg>}
                      </div>
                      <span className={`text-sm ${isSelected ? 'font-semibold text-primary' : 'text-text-secondary group-hover:text-text-primary'}`}>
                        {skill}
                      </span>
                    </button>
                  );
                })}
              </div>
            </div>
          ))}
        </div>
      </div>
      <p className="text-xs text-text-tertiary mt-2 text-right">
        {selectedSkills.length}개 스킬 선택됨
      </p>
    </section>
  );
};

