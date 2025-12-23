'use client';

import React, { useEffect } from 'react';
import { useAppSelector, useAppDispatch } from '../../store/hooks';
import { showNotification, hideNotification } from '../../store/features/notification/notificationSlice';
import { XCircle, CheckCircle, Info } from 'lucide-react';

const icons = {
  error: <XCircle className="w-6 h-6 text-red-500" />,
  success: <CheckCircle className="w-6 h-6 text-green-500" />,
  info: <Info className="w-6 h-6 text-blue-500" />,
};

// Define a type for the custom event detail that matches the action payload
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

    // Listen for the custom event dispatched from the Apollo error link
    document.addEventListener('show-notification', handleNotificationEvent as EventListener);

    // Cleanup the event listener on component unmount
    return () => {
      document.removeEventListener('show-notification', handleNotificationEvent as EventListener);
    };
  }, [dispatch]);

  useEffect(() => {
    if (open) {
      const timer = setTimeout(() => {
        dispatch(hideNotification());
      }, 5000); // Auto-hide after 5 seconds

      return () => clearTimeout(timer);
    }
  }, [open, dispatch]);

  if (!open) {
    return null;
  }

  return (
    <div 
      className="fixed bottom-5 right-5 bg-white shadow-lg rounded-lg p-4 max-w-md z-[100] border-l-4 transition-all duration-300 ease-in-out transform animate-slide-in-up"
      style={{ borderColor: type === 'error' ? '#ef4444' : type === 'success' ? '#22c55e' : '#3b82f6' }}
    >
      <div className="flex items-start">
        <div className="flex-shrink-0">
          {icons[type]}
        </div>
        <div className="ml-3 w-0 flex-1 pt-0.5">
          <p className="text-sm font-medium text-gray-900">{message}</p>
        </div>
        <div className="ml-4 flex-shrink-0 flex">
          <button
            onClick={() => dispatch(hideNotification())}
            className="bg-white rounded-md inline-flex text-gray-400 hover:text-gray-500 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-indigo-500"
          >
            <span className="sr-only">Close</span>
            <XCircle className="h-5 w-5" />
          </button>
        </div>
      </div>
    </div>
  );
};
