'use client';

import { ApolloProvider } from '@apollo/client/react';
import { Provider as ReduxProvider } from 'react-redux';
import { store } from '../services/state/store';
import { makeClient } from '../services/api/apollo-client';
import { useState } from 'react';

export default function Providers({
                                      children,
                                  }: {
    children: React.ReactNode;
}) {
    const [client] = useState(() => makeClient());

    return (
        <ReduxProvider store={store}>
            <ApolloProvider client={client}>
                {children}
            </ApolloProvider>
        </ReduxProvider>
    );
}
