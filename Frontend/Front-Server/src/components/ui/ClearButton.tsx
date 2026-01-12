import React from 'react';
import {useAppDispatch, useAppSelector} from '@/services/state/hooks';
import {resetSearch} from '@/services/state/features/search/searchSlice';
import {X} from 'lucide-react';

interface ClearButtonProps {
  onClear?: () => void;
  label?: string;
}

export const ClearButton: React.FC<ClearButtonProps> = ({ onClear, label = "Clear" }) => {
  const dispatch = useAppDispatch();
  const userMode = useAppSelector((state) => state.ui.userMode);

  const handleClear = () => {
    dispatch(resetSearch(userMode)); // 현재 모드의 검색 상태만 지움
    onClear && onClear(); // 외부 핸들러 호출(옵션)
  };

  return (
    <button
      onClick={handleClear}
      className="inline-flex items-center text-sm font-medium text-text-secondary hover:text-text-primary focus:outline-none focus:ring-1 focus:ring-ring rounded-md px-2 py-1 transition-colors duration-150"
    >
      {label}
      <X className="w-4 h-4 ml-1" />
    </button>
  );
};
