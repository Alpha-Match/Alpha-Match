# Frontend - Apollo Client Configuration for Alpha-Match

This document details the Apollo Client setup and how it integrates with the Next.js environment, specifically focusing on data fetching strategies for client-side and server-side contexts.

## 1. Apollo Client Setup (`src/services/api/apollo-client.ts`)

The Apollo Client is configured to interact with the GraphQL API. It uses `createHttpLink` to establish the connection, with the URI determined by the `NEXT_PUBLIC_GRAPHQL_ENDPOINT` environment variable. This setup is primarily intended for **client-side (browser) data fetching**.

```typescript
import { ApolloClient, InMemoryCache, createHttpLink } from '@apollo/client';

const httpLink = createHttpLink({
  uri: process.env.NEXT_PUBLIC_GRAPHQL_ENDPOINT
});

export const client = new ApolloClient({
  link: httpLink,
  cache: new InMemoryCache()
});
```

### Key Considerations:
-   `process.env.NEXT_PUBLIC_GRAPHQL_ENDPOINT`: This environment variable is exposed to the client-side bundle and should point to the externally accessible GraphQL endpoint. For path-based proxying via Cloudflare Tunnel, its value will be `/graphql`.
-   **Client-Side Fetching**: Apollo Client (and its hooks like `useQuery`) should primarily be used within React Client Components for data fetching that occurs in the browser.

## 2. Server-Side Data Fetching (`lib/server/graphql.ts`)

For Server Components in Next.js, a dedicated utility (`fetchGraphQLServer`) is used for data fetching. This utility directly leverages Node.js's `fetch` API and uses a server-only environment variable (`INTERNAL_GRAPHQL_ENDPOINT`) to ensure requests are made to the local API server using a full, absolute URL.

This approach strictly separates client-side and server-side data fetching concerns, adhering to the "localhost는 서버에서만 쓰고, 클라이언트는 절대 localhost를 몰라야 한다" 원칙.

```typescript
// lib/server/graphql.ts
// This utility is for Server Components to fetch data from the API server.
// It uses INTERNAL_GRAPHQL_ENDPOINT which is a full URL,
// ensuring server-side fetches work correctly without rewrites.

export async function fetchGraphQLServer(query: string, variables?: Record<string, any>) {
  if (!process.env.INTERNAL_GRAPHQL_ENDPOINT) {
    throw new Error('INTERNAL_GRAPHQL_ENDPOINT is not defined. Ensure it is set in .env.local or environment variables.');
  }

  const response = await fetch(process.env.INTERNAL_GRAPHQL_ENDPOINT, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ query, variables }),
  });

  if (!response.ok) {
    const errorBody = await response.text();
    console.error(`Server-side GraphQL fetch failed with status ${response.status}: ${errorBody}`);
    throw new Error(`Server-side GraphQL fetch failed: ${response.statusText}`);
  }

  return response.json();
}
```

### Key Considerations:
-   `process.env.INTERNAL_GRAPHQL_ENDPOINT`: This environment variable is **server-only** and must contain the full, absolute URL of the API server (e.g., `http://localhost:8080/graphql`). It is not exposed to the client-side bundle.
-   **Server Component Usage**: Server Components should import and use `fetchGraphQLServer` for all GraphQL data fetching.
-   **No `rewrites` for Server Fetches**: As Server Components perform Node.js internal `fetch` calls, Next.js `rewrites` mechanism does not apply to them. Hence, the `INTERNAL_GRAPHQL_ENDPOINT` must be a complete URL.

## 3. Environment Variable Configuration (`.env.local`)

To support the strict separation of client-side and server-side fetching, two environment variables are defined:

```
# .env.local

# Server-only (used by Node.js for internal API calls)
INTERNAL_GRAPHQL_ENDPOINT=http://localhost:8080/graphql

# Client-public (used by browser for external API calls)
NEXT_PUBLIC_GRAPHQL_ENDPOINT=/graphql
```

## 4. Cloudflare Tunnel Configuration for External Access

For the `NEXT_PUBLIC_GRAPHQL_ENDPOINT=/graphql` to work correctly for external clients, your Cloudflare Tunnel must be configured to route requests properly. The client browser will send requests to `https://your-tunnel-domain.com/graphql`. Therefore, the tunnel must know how to forward these `/graphql` requests to your local API server (`http://localhost:8080`).

This requires using a `config.yml` file with your `cloudflared` tunnel.

### Example `config.yml` for path-based routing:

```yaml
# Replace <YOUR_TUNNEL_ID> with your actual tunnel ID from Cloudflare
# Replace <YOUR_CREDENTIALS_FILE_PATH> with the path to your tunnel credentials file
# Replace your-app-domain.com with your actual registered domain or a trycloudflare.com hostname

tunnel: <YOUR_TUNNEL_ID>
credentials-file: <YOUR_CREDENTIALS_FILE_PATH> 

ingress:
  - hostname: your-app-domain.com # The public domain/hostname for your application
    service: http://localhost:3000 # Your local Next.js frontend server
    path: /.* # Frontend handles all paths by default
  - hostname: your-app-domain.com # Same public domain/hostname
    service: http://localhost:8080 # Your local API server
    path: /graphql # Requests to /graphql will be routed to your API
  - service: http_status:404 # Catch-all for unhandled routes
```

**Steps to run Cloudflare Tunnel with `config.yml`:**

1.  **Save** the above content as `config.yml` (or similar name) in your `cloudflared` directory.
2.  **Stop** any currently running `cloudflared tunnel --url ...` commands.
3.  **Run** the tunnel using the configuration file:
    ```bash
    cloudflared tunnel run <YOUR_TUNNEL_ID>
    ```
    (You will need to create a tunnel ID and credentials if you haven't already done so via the Cloudflare dashboard).

---

### **Summary of Changes and Next Steps for You:**

1.  **Code Changes Made**:
    *   `Frontend/Front-Server/.env.local`:
        *   `NEXT_PUBLIC_GRAPHQL_ENDPOINT=/graphql`로 설정 (클라이언트용)
        *   `INTERNAL_GRAPHQL_ENDPOINT=http://localhost:8080/graphql` 추가 (서버용)
    *   `Frontend/Front-Server/next.config.mjs`: `rewrites` 설정 제거
    *   `Frontend/Front-Server/lib/server/graphql.ts`: `fetchGraphQLServer` 유틸리티 추가 (Server Component용)
2.  **Your Actions Required**:
    *   **Crucial: Configure Cloudflare Tunnel using a `config.yml`** as described in "4. Cloudflare Tunnel Configuration for External Access" above. This is the only way for external API calls to `https://your-tunnel-domain.com/graphql` to reach your API server.
    *   **Restart your Next.js development server.**
    *   **Test your application thoroughly**:
        *   Locally (access `http://localhost:3000`).
        *   Externally (access your Cloudflare Tunnel public domain).
        *   Verify that API calls work correctly from both client-side (browser console) and SSR (page load).

Please let me know if you encounter any further issues after these steps.