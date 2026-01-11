import React from 'react';
import { Skeleton } from './Skeleton';

interface Stat {
    title: string;
    value: string | number;
    unit: string;
}

const StatCard = ({ title, value, unit }: Stat) => (
    <div className="bg-background p-4 rounded-lg text-center">
        <p className="text-sm text-text-secondary">{title}</p>
        <div className="flex items-baseline justify-center gap-1">
            <p className="text-2xl font-bold text-text-primary">{value}</p>
            <p className="text-sm text-text-tertiary">{unit}</p>
        </div>
    </div>
);

interface StatsDisplayProps {
    title: string;
    stats: Stat[];
    loading?: boolean;
}

export const StatsDisplay: React.FC<StatsDisplayProps> = ({ title, stats, loading }) => {

    if (loading) {
        return <Skeleton className="h-28" />;
    }

    return (
        <div className="bg-panel-main p-4 rounded-lg shadow-lg">
             <h3 className="text-lg font-semibold text-text-secondary mb-4 px-2">{title}</h3>
            <div className={`grid grid-cols-${stats.length} gap-4`}>
                {stats.map(stat => (
                    <StatCard key={stat.title} {...stat} />
                ))}
            </div>
        </div>
    );
};
