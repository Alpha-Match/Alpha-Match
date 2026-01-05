import { useState, useEffect } from 'react';

/**
 * 컴포넌트가 클라이언트에서 성공적으로 하이드레이션되었는지 여부를 추적하는 커스텀 훅입니다.
 * 서버 렌더링과 클라이언트의 첫 렌더링 간의 불일치로 인한 하이드레이션 오류를 방지하는 데 사용됩니다.
 * 
 * @returns {boolean} 컴포넌트가 하이드레이션되었으면 `true`를, 그렇지 않으면 `false`를 반환합니다.
 */
export const useHydrated = () => {
  const [isHydrated, setIsHydrated] = useState(false);

  useEffect(() => {
    // useEffect는 클라이언트에서만, 그리고 마운트(하이드레이션) 이후에 실행됩니다.
    setIsHydrated(true);
  }, []);

  return isHydrated;
};
