// Frontend/Front-Server/src/components/dashboard/SearchedSkillsCategoryDistributionChart.tsx
import React, {useEffect, useState} from 'react';
import {Cell, Legend, Pie, PieChart, ResponsiveContainer} from 'recharts';
import {useAppSelector} from '@/lib/client/services/state/hooks';
import {Skeleton} from '@/components/ui';

// --- Sub-components ---
const CustomPieLabel = ({ cx, cy, midAngle, outerRadius, innerRadius, percent, payload, labelColor }: any) => {
    const RADIAN = Math.PI / 180;
    // Position label inside the slice to avoid overflow
    const radius = innerRadius + (outerRadius - innerRadius) * 0.5;
    const x = cx + radius * Math.cos(-midAngle * RADIAN);
    const y = cy + radius * Math.sin(-midAngle * RADIAN);
    
    const { name } = payload;
  
    // Hide label if the slice is too small to avoid clutter
    if (percent < 0.05) {
        return null;
    }

    return (
      <text 
        x={x} y={y} 
        fill={labelColor}
        textAnchor="middle"
        dominantBaseline="central"
        className="text-xs font-bold"
      >
        {`${name} ${(percent * 100).toFixed(0)}%`}
      </text>
    );
};

// --- Props ---
interface CategoryPieChartProps {
    title: string;
    data: { name: string; value: number }[];
    colorScale: (value: number) => chroma.Color; // 타입 변경
    baseColor: string;
    loading: boolean;
}

// --- Main Component ---
export const BasicPieChart: React.FC<CategoryPieChartProps> = ({ title, data, colorScale, baseColor, loading }) => {
    const [resolvedLabelColor, setResolvedLabelColor] = useState('currentColor');
    const [resolvedLegendItemColor, setResolvedLegendItemColor] = useState('currentColor');
    const theme = useAppSelector((state) => state.ui.theme);

    useEffect(() => {
        const getCssVariableColor = (variableName: string) => {
            if (typeof window !== 'undefined') {
                const htmlElement = document.documentElement;
                const style = window.getComputedStyle(htmlElement);
                return style.getPropertyValue(variableName).trim();
            }
            return 'currentColor';
        };

        const primaryColor = getCssVariableColor('--color-text-primary');
        setResolvedLabelColor(primaryColor ? `rgb(${primaryColor})` : 'currentColor');

        const secondaryColor = getCssVariableColor('--color-text-secondary');
        setResolvedLegendItemColor(secondaryColor ? `rgb(${secondaryColor})` : 'currentColor');

    }, [theme]);

    const legendFormatter = (value: string) => {
        return <span style={{ color: resolvedLegendItemColor }}>{value}</span>;
    };
    
    const renderContent = () => {
        if (loading) {
            return <Skeleton className="w-full h-[300px]" />;
        }
        
        if (!data || data.length === 0) {
            return (
                <div className="flex items-center justify-center h-[300px] text-text-secondary">
                    <p>표시할 데이터가 없습니다.</p>
                </div>
            );
        }

        return (
            <ResponsiveContainer width="100%" height={350}>
                <PieChart>
                    <Pie
                        data={data}
                        cx="50%"
                        cy="50%"
                        labelLine={false}
                        label={<CustomPieLabel labelColor={resolvedLabelColor} />}
                        outerRadius={120}
                        innerRadius={60}
                        fill={baseColor}
                        dataKey="value"
                    >
                        {data.map((entry, index) => (
                            <Cell key={`cell-${entry.name}`} fill={colorScale(index).hex()} />
                        ))}
                    </Pie>
                    <Legend formatter={legendFormatter} verticalAlign="bottom" align="center" />
                </PieChart>
            </ResponsiveContainer>
        );
    };

    return (
        <div className="bg-panel-main p-6 rounded-lg shadow-lg">
            <h3 className="text-xl font-semibold mb-4 text-text-secondary">{title}</h3>
            {renderContent()}
        </div>
    );
};