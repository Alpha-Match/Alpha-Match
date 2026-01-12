import { useRef, useEffect, useCallback } from 'react';

interface IntersectionObserverOptions extends IntersectionObserverInit {
  // 한 번만 실행할지 여부
  once?: boolean;
}

/**
 * Intersection Observer를 사용하여 요소의 가시성을 감지하는 커스텀 훅입니다.
 * 무한 스크롤, 이미지 지연 로딩 등에 재사용할 수 있습니다.
 *
 * @param onIntersect - 요소가 화면에 나타났을 때 호출될 콜백 함수
 * @param options - IntersectionObserver에 전달될 옵션 (threshold, rootMargin 등)
 * @returns 관찰할 요소에 연결할 ref 객체
 */
export const useIntersectionObserver = <T extends HTMLElement>(
  onIntersect: () => void,
  options?: IntersectionObserverOptions
) => {
  const sentinelRef = useRef<T | null>(null);
  const onIntersectCallback = useRef(onIntersect);

  // onIntersect 콜백이 변경되더라도 Observer를 재생성하지 않도록 ref에 저장
  useEffect(() => {
    onIntersectCallback.current = onIntersect;
  }, [onIntersect]);

  const internalCallback = useCallback((entries: IntersectionObserverEntry[], observer: IntersectionObserver) => {
    entries.forEach(entry => {
      if (entry.isIntersecting) {
        onIntersectCallback.current();
        // 한 번만 실행하는 옵션이 켜져 있으면, 교차 후 관찰을 중단
        if (options?.once) {
          observer.unobserve(entry.target);
        }
      }
    });
  }, [options?.once]);

  useEffect(() => {
    if (!sentinelRef.current) return;
    
    const observer = new IntersectionObserver(internalCallback, options);
    
    observer.observe(sentinelRef.current);

    return () => observer.disconnect();
  }, [internalCallback, options]);

  return sentinelRef;
};
