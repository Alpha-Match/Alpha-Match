'use client';

import { ApolloProvider } from '@apollo/client/react';
import { Provider as ReduxProvider } from 'react-redux';
import { store } from '../services/state/store';
import { makeClient, setApolloStore } from '../services/api/apollo-client';
import { showNotification } from '../services/state/features/notification/notificationSlice';
import { useState, useEffect } from 'react';

export default function Providers({
                                      children,
                                  }: {
    children: React.ReactNode;
}) {
    const [client] = useState(() => makeClient());

    useEffect(() => {
        // Redux store를 Apollo Client에 주입
        setApolloStore(store, showNotification);
    }, []);

    return (
        <ReduxProvider store={store}>
            <ApolloProvider client={client}>
                {children}
            </ApolloProvider>
        </ReduxProvider>
    );
}
