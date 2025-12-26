// src/components/ThemeToggle.tsx
'use client';

import React from 'react';
import { useAppSelector, useAppDispatch } from '../../services/state/hooks';
import { setTheme } from '../../services/state/features/ui/uiSlice';
import { Sun, Moon } from 'lucide-react';

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
      className="p-2 rounded-full text-slate-400 hover:text-slate-200 hover:bg-slate-700 transition-colors duration-200 focus:outline-none focus:ring-2 focus:ring-blue-500"
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
