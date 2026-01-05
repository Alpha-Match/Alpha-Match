// Frontend/Front-Server/src/components/dashboard/DefaultDashboard.tsx
import React from 'react';
import chroma from 'chroma-js';
import { useAppSelector } from '../../services/state/hooks';
import { UserMode } from '../../types';
import { RECRUITER_THEME_COLORS, CANDIDATE_THEME_COLORS } from '../../constants';
import CategoryPieChart from './CategoryPieChart';
import GenericTreemap from './GenericTreemap';
import BaseTooltip from '../common/BaseTooltip';
import SkillIcon from '../common/SkillIcon';
import { Skeleton } from '../common/Skeleton';

export default function DefaultDashboard() {
    const userMode = useAppSelector((state) => state.ui.userMode);
    const dashboardData = useAppSelector((state) => state.search[userMode].dashboardData);

    const themeColors = userMode === UserMode.RECRUITER
        ? RECRUITER_THEME_COLORS
        : CANDIDATE_THEME_COLORS;
        
    // 데이터가 아직 Redux에 의해 로드되지 않았을 수 있습니다.
    if (!dashboardData) {
        return (
            <div className="p-6 h-full">
                <Skeleton className="h-24 mb-8" />
                <Skeleton className="h-12 w-1/2 my-4" />
                <div className="space-y-8">
                    <Skeleton className="h-64" />
                    <div className="grid grid-cols-1 md:grid-cols-2 xl:grid-cols-3 gap-8">
                        <Skeleton className="h-64" />
                        <Skeleton className="h-64" />
                        <Skeleton className="h-64" />
                    </div>
                </div>
            </div>
        );
    }

    const sortedCategoryTotals = dashboardData
        .map(cat => ({
            name: cat.category,
            value: cat.skills.reduce((acc, skill) => acc + skill.count, 0),
        }))
        .sort((a, b) => b.value - a.value);

    const baseColor = themeColors[0];
    const categoryColorScale = chroma.scale([baseColor, chroma(baseColor).brighten(2)])
                                     .domain([0, sortedCategoryTotals.length - 1]);
    
    return (
        <div className="p-6 h-full text-text-primary animate-fade-in">
            {/* 상단 안내 패널 */}
            <div className="bg-panel-main p-6 rounded-lg shadow-lg text-center mb-8">
                <p className="text-text-secondary">좌측 패널에서 원하는 기술 스택과 경력을 선택하여 검색을 시작하세요.</p>
            </div>

            <h2 className="text-3xl font-bold text-text-primary my-4">전체 직무 선호도 대시보드</h2>

            {dashboardData.length === 0 ? (
                <div className="flex justify-center items-center h-64"><p className="text-text-secondary">대시보드 데이터가 없습니다.</p></div>
            ) : (
                <div className="space-y-8">
                    <CategoryPieChart
                        data={sortedCategoryTotals}
                        colorScale={categoryColorScale}
                        baseColor={baseColor}
                    />
                    <div className="grid grid-cols-1 md:grid-cols-2 xl:grid-cols-3 gap-8">
                        {sortedCategoryTotals.map((sortedEntry, index) => {
                            const categoryData = dashboardData.find(d => d.category === sortedEntry.name);
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
                                                {showText && <p className="text-xs font-semibold text-center truncate w-full">{name}</p>}
                                            </div>
                                        );
                                    }}
                                    renderTooltipContent={({ name, value }) => (
                                        <BaseTooltip
                                            title={name}
                                            value={value}
                                            color={baseColor}
                                            icon={<SkillIcon skill={name} className="w-6 h-6" />}
                                        />
                                    )}
                                />
                            );
                        })}
                    </div>
                </div>
            )}
        </div>
    );
}