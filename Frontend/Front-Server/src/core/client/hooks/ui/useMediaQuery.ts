'use client';

import { useState, useEffect } from 'react';

/**
 * A custom hook that tracks the state of a CSS media query.
 * @param query The media query string to match (e.g., '(min-width: 1024px)').
 * @returns `true` if the query matches, `false` otherwise.
 */
export const useMediaQuery = (query: string): boolean => {
  const [matches, setMatches] = useState<boolean>(false);

  useEffect(() => {
    // Check on component mount
    const mediaQueryList = window.matchMedia(query);
    setMatches(mediaQueryList.matches);

    // Create a listener function
    const listener = (event: MediaQueryListEvent) => {
      setMatches(event.matches);
    };

    // Add the listener
    mediaQueryList.addEventListener('change', listener);

    // Cleanup function to remove the listener
    return () => {
      mediaQueryList.removeEventListener('change', listener);
    };
  }, [query]); // Re-run effect if query changes

  return matches;
};
