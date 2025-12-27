// Frontend/Front-Server/src/components/DefaultDashboard.tsx
/**
 * @file DefaultDashboard.tsx
 * @description 검색 실행 전 표시되는 기본 대시보드 컨테이너 컴포넌트.
 *              재사용 가능한 자식 컴포넌트(PieChart, Treemap)에 데이터와 스타일을 전달합니다.
 *              운영체제: Windows
 */
import React from 'react';
import chroma from 'chroma-js';
import { useAppSelector } from '../../services/state/hooks';
import { UserMode } from '../../types';
import { RECRUITER_THEME_COLORS, CANDIDATE_THEME_COLORS } from '../../constants';
import CategoryPieChart from './CategoryPieChart';
import GenericTreemap from './GenericTreemap';
import BaseTooltip from '../common/BaseTooltip';
import SkillIcon from '../common/SkillIcon';
import { useQuery } from '@apollo/client';
import { GET_DASHBOARD_DATA } from '../../services/api/queries/dashboard';

export default function DefaultDashboard() {
    const userMode = useAppSelector((state) => state.ui.userMode);

    const { loading, error, data } = useQuery(GET_DASHBOARD_DATA, {
        variables: { userMode: userMode },
    });

    if (loading) return <p className="p-6 text-white">Loading dashboard data...</p>;
    if (error) return <p className="p-6 text-red-500">Error loading dashboard data: {error.message}</p>;

    const dashboardData = data?.dashboardData || [];


    const themeColors = userMode === UserMode.RECRUITER
        ? RECRUITER_THEME_COLORS
        : CANDIDATE_THEME_COLORS;

    // 1. 데이터 가공: 카테고리별 합계 계산 및 정렬
    const sortedCategoryTotals = dashboardData
        .map(cat => ({
            name: cat.category,
            value: cat.skills.reduce((acc, skill) => acc + skill.count, 0),
        }))
        .sort((a, b) => b.value - a.value);

    // 2. 동적 색상 스케일 및 툴팁 색상 생성
    const baseColor = themeColors[0];
    const categoryColorScale = chroma.scale([baseColor, chroma(baseColor).brighten(2)])
                                     .domain([0, sortedCategoryTotals.length - 1]);
    const tooltipTextColor = chroma(baseColor).luminance() > 0.5 ? '#222' : '#fff';

    return (
        <div className="p-6 h-full text-white animate-fade-in">
            {/* 상단 안내 패널 */}
            <div className="bg-slate-800/50 p-6 rounded-lg shadow-lg text-center mb-8">
                <p className="text-gray-400">좌측 패널에서 원하는 기술 스택과 경력을 선택하여 검색을 시작하세요.</p>
            </div>

            <h2 className="text-3xl font-bold text-gray-200 my-4">전체 직무 선호도 대시보드</h2>

            <div className="space-y-8">
                {/* 직무별 점유율 (파이 차트) */}
                <CategoryPieChart
                    data={sortedCategoryTotals}
                    colorScale={categoryColorScale}
                    baseColor={baseColor}
                />

                {/* 직무별 기술 스택 (트리맵) */}
                <div className="grid grid-cols-1 md:grid-cols-2 xl:grid-cols-3 gap-8">
                    {sortedCategoryTotals.map((sortedEntry, index) => {
                        const categoryData = dashboardData.find(data => data.category === sortedEntry.name);
                        if (!categoryData) return null;

                        const baseCategoryColor = categoryColorScale(index).hex();

                        return (
                            <GenericTreemap
                                key={categoryData.category}
                                title={categoryData.category}
                                data={categoryData.skills.map(s => ({ name: s.skill, value: s.count }))}
                                baseCategoryColor={baseCategoryColor}
                                renderCellContent={({ name, width, height, textColor }) => {
                                    const showText = width > 50 && height > 30;
                                    return (
                                        <div className="w-full h-full flex flex-col items-center justify-center overflow-hidden p-1" style={{ color: textColor }}>
                                            <SkillIcon skill={name} className={`${showText ? 'w-8 h-8' : 'w-5 h-5'} mb-1`} />
                                            {showText && (
                                                <p className="text-xs font-semibold text-center truncate w-full">{name}</p>
                                            )}
                                        </div>
                                    );
                                }}
                                renderTooltipContent={({ name, value }) => (
                                    <BaseTooltip
                                        title={name}
                                        value={value}
                                        color={baseColor}
                                        textColor={tooltipTextColor}
                                        icon={<SkillIcon skill={name} className="w-6 h-6" />}
                                    />
                                )}
                            />
                        );
})}
                </div>
            </div>
        </div>
    );
}

