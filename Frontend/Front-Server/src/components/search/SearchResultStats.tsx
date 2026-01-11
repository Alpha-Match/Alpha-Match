import React from 'react';
import { Skeleton } from '../common/Skeleton';

const StatCard = ({ title, value, unit }: { title: string; value: string | number; unit: string }) => (
    <div className="bg-background p-3 rounded-lg text-center">
        <p className="text-xs text-text-secondary">{title}</p>
        <div className="flex items-baseline justify-center gap-1">
            <p className="text-xl font-bold text-text-primary">{value}</p>
            <p className="text-xs text-text-tertiary">{unit}</p>
        </div>
    </div>
);

interface SearchResultStatsProps {
    searchedSkills: string[];
    totalCount?: number;
    loading: boolean;
}

export const SearchResultStats: React.FC<SearchResultStatsProps> = ({ searchedSkills, totalCount, loading }) => {

    if (loading) {
        return <Skeleton className="h-28" />;
    }

    return (
        <div className="bg-panel-main p-4 rounded-lg border border-border/30 mt-6">
             <h3 className="text-sm font-semibold mb-3 text-text-secondary">검색 요약</h3>
            <div className="grid grid-cols-2 gap-3">
                <StatCard title="검색된 스킬" value={searchedSkills.length} unit="개" />
                <StatCard title="총 결과" value={(totalCount ?? 0).toLocaleString()} unit="건" />
            </div>
        </div>
    );
};
