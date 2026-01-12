'use client';

import React from 'react';
import {useAppDispatch, useAppSelector} from '@/services/state/hooks';
import {setTheme} from '@/services/state/features/ui/uiSlice';
import {Moon, Sun} from 'lucide-react';

export const ThemeToggle: React.FC = () => {
  const dispatch = useAppDispatch();
  const currentTheme = useAppSelector((state) => state.ui.theme);

  const handleToggle = () => {
    const newTheme = currentTheme === 'dark' ? 'light' : 'dark';
    dispatch(setTheme(newTheme));
  };

  return (
    <button
      onClick={handleToggle}
      className="p-2 rounded-full text-text-tertiary hover:text-text-primary hover:bg-panel-2 transition-colors duration-200 focus:outline-none focus:ring-2 focus:ring-primary"
      aria-label={`Switch to ${currentTheme === 'dark' ? 'light' : 'dark'} mode`}
    >
      {currentTheme === 'dark' ? (
        <Sun className="w-5 h-5" />
      ) : (
        <Moon className="w-5 h-5" />
      )}
    </button>
  );
};
