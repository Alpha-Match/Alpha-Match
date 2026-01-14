'use client';

import { ApolloProvider } from '@apollo/client/react';
import { Provider as ReduxProvider } from 'react-redux';
import { makeStore } from '@/core/client/services/state/store';
import { makeClient, setApolloStore } from '@/core/client/services/api/apollo-client';
import { showNotification } from '@/core/client/services/state/features/notification/notificationSlice';
import { useState, useEffect, useRef } from 'react';
import { PersistGate } from 'redux-persist/integration/react';
import { Persistor, persistStore, REHYDRATE } from 'redux-persist';

export default function Providers({
  children,
}: {
  children: React.ReactNode;
}) {
  const [client] = useState(() => makeClient());
  const storeRef = useRef<ReturnType<typeof makeStore> | null>(null);
  const persistorRef = useRef<Persistor | null>(null);

  if (!storeRef.current) {
    storeRef.current = makeStore();
    persistorRef.current = persistStore(storeRef.current);
  }

  useEffect(() => {
    if (storeRef.current) {
      setApolloStore(storeRef.current, showNotification);
    }

    const handleStorageChange = (event: StorageEvent) => {
      // Check if the change is from redux-persist's root key
      if (event.key === 'root' && event.newValue) {
        try {
          // Parse the new state from localStorage
          const newPersistedState = JSON.parse(event.newValue);
          const currentStore = storeRef.current;
          
          if (currentStore) {
            // Manually rehydrate only 'search' and 'theme' slices
            const rehydratedSearch = newPersistedState.search;
            const rehydratedTheme = newPersistedState.theme;

            // Dispatch REHYDRATE action for relevant reducers.
            // redux-persist's REHYDRATE action expects the entire state,
            // so we construct a partial state to rehydrate.
            currentStore.dispatch({
              type: REHYDRATE,
              key: 'root',
              payload: { search: rehydratedSearch, theme: rehydratedTheme },
            });
            console.log('Redux-persist: Rehydrated search and theme slices from other tab.');
          }
        } catch (error) {
          console.error('Failed to rehydrate state from storage event:', error);
        }
      }
    };

    window.addEventListener('storage', handleStorageChange);
    return () => window.removeEventListener('storage', handleStorageChange);
  }, []);

  return (
    <ReduxProvider store={storeRef.current}>
      <PersistGate loading={null} persistor={persistorRef.current!}>
        <ApolloProvider client={client}>
          {children}
        </ApolloProvider>
      </PersistGate>
    </ReduxProvider>
  );
}