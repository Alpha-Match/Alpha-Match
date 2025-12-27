import React from 'react';
import { InputPanelHeader } from './InputPanelHeader';
import { ExperienceSelector } from './ExperienceSelector';
import { SkillSelector } from './SkillSelector';
import { SearchButton } from './SearchButton';

interface InputPanelProps {
  onSearch: () => void;
  isLoading: boolean;
}

export const InputPanel: React.FC<InputPanelProps> = ({
  onSearch,
  isLoading,
}) => {
  return (
    <div className="h-full flex flex-col bg-white border-r border-slate-200 shadow-sm overflow-hidden">
      <InputPanelHeader />

      {/* Scrollable Form Content */}
      <div className="flex-1 overflow-y-auto p-6 space-y-8">
        {/* <ExperienceSelector /> */}
        <SkillSelector />
      </div>

      {/* Footer Action */}
      <div className="p-6 border-t border-slate-100 bg-white">
        <SearchButton onSearch={onSearch} isLoading={isLoading} />
      </div>
    </div>
  );
};
