import React, { useState, useEffect } from 'react';
import { Radar, RadarChart, PolarGrid, PolarAngleAxis, ResponsiveContainer, Tooltip, Legend } from 'recharts';
import { useAppSelector } from '@/lib/client/services/state/hooks';

interface CategoryRadarChartProps {
    title: string;
    data: { name: string; value: number; fullMark: number }[];
    color: string;
}

export const BasicRadarChart: React.FC<CategoryRadarChartProps> = ({ title, data, color }) => {
    const [tickColor, setTickColor] = useState('currentColor');
    const theme = useAppSelector((state) => state.ui.theme);

    useEffect(() => {
        const getCssVariableColor = (variableName: string) => {
            if (typeof window !== 'undefined') {
                return window.getComputedStyle(document.documentElement).getPropertyValue(variableName).trim();
            }
            return 'currentColor';
        };
        const secondaryColor = getCssVariableColor('--color-text-secondary');
        setTickColor(secondaryColor ? `rgb(${secondaryColor})` : 'currentColor');
    }, [theme]);
    
    const renderPolarAngleAxis = (props: any) => {
        const { x, y, payload } = props;
        return (
            <text
                x={x}
                y={y}
                fill={tickColor}
                fontSize={12}
                textAnchor="middle"
            >
                {payload.value}
            </text>
        );
    };

    return (
        <div className="bg-panel-main p-6 rounded-lg shadow-lg h-full">
            <h3 className="text-xl font-semibold mb-4 text-text-secondary">{title}</h3>
            <ResponsiveContainer width="100%" height={350}>
                <RadarChart cx="50%" cy="50%" outerRadius="80%" data={data}>
                    <PolarGrid stroke="rgb(var(--color-border))" />
                    <PolarAngleAxis dataKey="name" tick={renderPolarAngleAxis} />
                    <Radar 
                        name="Count" 
                        dataKey="value" 
                        stroke={color} 
                        fill={color} 
                        fillOpacity={0.6} 
                    />
                    <Tooltip 
                        contentStyle={{ 
                            backgroundColor: 'rgb(var(--color-panel-sidebar))',
                            borderColor: 'rgb(var(--color-border))',
                            color: 'rgb(var(--color-text-primary))'
                        }}
                    />
                    <Legend wrapperStyle={{color: tickColor, paddingTop: '20px'}}/>
                </RadarChart>
            </ResponsiveContainer>
        </div>
    );
};
