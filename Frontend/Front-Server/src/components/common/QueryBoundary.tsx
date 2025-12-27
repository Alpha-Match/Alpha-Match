// Frontend/Front-Server/src/components/common/QueryBoundary.tsx
import React, { ReactNode, useEffect } from 'react';
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

const DefaultErrorComponent = ({ error }: { error: ApolloError | Error }) => {
  useEffect(() => {
    console.error("QueryBoundary caught an error:", error);
  }, [error]);

  return (
    <div className="flex justify-center items-center h-64 bg-red-900/20 rounded-lg">
      <p className="text-red-400">데이터를 불러오는 중 오류가 발생했습니다.</p>
    </div>
  );
};

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

  // The children prop is now a render function
  if (typeof children === 'function') {
    return <>{children()}</>;
  }

  return <>{children}</>;
}
