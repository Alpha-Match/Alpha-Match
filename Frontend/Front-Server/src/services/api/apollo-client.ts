import { ApolloClient, InMemoryCache, HttpLink, from } from "@apollo/client";
import { onError } from "@apollo/client/link/error";
import { CombinedGraphQLErrors, ServerError } from '@apollo/client/errors';

const GRAPHQL_ENDPOINT = process.env.NEXT_PUBLIC_GRAPHQL_ENDPOINT || "http://localhost:8080/graphql";

const httpLink = new HttpLink({ uri: GRAPHQL_ENDPOINT });

const errorLink = onError(({ error }) => {
  let userMessage = "An unexpected error occurred.";

  if (CombinedGraphQLErrors.is(error)) {
    // This handles GraphQL validation errors and execution errors.
    console.error('[GraphQL error]:', error.errors);
    // We can be more specific, but for now, a generic message is fine.
    userMessage = error.errors.map(e => e.message).join(' ');
  } else if (ServerError.is(error)) {
    // This handles server-side HTTP errors (e.g., 5xx).
    console.error(`[Server error]: ${error.message}`);
    userMessage = 'Server is not responding. Please try again later.';
  } else if (error) {
    // This catches generic network errors or others.
    console.error(`[Network error]: ${error.message}`);
    userMessage = 'Server connection failed. Please check your network.';
  }

  document.dispatchEvent(new CustomEvent('show-notification', {
    detail: { message: userMessage, type: 'error' }
  }));
});

// Export a factory function that creates a new client instance
export const makeClient = () => {
    return new ApolloClient({
        link: from([errorLink, httpLink]),
        cache: new InMemoryCache(),
        ssrMode: typeof window === 'undefined', // Enable SSR mode on the server
    });
};

