'use client';

import {useEffect} from 'react';
import {useAppSelector} from '@/lib/client/services/state/hooks';
import {UserMode} from '@/types';

/**
 * @file ThemeManager.tsx
 * @description Redux 스토어의 테마 및 사용자 모드 상태를 감지하여 <html> 태그에 동적 클래스를 적용합니다.
 *              이 컴포넌트는 UI를 렌더링하지 않습니다.
 *              운영체제: Windows
 */
export const ThemeManager: React.FC = () => {
  const theme = useAppSelector((state) => state.ui.theme);
  const userMode = useAppSelector((state) => state.ui.userMode);

  useEffect(() => {
    const root = document.documentElement;
    
    // 다크/라이트 모드 클래스 관리
    if (theme === 'dark') {
      root.classList.add('dark');
    } else {
      root.classList.remove('dark');
    }

    // 사용자 모드(candidate/recruiter)에 따른 테마 클래스 관리
    if (userMode === UserMode.CANDIDATE) {
      root.classList.add('theme-candidate');
      root.classList.remove('theme-recruiter');
    } else {
      root.classList.add('theme-recruiter');
      root.classList.remove('theme-candidate');
    }

  }, [theme, userMode]);

  return null; // 이 컴포넌트는 UI를 렌더링하지 않음
};
