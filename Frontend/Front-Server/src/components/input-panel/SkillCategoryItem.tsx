import React, {useEffect, useRef, useState} from 'react';
import {ChevronDown} from 'lucide-react';
import {useAppDispatch, useAppSelector} from '@/core/client/services/state/hooks';
import {toggleSkill} from '@/core/client/services/state/features/search/searchSlice';

interface SkillCategoryItemProps {
  category: {
    category: string;
    skills: string[];
  };
  isOpen: boolean;
  onToggle: (categoryName: string) => void;
  selectedSkills: string[];
  searchTerm: string; // Add searchTerm prop
}

export const SkillCategoryItem: React.FC<SkillCategoryItemProps> = ({
  category,
  isOpen,
  onToggle,
  selectedSkills,
  searchTerm, // Receive searchTerm
}) => {
  const dispatch = useAppDispatch();
  const mode = useAppSelector((state) => state.ui.userMode);
  const contentRef = useRef<HTMLDivElement>(null);
  const [dynamicHeight, setDynamicHeight] = useState('0px');

  useEffect(() => {
    if (isOpen && contentRef.current) {
      setDynamicHeight(`${contentRef.current.scrollHeight}px`);
    } else if (!isOpen) {
      setDynamicHeight('0px');
    }
    // searchTerm이 변경될 때도 높이를 다시 계산해야 할 수 있습니다 (필터링된 스킬 목록이 변경되므로)
    // 하지만 filteredCategories는 SkillSelector에서 계산되므로 여기서는 isOpen과 contentRef.current만 의존합니다.
    // filteredCategories가 이 컴포넌트의 props로 직접 전달되지 않으므로,
    // 이 useEffect는 filteredCategories 변경에 직접적으로 반응하지 않습니다.
    // SkillSelector에서 filteredCategories가 변경되면 전체 SkillCategoryItem이 리렌더링되어
    // isOpen과 contentRef.current가 업데이트되는 경우 이 useEffect가 다시 실행될 것입니다.
  }, [isOpen, contentRef.current]);

  const handleSkillToggle = (skill: string) => {
    console.log(`Toggling skill: ${skill} for mode: ${mode}`); // 디버깅 로그 추가
    dispatch(toggleSkill({ userMode: mode, skill }));
  };

  return (
    <div>
      <button
        onClick={() => onToggle(category.category)}
        className="flex items-center justify-between w-full px-2 py-3 hover:bg-panel-2 rounded-md transition-colors duration-150"
      >
        <h3 className="text-sm font-bold text-text-tertiary uppercase tracking-wider">
          {category.category}
        </h3>
        <ChevronDown className={`w-4 h-4 text-text-tertiary transition-transform duration-200 ${isOpen ? 'rotate-0' : '-rotate-90'}`} />
      </button>
      {/* 동적 높이와 opacity를 적용하여 부드러운 전환 구현 */}
      <div
        ref={contentRef}
        style={{ maxHeight: dynamicHeight, opacity: isOpen ? 1 : 0 }}
        className={`grid grid-cols-1 gap-1 overflow-hidden transition-all duration-300 ease-in-out`}
      >
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
  );
};
