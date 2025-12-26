// src/components/ThemeManager.tsx
'use client';

import { useEffect } from 'react';
import { useAppSelector } from '../../services/state/hooks';

/**
 * @file ThemeManager.tsx
 * @description Redux 스토어의 테마 상태를 감지하여 <html> 태그에 'dark' 클래스를 적용/제거합니다.
 *              이 컴포넌트는 UI를 렌더링하지 않습니다.
 *              운영체제: Windows
 */
export const ThemeManager: React.FC = () => {
  const theme = useAppSelector((state) => state.ui.theme);

  useEffect(() => {
    const root = document.documentElement;
    if (theme === 'dark') {
      root.classList.add('dark');
    } else {
      root.classList.remove('dark');
    }
  }, [theme]);

  return null; // 이 컴포넌트는 UI를 렌더링하지 않음
};
