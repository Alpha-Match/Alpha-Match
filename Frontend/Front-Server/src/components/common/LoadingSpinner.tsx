// Frontend/Front-Server/src/components/common/LoadingSpinner.tsx
import React from 'react';
import { ClipLoader } from 'react-spinners';
import { useAppSelector } from '../../services/state/hooks';
import { UserMode } from '../../types';
import { CANDIDATE_THEME_COLORS, RECRUITER_THEME_COLORS } from '../../constants';

interface LoadingSpinnerProps {
  size?: number;
  message?: string;
}

export const LoadingSpinner: React.FC<LoadingSpinnerProps> = ({ size = 40, message }) => {
  const userMode = useAppSelector((state) => state.ui.userMode);
  const theme = useAppSelector((state) => state.ui.theme);

  const themeColors = userMode === UserMode.CANDIDATE ? CANDIDATE_THEME_COLORS : RECRUITER_THEME_COLORS;
  const activeColor = theme === 'dark' ? themeColors[1] : themeColors[0];

  return (
    <div className="flex flex-col justify-center items-center h-full w-full gap-4 p-8">
      <ClipLoader color={activeColor} size={size} speedMultiplier={0.8} />
      {message && <p className="text-lg text-gray-400 mt-2">{message}</p>}
    </div>
  );
};
