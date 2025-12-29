// Frontend/Front-Server/src/components/common/LoadingSpinner.tsx
import React from 'react';
import { ClipLoader } from 'react-spinners';

interface LoadingSpinnerProps {
  size?: number;
  message?: string;
}

export const LoadingSpinner: React.FC<LoadingSpinnerProps> = ({ size = 40, message }) => {
  // The primary color is now controlled by CSS variables based on the active theme
  const primaryColor = 'rgb(var(--color-primary))';

  return (
    <div className="flex flex-col justify-center items-center h-full w-full gap-4 p-8">
      <ClipLoader color={primaryColor} size={size} speedMultiplier={0.8} />
      {message && <p className="text-lg text-text-secondary mt-2">{message}</p>}
    </div>
  );
};
