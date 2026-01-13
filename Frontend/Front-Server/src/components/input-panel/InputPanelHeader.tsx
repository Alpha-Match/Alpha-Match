import React from 'react';
import {useAppSelector} from '@/core/client/services/state/hooks';
import {UserMode} from '@/types';
import {Briefcase, Search} from 'lucide-react';

export const InputPanelHeader: React.FC = () => {
  const mode = useAppSelector((state) => state.ui.userMode);
  const isCandidate = mode === UserMode.CANDIDATE;

  return (
    <div className="p-6 pb-4 border-b border-border/30">
      <h2 
        className="text-xl font-bold flex items-center gap-2 text-primary"
      >
        {isCandidate ? <Briefcase className="w-6 h-6" /> : <Search className="w-6 h-6" />}
        {isCandidate ? "구직자 프로필 설정" : "이상적인 후보자 프로필 정의"}
      </h2>
      <p className="text-text-secondary text-sm mt-1">
        {isCandidate 
          ? "매칭되는 공고를 찾기 위해 기술 스택을 선택하세요." 
          : "최적의 후보자 프로필을 찾기 위해 필요한 기술을 선택하세요."}
      </p>
    </div>
  );
};
