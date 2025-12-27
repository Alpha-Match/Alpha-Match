// Frontend/Front-Server/src/components/common/ClearButton.tsx
import React from 'react';
import { useAppDispatch, useAppSelector } from '../../services/state/hooks';
import { resetSearch } from '../../services/state/features/search/searchSlice';
import { X } from 'lucide-react';
import chroma from 'chroma-js';
import { CANDIDATE_THEME_COLORS, RECRUITER_THEME_COLORS } from '../../constants';
import { UserMode } from '../../types';

interface ClearButtonProps {
  onClear?: () => void; // Optional callback for external handling
  label?: string; // Optional label for the button
}

export const ClearButton: React.FC<ClearButtonProps> = ({ onClear, label = "Clear" }) => {
  const dispatch = useAppDispatch();
  const userMode = useAppSelector((state) => state.ui.userMode);

  const themeColors = userMode === UserMode.CANDIDATE ? CANDIDATE_THEME_COLORS : RECRUITER_THEME_COLORS;
  const primaryColor = themeColors[0];

  const shadowColor = chroma(primaryColor).alpha(0.3).hex(); // This shadow might be too much for a small inline button

  const handleClear = () => {
    dispatch(resetSearch(userMode)); // Clear only current mode's search state
    onClear && onClear(); // Call optional external handler
  };

  return (
    <button
      onClick={handleClear}
      className="inline-flex items-center text-sm font-medium text-slate-500 hover:text-slate-700 focus:outline-none focus:ring-1 focus:ring-slate-300 rounded-md px-2 py-1 transition-colors duration-150"
    >
      {label}
      <X className="w-4 h-4 ml-1" />
    </button>
  );
};
