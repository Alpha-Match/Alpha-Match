'use client';

import React, { useEffect } from 'react';
import { useQuery } from '@apollo/client/react';
import { NetworkStatus } from '@apollo/client'; // Add this import
import { SkillCategory, SkillFrequency, UserMode } from '@/types';
import { TopSkills, SearchResultAnalysis } from '@/components/search/analysis';
import { GET_SEARCH_STATISTICS } from '@/core/client/services/api/queries/stats';
import { LoadingSpinner } from '@/components/ui';
import { useAppDispatch, useAppSelector } from '@/core/client/services/state/hooks';
import { setSearchStatistics } from '@/core/client/services/state/features/search/searchSlice';

interface SearchResultAnalysisPanelProps {
	activeColor: string;
	userMode: UserMode;
	searchedSkills: string[];
	skillCategories: SkillCategory[];
}

interface SearchStatisticsData {
	searchStatistics: {
		topSkills: SkillFrequency[];
		totalCount: number;
	};
}

interface SearchStatisticsVars {
	mode: UserMode;
	skills: string[];
}

export const SearchResultAnalysisPanel: React.FC<
	SearchResultAnalysisPanelProps
> = ({ activeColor, userMode, searchedSkills = [], skillCategories }) => {
	const dispatch = useAppDispatch();

	// Redux에서 persist된 데이터 가져오기
	const persistedTotalCount = useAppSelector((state) => state.search[userMode].totalCount);
	const persistedTopSkills = useAppSelector((state) => state.search[userMode].topSkills);


	const { data: statsData, loading: statsLoading, networkStatus, error: statsError } = useQuery<
		SearchStatisticsData,
		SearchStatisticsVars
	>(GET_SEARCH_STATISTICS, {
		variables: { mode: userMode, skills: searchedSkills },
		skip: searchedSkills.length === 0,
		fetchPolicy: 'cache-and-network',
	});

	// 에러 발생 시 콘솔에 기록 (UI에서는 빈 결과로 처리)
	useEffect(() => {
		if (statsError) {
			console.warn('[SearchResultAnalysisPanel] Statistics query error:', statsError.message);
		}
	}, [statsError]);

	// Apollo 데이터가 있으면 사용, 없으면 Redux persist 데이터 사용
	const totalCount = statsData?.searchStatistics?.totalCount ?? persistedTotalCount;
	const topSkills = statsData?.searchStatistics?.topSkills ?? persistedTopSkills;

	// Apollo 데이터가 도착하면 Redux에 저장 (persist용)
	useEffect(() => {
		if (statsData?.searchStatistics) {
			dispatch(setSearchStatistics({
				userMode,
				totalCount: statsData.searchStatistics.totalCount,
				topSkills: statsData.searchStatistics.topSkills,
			}));
		}
	}, [statsData, userMode, dispatch]);

	if (searchedSkills.length === 0) {
		return (
			<div className="text-center p-8 text-text-tertiary h-full flex flex-col justify-center items-center">
				<h3 className="text-xl font-bold mb-4">분석할 검색어가 없습니다.</h3>
				<p>좌측 패널에서 스킬을 선택하고 검색해주세요.</p>
			</div>
		);
	}

	// persist된 데이터가 있으면 스피너 없이 바로 표시 여부를 판단 (SearchResultAnalysis 컴포넌트에 전달)
	const hasPersistedData = persistedTopSkills.length > 0 && persistedTotalCount !== null;

	// 로딩 스피너는 statsLoading (네트워크 요청 중) && networkStatus === NetworkStatus.loading (초기 로딩) && statsData가 없을 때만 표시
	if (statsLoading && networkStatus === NetworkStatus.loading && !statsData?.searchStatistics) {
		return (
			<div className="w-full h-full flex justify-center items-center">
				<LoadingSpinner
					size={48}
					message="분석 데이터 로딩 중..."
					color={activeColor}
				/>
			</div>
		);
	}

	return (
		<div className="w-full animate-fade-in h-full overflow-y-auto custom-scrollbar pr-2 space-y-6">
			<h2 className="text-2xl font-bold text-text-primary">검색 결과 분석</h2>
			<SearchResultAnalysis
				searchedSkills={searchedSkills}
				totalCount={totalCount ?? undefined}
				statsLoading={statsLoading}
				activeColor={activeColor}
			/>
			<div className="bg-panel-main p-6 rounded-lg shadow-lg border border-border/30">
				<TopSkills
					mode={userMode}
					skills={searchedSkills}
					limit={15}
					skillCategories={skillCategories}
					topSkills={topSkills}
					loading={statsLoading}
				/>
			</div>
		</div>
	);
};
