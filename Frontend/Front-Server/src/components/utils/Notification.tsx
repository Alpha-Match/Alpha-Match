'use client';

import React, { useEffect } from 'react';
import { useAppSelector, useAppDispatch } from '@/services/state/hooks';
import { showNotification, hideNotification } from '@/services/state/features/notification/notificationSlice';
import { XCircle, CheckCircle, Info } from 'lucide-react';

const icons = {
  error: <XCircle className="w-6 h-6 text-red-500" />,
  success: <CheckCircle className="w-6 h-6 text-green-500" />,
  info: <Info className="w-6 h-6 text-blue-500" />,
};

// 액션 페이로드와 일치하는 커스텀 이벤트 상세 타입을 정의
type NotificationEventDetail = {
  message: string;
  type: 'error' | 'success' | 'info';
};

export const Notification = () => {
  const dispatch = useAppDispatch();
  const { open, message, type } = useAppSelector((state) => state.notification);

  useEffect(() => {
    const handleNotificationEvent = (event: CustomEvent<NotificationEventDetail>) => {
      dispatch(showNotification(event.detail));
    };

    // Apollo 에러 링크에서 디스패치된 커스텀 이벤트를 리스닝
    document.addEventListener('show-notification', handleNotificationEvent as EventListener);

    // 컴포넌트 언마운트 시 이벤트 리스너 정리
    return () => {
      document.removeEventListener('show-notification', handleNotificationEvent as EventListener);
    };
  }, [dispatch]);

  useEffect(() => {
    if (open) {
      const timer = setTimeout(() => {
        dispatch(hideNotification());
      }, 5000); // 5초 후 자동 숨김

      return () => clearTimeout(timer);
    }
  }, [open, dispatch]);

  if (!open) {
    return null;
  }

  const borderColorClass = {
    error: 'border-red-500',
    success: 'border-green-500',
    info: 'border-blue-500',
  }[type];

  return (
    <div 
      className={`fixed bottom-5 right-5 bg-panel-main shadow-lg rounded-lg p-4 max-w-sm min-w-[320px] z-[100] border-l-4 transition-all duration-300 ease-in-out transform animate-slide-in-up ${borderColorClass}`}
    >
      <div className="flex items-start">
        <div className="flex-shrink-0">
          {icons[type]}
        </div>
        <div className="ml-3 w-full flex-1 pt-0.5">
          <p className="text-sm font-medium text-text-primary break-words">{message}</p>
        </div>
        <div className="ml-4 flex-shrink-0 flex">
          <button
            onClick={() => dispatch(hideNotification())}
            className="bg-transparent rounded-md inline-flex text-text-tertiary hover:text-text-primary focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-primary"
          >
            <span className="sr-only">Close</span>
            <XCircle className="h-5 w-5" />
          </button>
        </div>
      </div>
    </div>
  );
};
