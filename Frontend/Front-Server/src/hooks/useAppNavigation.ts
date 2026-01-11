import { useAppDispatch, useAppSelector } from '../services/state/hooks';
import { pushHistory, navigateBack } from '../services/state/features/ui/uiSlice';
import { MatchItem, UserMode } from '../types';

type PageViewMode = 'dashboard' | 'input' | 'results' | 'detail';

/**
 * 앱 내 페이지 네비게이션 로직을 관리하는 커스텀 훅입니다.
 * Redux uiSlice의 히스토리 상태와 상호작용하여 뷰 전환을 처리합니다.
 *
 * @returns 네비게이션 함수들과 현재 뷰 상태를 포함하는 객체
 */
export const useAppNavigation = () => {
  const dispatch = useAppDispatch();
  const userMode = useAppSelector((state) => state.ui.userMode);
  const { history, currentIndex } = useAppSelector((state) => state.ui[userMode]);

  const currentView = history[currentIndex] || { pageViewMode: 'dashboard', selectedMatchId: null };
  const { pageViewMode, selectedMatchId } = currentView;

  const navigateToDashboard = () => {
    dispatch(pushHistory({ userMode, view: { pageViewMode: 'dashboard', selectedMatchId: null } }));
  };

  const navigateToInput = () => {
    dispatch(pushHistory({ userMode, view: { pageViewMode: 'input', selectedMatchId: null } }));
  };

  const navigateToResults = () => {
    dispatch(pushHistory({ userMode, view: { pageViewMode: 'results', selectedMatchId: null } }));
  };

  const navigateToDetail = (match: MatchItem) => {
    dispatch(pushHistory({ userMode, view: { pageViewMode: 'detail', selectedMatchId: match.id } }));
  };

  const goBack = () => {
    dispatch(navigateBack({ userMode }));
  };
  
  const navigateToView = (view: PageViewMode) => {
    // When switching tabs on mobile, we generally don't want to keep the old matchId unless we are going to the detail view itself
    const newSelectedMatchId = view === 'detail' ? selectedMatchId : null;
    dispatch(pushHistory({ userMode, view: { pageViewMode: view, selectedMatchId: newSelectedMatchId } }));
  };

  return {
    userMode,
    pageViewMode,
    selectedMatchId,
    navigateToDashboard,
    navigateToInput,
    navigateToResults,
    navigateToDetail,
    goBack,
    navigateToView,
  };
};
