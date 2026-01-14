import React from 'react';
import {useQuery} from '@apollo/client/react';
import {GET_CATEGORY_DISTRIBUTION} from '@/core/client/services/api/queries/search';
import {CategoryMatchDistribution} from '@/types';
import {LoadingSpinner} from '@/components/ui';
import chroma from 'chroma-js';

// --- Sub-components ---

const StatCard = ({ title, value, unit }: { title: string; value: string | number; unit: string }) => (
    <div className="bg-background p-3 rounded-lg text-center">
        <p className="text-xs text-text-secondary">{title}</p>
        <div className="flex items-baseline justify-center gap-1">
            <p className="text-xl font-bold text-text-primary">{value}</p>
            <p className="text-xs text-text-tertiary">{unit}</p>
        </div>
    </div>
);



// --- Combined Props ---
interface SearchResultAnalysisProps {
    searchedSkills: string[];
    totalCount?: number;
    statsLoading: boolean;
    activeColor: string;
}

interface CategoryDistributionData {
  getCategoryDistribution: CategoryMatchDistribution[];
}

interface CategoryDistributionVars {
  skills: string[];
}

export const SearchResultAnalysis: React.FC<SearchResultAnalysisProps> = ({ searchedSkills, totalCount, statsLoading, activeColor }) => {
    const { data: catData, loading: catLoading, error: catError } = useQuery<CategoryDistributionData, CategoryDistributionVars>(
        GET_CATEGORY_DISTRIBUTION,
        {
            variables: { skills: searchedSkills },
            skip: !searchedSkills || searchedSkills.length === 0,
        }
    );

    const distributions = catData?.getCategoryDistribution;

    const renderStats = () => {
        if (statsLoading) {
            return (
                <div className="flex items-center justify-center h-24">
                    <LoadingSpinner size={24} />
                </div>
            );
        }
        return (
            <div>
                <h3 className="text-sm font-semibold mb-3 text-text-secondary">검색 요약</h3>
                <div className="grid grid-cols-2 gap-3">
                    <StatCard title="검색된 스킬" value={searchedSkills.length} unit="개" />
                    <StatCard title="총 결과" value={(totalCount ?? 0).toLocaleString()} unit="건" />
                </div>
            </div>
        );
    };

    const renderCategoryDistribution = () => {
        if (catLoading) {
            return (
                <div className="mt-6">
                    <h3 className="text-sm font-semibold mb-3 text-text-secondary">검색 스킬 카테고리 분포</h3>
                    <div className="flex justify-center py-4 h-[150px] items-center">
                        <LoadingSpinner size={24} color={activeColor} />
                    </div>
                </div>
            );
        }
        if (catError || !distributions || distributions.length === 0) {
            return null;
        }

        const size = 120;
        const strokeWidth = 20;
        const radius = (size - strokeWidth) / 2;
        const circumference = 2 * Math.PI * radius;
        let currentOffset = 0;

        const categoryColorScale = chroma.scale([activeColor, chroma(activeColor).brighten(2)]).domain([0, distributions.length - 1]);

        return (
            <div className="mt-6">
                <h3 className="text-sm font-semibold mb-3 text-text-secondary">검색 스킬 카테고리 분포</h3>
                <div className="flex items-center gap-6">
                    <div className="relative" style={{ width: size, height: size }}>
                        <svg width={size} height={size} className="transform -rotate-90">
                            {distributions.map((dist, index) => {
                                const percentage = dist.percentage / 100;
                                const strokeDasharray = circumference * percentage;
                                const strokeDashoffset = -currentOffset;
                                currentOffset += strokeDasharray;
                                const color = categoryColorScale(index).hex();
                                return (
                                    <circle key={index} cx={size/2} cy={size/2} r={radius} fill="transparent" stroke={color} strokeWidth={strokeWidth} strokeDasharray={`${strokeDasharray} ${circumference - strokeDasharray}`} strokeDashoffset={strokeDashoffset} />
                                );
                            })}
                        </svg>
                        <div className="absolute inset-0 flex items-center justify-center">
                            <div className="text-center">
                                <div className="text-2xl font-bold text-text-primary">{distributions.length}</div>
                                <div className="text-xs text-text-tertiary">카테고리</div>
                            </div>
                        </div>
                    </div>
                    <div className="flex-1 space-y-1.5">
                        {distributions.map((dist, index) => {
                            const color = categoryColorScale(index).hex();
                            return (
                                <div key={index} className="flex items-center justify-between text-sm">
                                    <div className="flex items-center gap-2 flex-1 min-w-0">
                                        <div className="w-3 h-3 rounded-full flex-shrink-0" style={{ backgroundColor: color }} />
                                        <span className="text-text-secondary truncate" title={dist.category}>{dist.category}</span>
                                    </div>
                                    <div className="flex items-center gap-2 ml-2">
                                        <span className="text-text-tertiary text-xs">({dist.skillCount}개)</span>
                                        <span className="text-text-primary font-medium min-w-[45px] text-right">{dist.percentage.toFixed(1)}%</span>
                                    </div>
                                </div>
                            );
                        })}
                    </div>
                </div>
                <div className="mt-3 pt-3 border-t border-border/20">
                    <div className="text-xs text-text-tertiary">
                        {distributions.map((dist, index) => (
                            <span key={index}>
                                <span className="font-medium" style={{ color: categoryColorScale(index).hex() }}>{dist.category}</span>
                                : {dist.matchedSkills.join(', ')}
                                {index < distributions.length - 1 && ' | '}
                            </span>
                        ))}
                    </div>
                </div>
            </div>
        );
    };

    return (
        <div className="bg-panel-main p-6 rounded-lg shadow-lg border border-border/30">
            {renderStats()}
            {renderCategoryDistribution()}
        </div>
    );
};
