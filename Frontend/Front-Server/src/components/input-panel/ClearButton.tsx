// Frontend/Front-Server/src/components/input-panel/ClearButton.tsx
import React from 'react';
import { useAppDispatch, useAppSelector } from '../../services/state/hooks';
import { resetSearch } from '../../services/state/features/search/searchSlice';
import { resetView } from '../../services/state/features/ui/uiSlice';
import { X } from 'lucide-react';
import chroma from 'chroma-js';
import { CANDIDATE_THEME_COLORS, RECRUITER_THEME_COLORS } from '../../constants';
import { UserMode } from '../../types';

export const ClearButton: React.FC = () => {
  const dispatch = useAppDispatch();
  const userMode = useAppSelector((state) => state.ui.userMode);

  const themeColors = userMode === UserMode.CANDIDATE ? CANDIDATE_THEME_COLORS : RECRUITER_THEME_COLORS;
  const primaryColor = themeColors[0];

  const shadowColor = chroma(primaryColor).alpha(0.3).hex();

  const buttonStyle = {
    background: '#ef4444', // Red color for clear
    boxShadow: `0 10px 15px -3px ${shadowColor}, 0 4px 6px -4px ${shadowColor}`,
  };

  const handleClear = () => {
    dispatch(resetSearch());
    dispatch(resetView()); // Go back to dashboard after clearing
  };

  return (
    <button
      onClick={handleClear}
      className="w-full py-3 px-4 rounded-xl text-white font-bold text-lg shadow-lg transform transition hover:-translate-y-0.5 active:translate-y-0 disabled:opacity-50 disabled:cursor-not-allowed flex items-center justify-center gap-2"
      style={buttonStyle}
    >
      Clear All
      <X className="w-5 h-5" />
    </button>
  );
};
