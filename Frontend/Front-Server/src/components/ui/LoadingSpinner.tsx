import React from 'react';
import {ClipLoader} from 'react-spinners';

interface LoadingSpinnerProps {
  size?: number;
  message?: string;
  color?: string; // 커스텀 색상 (선택적)
}

export const LoadingSpinner: React.FC<LoadingSpinnerProps> = ({ size = 40, message, color }) => {
  // color prop이 전달되면 사용하고, 아니면 테마의 primary color 사용
  const spinnerColor = color || 'rgb(var(--color-primary))';

  return (
    <div className="flex flex-col justify-center items-center h-full w-full gap-4 p-8">
      <ClipLoader color={spinnerColor} size={size} speedMultiplier={0.8} />
      {message && <p className="text-lg text-text-secondary mt-2">{message}</p>}
    </div>
  );
};
