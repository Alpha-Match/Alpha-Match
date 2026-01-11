import React, { useState, useEffect } from 'react';
import { BarChart, Bar, XAxis, YAxis, Tooltip, ResponsiveContainer, Cell } from 'recharts';
import { Scale } from 'chroma-js';
import { useAppSelector } from '../../services/state/hooks';

interface CategoryBarChartProps {
    data: { name: string; value: number }[];
    colorScale: Scale;
}

const CustomTooltip = ({ active, payload, label }: any) => {
    if (active && payload && payload.length) {
        return (
            <div className="bg-panel-main p-2 border border-border rounded-md shadow-lg">
                <p className="label text-text-primary">{`${label} : ${payload[0].value.toLocaleString()}`}</p>
            </div>
        );
    }

    return null;
};

const CategoryBarChart: React.FC<CategoryBarChartProps> = ({ data, colorScale }) => {
    const [resolvedTickColor, setResolvedTickColor] = useState('currentColor');
    const theme = useAppSelector((state) => state.ui.theme);

    useEffect(() => {
        const getCssVariableColor = (variableName: string) => {
            if (typeof window !== 'undefined') {
                const htmlElement = document.documentElement;
                return window.getComputedStyle(htmlElement).getPropertyValue(variableName).trim();
            }
            return 'currentColor';
        };

        const secondaryColor = getCssVariableColor('--color-text-secondary');
        setResolvedTickColor(secondaryColor ? `rgb(${secondaryColor})` : 'currentColor');
    }, [theme]);

    return (
        <div className="bg-panel-main p-6 rounded-lg shadow-lg">
            <h3 className="text-xl font-semibold mb-4 text-text-secondary">기술 스택 분포</h3>
            <ResponsiveContainer width="100%" height={300}>
                <BarChart data={data} layout="vertical" margin={{ top: 5, right: 20, left: 20, bottom: 5 }}>
                    <XAxis type="number" hide />
                    <YAxis 
                        dataKey="name" 
                        type="category" 
                        axisLine={false} 
                        tickLine={false}
                        tick={{ fill: resolvedTickColor, fontSize: 12 }}
                        width={80}
                        interval={0}
                    />
                    <Tooltip content={<CustomTooltip />} cursor={{ fill: 'rgba(128, 128, 128, 0.1)' }} />
                    <Bar dataKey="value" radius={[0, 4, 4, 0]}>
                        {data.map((entry, index) => (
                            <Cell key={`cell-${entry.name}`} fill={colorScale(index).hex()} />
                        ))}
                    </Bar>
                </BarChart>
            </ResponsiveContainer>
        </div>
    );
};

export default CategoryBarChart;
