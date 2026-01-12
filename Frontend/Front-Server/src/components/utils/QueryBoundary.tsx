// Frontend/Front-Server/src/components/common/QueryBoundary.tsx
import React, { ReactNode, useEffect } from 'react';
import { AlertTriangle } from 'lucide-react';
import { LoadingSpinner } from '@/components/ui/LoadingSpinner';

interface QueryBoundaryProps {
  loading?: boolean;
  error?: Error | null;
  children: React.ReactNode | (() => React.ReactNode);
  loadingComponent?: ReactNode;
  errorComponent?: ReactNode;
}

const DefaultLoadingComponent = () => (
  <div className="flex justify-center items-center h-64">
    <LoadingSpinner message="데이터 로딩 중..." />
  </div>
);

const DefaultErrorComponent = ({ error }: { error: Error }) => {
  useEffect(() => {
    // AbortError는 일반적으로 다른 요청에 의해 취소된 경우이므로,
    // 사용자에게 심각한 오류로 보이지 않도록 콘솔에만 기록합니다.
    if (error.name !== 'AbortError') {
      console.error("QueryBoundary에서 에러 감지:", error);
    }
  }, [error]);

  // AbortError는 UI에 오류를 표시하지 않고 아무것도 렌더링하지 않습니다.
  // 이는 요청 취소가 정상적인 작업 흐름의 일부일 수 있기 때문입니다.
  if (error.name === 'AbortError') {
    return null;
  }

  return (
    <div className="flex flex-col justify-center items-center h-64 bg-red-900/20 rounded-lg gap-4">
      <AlertTriangle className="w-12 h-12 text-red-400" />
      <p className="text-red-400 text-lg">데이터를 불러오는 중 오류가 발생했습니다.</p>
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

  // 이제 children prop은 렌더 함수가 될 수 있습니다.
  if (typeof children === 'function') {
    return <>{children()}</>;
  }

  return <>{children}</>;
}
