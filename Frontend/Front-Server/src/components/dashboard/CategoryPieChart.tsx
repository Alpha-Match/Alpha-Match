// Frontend/Front-Server/src/components/dashboard/CategoryPieChart.tsx
import React from 'react';
import { PieChart, Pie, Cell, ResponsiveContainer, Legend } from 'recharts';
import { Scale } from 'chroma-js';

// --- Sub-components ---
const CustomPieLabel = ({ cx, cy, midAngle, outerRadius, percent }: any) => {
    const RADIAN = Math.PI / 180;
    const radius = outerRadius * 1.1;
    const x = cx + radius * Math.cos(-midAngle * RADIAN);
    const y = cy + radius * Math.sin(-midAngle * RADIAN);
  
    return (
      <text x={x} y={y} className="fill-text-primary" textAnchor="middle" dominantBaseline="central">
        {`${(percent * 100).toFixed(0)}%`}
      </text>
    );
};

// --- Props ---
interface CategoryPieChartProps {
    data: { name: string; value: number }[];
    colorScale: Scale;
    baseColor: string;
}

// --- Main Component ---
const CategoryPieChart: React.FC<CategoryPieChartProps> = ({ data, colorScale, baseColor }) => {
    return (
        <div className="bg-panel-main p-6 rounded-lg shadow-lg">
            <h3 className="text-xl font-semibold mb-4 text-text-secondary">직무별 점유율</h3>
            <ResponsiveContainer width="100%" height={300}>
                <PieChart>
                    <Pie
                        data={data}
                        cx="50%"
                        cy="50%"
                        labelLine={false}
                        label={<CustomPieLabel />}
                        outerRadius={120}
                        fill={baseColor}
                        dataKey="value"
                    >
                        {data.map((entry, index) => (
                            <Cell key={`cell-${entry.name}`} fill={colorScale(index).hex()} />
                        ))}
                    </Pie>
                    {/* Recharts' own tooltip is disabled to allow for a consistent UX with Tippy.js if needed elsewhere */}
                    {/* <Tooltip contentStyle={{ display: 'none' }} /> */}
                    <Legend wrapperStyle={{ fill: 'var(--color-text-secondary)' }}/>
                </PieChart>
            </ResponsiveContainer>
        </div>
    );
};

export default CategoryPieChart;
