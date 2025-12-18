'use client';

import { ApolloProvider } from '@apollo/client/react';
import { Provider as ReduxProvider } from 'react-redux';
import { store } from '../store/store';
import { makeClient } from '../lib/apollo-client';
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
