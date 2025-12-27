// Frontend/Front-Server/src/components/common/QueryBoundary.tsx
import React, { ReactNode } from 'react';
import { ApolloError } from '@apollo/client';

interface QueryBoundaryProps {
  loading: boolean;
  error?: ApolloError | Error;
  children: ReactNode;
  loadingComponent?: ReactNode;
  errorComponent?: ReactNode;
}

const DefaultLoadingComponent = () => (
  <div className="flex justify-center items-center h-64">
    <p className="text-white">Loading data...</p>
  </div>
);

const DefaultErrorComponent = ({ error }: { error: ApolloError | Error }) => (
  <div className="flex justify-center items-center h-64 bg-red-900/20 rounded-lg">
    <p className="text-red-400">Error: {error.message}</p>
  </div>
);

export default function QueryBoundary({
  loading,
  error,
  children,
  loadingComponent,
  errorComponent,
}: QueryBoundaryProps) {
  if (loading) {
    return loadingComponent || <DefaultLoadingComponent />;
  }

  if (error) {
    return errorComponent || <DefaultErrorComponent error={error} />;
  }

  return <>{children}</>;
}
