import { ApolloClient, InMemoryCache, HttpLink, from } from "@apollo/client";
import { onError } from "@apollo/client/link/error";
import { GraphQLError } from "graphql";

const GRAPHQL_ENDPOINT = process.env.NEXT_PUBLIC_GRAPHQL_ENDPOINT || "http://localhost:8080/graphql";

const httpLink = new HttpLink({ uri: GRAPHQL_ENDPOINT });

const errorLink = onError((errorResponse: any) => {
  // The 'any' cast is a pragmatic workaround for a persistent and environment-specific compile error.

  if (errorResponse.graphQLErrors) {
    errorResponse.graphQLErrors.forEach(({ message, path }: GraphQLError) => {
      console.error(`[GraphQL error]: Message: ${message}, Path: ${path}`);
      let userMessage = "An error occurred while fetching data.";
      if (message.includes("Access denied")) {
        userMessage = "You are not authorized to perform this action.";
      }
      document.dispatchEvent(new CustomEvent('show-notification', {
        detail: { message: userMessage, type: 'error' }
      }));
    });
  }

  // Check for networkError, which occurs on transport-level failures.
  if (errorResponse.networkError) {
    console.error(`[Network error]: ${errorResponse.networkError.message}`);
    document.dispatchEvent(new CustomEvent('show-notification', {
      detail: { message: 'Server connection failed. Please check your network.', type: 'error' }
    }));
  }
});

// Export a factory function that creates a new client instance
export const makeClient = () => {
    return new ApolloClient({
        link: from([errorLink, httpLink]),
        cache: new InMemoryCache(),
        ssrMode: typeof window === 'undefined', // Enable SSR mode on the server
    });
};

