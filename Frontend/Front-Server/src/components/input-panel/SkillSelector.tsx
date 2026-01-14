import React, {useEffect, useState} from 'react';
import {useAppDispatch, useAppSelector} from '@/core/client/services/state/hooks';
import {resetSearch, toggleSkill} from '@/core/client/services/state/features/search/searchSlice';
import {Code, Search, X} from 'lucide-react';
import {ClearButton} from '@/components/ui/ClearButton';
import {SkillCategoryItem} from '@/components/input-panel'; // Import the new component

interface SkillCategory {
    skills: string[];
    category: string;
}


export const SkillSelector: React.FC = () => {
  const dispatch = useAppDispatch();
  const mode = useAppSelector((state) => state.ui.userMode);
  const { skillCategories, skillsLoaded } = useAppSelector((state) => state.search);
  const { selectedSkills } = useAppSelector((state) => state.search[mode]);

  const [searchTerm, setSearchTerm] = useState('');
  const [openCategories, setOpenCategories] = useState<Set<string>>(() => new Set(
    skillCategories.map((cat: {category : string}) => cat.category)
  ));

  useEffect(() => {
    if (skillCategories.length > 0) {
      setOpenCategories(prev => {
        const newSet = new Set(prev);
        skillCategories.forEach((cat: {category : string}) => newSet.add(cat.category));
        return newSet;
      });
    }
  }, [skillCategories]);

  const handleSkillToggle = (skill: string) => {
    dispatch(toggleSkill({ userMode: mode, skill }));
  };
  
  const handleClearSkills = () => {
    dispatch(resetSearch(mode));
    setSearchTerm('');
  };

  const handleCategoryToggle = (categoryName: string) => {
    setOpenCategories(prev => {
      const newSet = new Set(prev);
      if (newSet.has(categoryName)) {
        newSet.delete(categoryName);
      } else {
        newSet.add(categoryName);
      }
      return newSet;
    });
  };

  const filteredCategories = skillCategories
    .map((category: SkillCategory) => ({
      ...category,
      skills: (category.skills ?? []).filter(skill =>
        skill.toLowerCase().includes(searchTerm.toLowerCase())
      ),
    }))
    .filter((category: SkillCategory) => category.skills.length > 0 || category.category.toLowerCase().includes(searchTerm.toLowerCase()))
    .sort((a:SkillCategory, b:SkillCategory) => a.category.localeCompare(b.category));

  return (
    <section className="bg-panel-main p-4 rounded-lg shadow-sm border border-border space-y-3">
      <div className="flex items-center justify-between text-sm font-semibold text-text-secondary uppercase tracking-wider">
        <label className="flex items-center gap-2">
          <Code className="w-4 h-4" />
          기술 스택 (다중 선택)
        </label>
        <ClearButton onClear={handleClearSkills} label="초기화" />
      </div>
      
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

      <div className={`flex flex-wrap gap-2 overflow-hidden transition-all duration-300 ease-in-out py-3 border-b border-border/30 ${
        selectedSkills.length > 0 ? 'max-h-96 opacity-100' : 'max-h-0 opacity-0 p-0 border-transparent'
      }`}>
        {selectedSkills.map((skill:string) => (
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

      <div className="bg-transparent py-3">
        <div className="grid grid-cols-1 gap-1">
          {!skillsLoaded && (
            <div className="text-center text-text-tertiary text-sm py-4">기술 스택 로딩 중...</div>
          )}
          {skillsLoaded && filteredCategories.length === 0 && searchTerm !== '' && (
            <div className="text-center text-text-tertiary text-sm py-4">일치하는 기술 스택이 없습니다.</div>
          )}
          {skillsLoaded && filteredCategories.map((category:SkillCategory) => (
            <SkillCategoryItem
              key={category.category}
              category={category}
              isOpen={openCategories.has(category.category)}
              onToggle={handleCategoryToggle}
              selectedSkills={selectedSkills}
              searchTerm={searchTerm} // Pass searchTerm down
            />
          ))}
        </div>
      </div>
      <p className="text-xs text-text-tertiary mt-2 text-right">
        {selectedSkills.length}개 스킬 선택됨
      </p>
    </section>
  );
};

