import React from 'react';
import {InputPanelHeader, SkillSelector, SearchButton} from '@/components/input-panel';

interface InputPanelProps {
  onSearch: () => void;
  isLoading: boolean;
}

export const InputPanel: React.FC<InputPanelProps> = ({
  onSearch,
  isLoading,
}) => {
  return (
    <div className="h-full flex flex-col bg-panel-sidebar rounded-lg shadow-lg overflow-hidden">
      <InputPanelHeader />

      {/* Scrollable Form Content - Contains individual selector panels */}
      <div className="flex-1 overflow-y-auto p-6 space-y-4 custom-scrollbar"> {/* Adjusted space-y */}
        <SkillSelector />
        {/* <ExperienceSelector /> */} {/* Commented out - experience filter not used */}
      </div>

      {/* Footer Action */}
      <div className="p-6 bg-panel-sidebar border-t border-border/30">
        <SearchButton onSearch={onSearch} isLoading={isLoading} />
      </div>
    </div>
  );
};
