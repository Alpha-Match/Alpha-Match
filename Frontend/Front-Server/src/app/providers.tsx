'use client';

import { ApolloProvider } from '@apollo/client/react';
import { Provider as ReduxProvider } from 'react-redux';
import { makeStore } from '../services/state/store';
import { makeClient, setApolloStore } from '../services/api/apollo-client';
import { showNotification } from '../services/state/features/notification/notificationSlice';
import { useState, useEffect, useRef } from 'react';
import { PersistGate } from 'redux-persist/integration/react';
import { Persistor, persistStore } from 'redux-persist';

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