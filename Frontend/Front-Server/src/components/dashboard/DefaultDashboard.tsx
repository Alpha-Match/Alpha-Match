// Frontend/Front-Server/src/components/dashboard/DefaultDashboard.tsx
import React from 'react';
import chroma from 'chroma-js';
import {useQuery} from '@apollo/client/react';
import {useAppSelector} from '../../services/state/hooks';
import {CompanyJobCount, UserMode} from '../../types';
import {useHydrated} from '../../hooks/useHydrated';
import {CANDIDATE_THEME_COLORS, RECRUITER_THEME_COLORS} from '../../constants';
import {GET_TOP_COMPANIES} from '../../services/api/queries/dashboard';

// Common Components
import {
    BaseTooltip,
    CategoryPieChart,
    GenericTreemap,
    Skeleton,
    SkillIcon,
    StatsDisplay,
    TopItemsBarChart
} from '../common';

interface TopCompaniesData {
    topCompanies: CompanyJobCount[];
}

export default function DefaultDashboard() {
    const isHydrated = useHydrated();
    const userMode = useAppSelector((state) => state.ui.userMode);
    const dashboardData = useAppSelector((state) => state.search[userMode].dashboardData);

    const { data: companiesData, loading: companiesLoading, error: companiesError } = useQuery<TopCompaniesData>(
        GET_TOP_COMPANIES, 
        { 
            variables: { limit: 10 },
            skip: userMode !== UserMode.CANDIDATE
        }
    );

    const themeColors = userMode === UserMode.RECRUITER
        ? RECRUITER_THEME_COLORS
        : CANDIDATE_THEME_COLORS;
        
    if (!isHydrated || !dashboardData) {
        return (
            <div className="p-6 h-full">
                <Skeleton className="h-24 mb-8" />
                <Skeleton className="h-12 w-1/2 my-4" />
                <div className="space-y-8">
                    <Skeleton className="h-96" />
                    <Skeleton className="h-64" />
                </div>
            </div>
        );
    }

    const baseColor = themeColors[0];

    // --- Dynamic Content ---
    const isCandidateMode = userMode === UserMode.CANDIDATE;
    const totalSkills = dashboardData.reduce((acc, category) => acc + category.skills.length, 0);
    const totalJobs = dashboardData.reduce((acc, category) => acc + category.skills.reduce((sAcc, s) => sAcc + s.count, 0), 0);

    const stats = [
        { title: "총 기술 스택", value: totalSkills.toLocaleString(), unit: "개" },
        { title: "기술 카테고리", value: dashboardData.length.toLocaleString(), unit: "개" },
        { title: isCandidateMode ? "총 공고" : "총 인재", value: (totalJobs / 1000).toFixed(1), unit: "k" },
    ];

    const mainTitle = isCandidateMode ? "채용 시장 기술 트렌드 (구직자)" : "인재 시장 기술 트렌드 (리크루터)";
    const searchPrompt = isCandidateMode ? "관심 있는 기술을 선택하고, 시장의 기회를 확인하세요." : "필요한 기술을 선택하고, 최고의 인재를 찾아보세요.";
    const analysisSectionTitle = isCandidateMode ? "주요 기술별 채용 트렌드" : "주요 기술별 인재풀 분석";
    const topSkillsTitle = isCandidateMode ? "가장 많이 요구되는 기술 Top " : "가장 많은 인재가 보유한 기술 Top ";
    const categoryDistributionTitle = isCandidateMode ? "기술 영역별 채용 비중" : "기술 영역별 인재 비중";
    const detailSectionTitle = isCandidateMode ? "기술 영역별 채용 요구 기술" : "기술 영역별 인재 보유 기술";

    // --- Data Preparation ---
    const categoryTotals = dashboardData
        .map(cat => ({
            name: cat.category,
            value: cat.skills.reduce((acc, skill) => acc + skill.count, 0),
        }))
        .sort((a, b) => b.value - a.value);
    const categoryColorScale = chroma.scale([baseColor, chroma(baseColor).brighten(2)]).domain([0, categoryTotals.length - 1]);

    const topCompaniesData = (companiesData?.topCompanies || []).map((company) => ({
        name: company.companyName,
        value: company.jobCount,
    })).sort((a, b) => b.value - a.value);

    const allSkills = dashboardData.flatMap(category => category.skills)
        .sort((a, b) => b.count - a.count)
        .slice(0, 20);

    return (
        <div className="p-6 h-full text-text-primary animate-fade-in">
            <div className="bg-panel-main p-6 rounded-lg shadow-lg text-center mb-8">
                <p className="text-text-secondary">{searchPrompt}</p>
            </div>

            <h2 className="text-3xl font-bold text-text-primary my-4">{mainTitle}</h2>

            {dashboardData.length === 0 ? (
                <div className="flex justify-center items-center h-64"><p className="text-text-secondary">대시보드 데이터가 없습니다.</p></div>
            ) : (
                <div className="space-y-8">
                    {/* 1. High-Level Summary */}
                    <StatsDisplay title="시장 현황 요약" stats={stats} />
                    
                    {/* 2. Top Companies (Candidate Mode Only) */}
                    {userMode === UserMode.CANDIDATE && (
                        <TopItemsBarChart
                            title={"지금 가장 채용에 적극적인 기업 Top "+topCompaniesData.length}
                            data={topCompaniesData}
                            color={baseColor}
                            loading={companiesLoading}
                        />
                    )}

                    {/* 3. Tech Stack Analysis Section */}
                    <div className="space-y-8">
                        <h2 className="text-2xl font-bold text-text-primary pt-8 border-t border-border/20">{analysisSectionTitle}</h2>
                        <div className="grid grid-cols-1 lg:grid-cols-2 gap-8 items-start">
                             <GenericTreemap
                                key={`top-skills-treemap-${userMode}`}
                                title={topSkillsTitle + allSkills.length}
                                data={allSkills.map(s => ({ name: s.skill, value: s.count }))}
                                baseCategoryColor={baseColor}
                                renderCellContent={({ name, width, height, textColor }) => {
                                    const showText = width > 50 && height > 40;
                                    return (
                                        <div className="w-full h-full flex flex-col items-center justify-center overflow-hidden p-1" style={{ color: textColor }}>
                                            <SkillIcon skill={name} className={`${showText ? 'w-8 h-8' : 'w-5 h-5'} mb-1`} />
                                            {showText && <p className="text-sm font-semibold text-center truncate w-full">{name}</p>}
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
                            <CategoryPieChart
                                title={categoryDistributionTitle}
                                data={categoryTotals}
                                colorScale={categoryColorScale}
                                baseColor={baseColor}
                                loading={false}
                            />
                        </div>
                    </div>

                    {/* 4. Detailed Category Breakdown */}
                    <div className="space-y-4">
                        <h2 className="text-2xl font-bold text-text-primary pt-8 border-t border-border/20">{detailSectionTitle}</h2>
                        <div className="grid grid-cols-1 md:grid-cols-2 xl:grid-cols-3 gap-8">
                            {categoryTotals.map((sortedEntry, index) => {
                                const categoryData = dashboardData.find(d => d.category === sortedEntry.name);
                                if (!categoryData) return null;
                                
                                const baseCategoryColor = categoryColorScale(index).hex();

                                return (
                                    <GenericTreemap
                                        key={categoryData.category}
                                        title={`${categoryData.category} 시장의 핵심 기술`}
                                        data={categoryData.skills.map(s => ({ name: s.skill, value: s.count }))}
                                        baseCategoryColor={baseCategoryColor}
                                        renderCellContent={({ name, width, height, textColor }) => {
                                            const showText = width > 40 && height > 30;
                                            return (
                                                <div className="w-full h-full flex flex-col items-center justify-center overflow-hidden p-1" style={{ color: textColor }}>
                                                    <SkillIcon skill={name} className={`${showText ? 'w-6 h-6' : 'w-4 h-4'} mb-1`} />
                                                    {showText && <p className="text-xs text-center truncate w-full">{name}</p>}
                                                </div>
                                            );
                                        }}
                                        renderTooltipContent={({ name, value }) => (
                                            <BaseTooltip
                                                title={name}
                                                value={value}
                                                color={baseCategoryColor} // Use category-specific color for tooltip
                                                icon={<SkillIcon skill={name} className="w-6 h-6" />}
                                            />
                                        )}
                                    />
                                );
                            })}
                        </div>
                    </div>
                </div>
            )}
        </div>
    );
}

