import React from 'react';
import { PieChart, Pie, Cell, ResponsiveContainer, Tooltip, Legend } from 'recharts';
import chroma from 'chroma-js';
import { SkillIcon } from './SkillIcon'; // Import SkillIcon
import { PieData } from '../../types'; // Import PieData

// Props for the component
interface TwoLevelPieChartProps {
    innerData: PieData[];
    outerData: PieData[];
    categoryColorMap: Map<string, string>;
}

// --- Sub-component: Custom Tooltip ---
const CustomTooltip = ({ active, payload, categoryColorMap }: any) => {
    if (active && payload && payload.length) {
        const data = payload[0].payload;
        // The payload for outer pie slices will have a 'category' field
        const category = data.category || (data.payload && data.payload.category);
        const color = categoryColorMap.get(category) || '#CCCCCC'; 

        return (
            <div className="bg-panel-main p-2 border border-border rounded-md shadow-lg flex items-center gap-2">
                <SkillIcon skill={data.name} className="w-5 h-5" color={color} />
                <div className="flex flex-col">
                  <p className="label text-text-primary">{`${data.name} : ${data.value.toLocaleString()}`}</p>
                  {data.percentage && (
                    <p className="text-xs text-text-secondary">{`(${data.percentage.toFixed(1)}%)`}</p>
                  )}
                </div>
            </div>
        );
    }
    return null;
};


// Custom Legend formatter component
const RenderCustomizedLegend = (props: any) => {
  const { payload, categoryColorMap } = props;
  const tickColor = 'rgb(var(--color-text-secondary))'; // Assuming text-secondary for legend items

  return (
    <ul className="flex flex-wrap gap-x-4 gap-y-1 justify-center p-2">
      {payload.map((entry: any, index: number) => {
        // payload only contains categories (from innerData)
        const itemCategory = entry.value; 
        const color = categoryColorMap.get(itemCategory) || '#CCCCCC';

        return (
          <li key={`item-${index}`} className="flex items-center text-sm" style={{ color: tickColor }}>
            <div className="w-3 h-3 rounded-full mr-2" style={{ backgroundColor: color }}></div>
            {itemCategory}
          </li>
        );
      })}
    </ul>
  );
};

export const TwoLevelPieChart: React.FC<TwoLevelPieChartProps> = ({ innerData, outerData, categoryColorMap }) => {
    // Recharts' label function receives props like { cx, cy, midAngle, outerRadius, percent, name, value }
    const RADIAN = Math.PI / 180;

    return (
        <ResponsiveContainer width="100%" height={450}>
            <PieChart>
                {/* Inner Ring: Categories */}
                <Pie
                    data={innerData}
                    dataKey="value"
                    cx="50%"
                    cy="50%"
                    outerRadius={90} // Increased outer radius
                    labelLine={false} // Label inside, no line
                    label={({ name, percent, x, y, cx, cy, midAngle, outerRadius, fill }) => {
                      if (percent < 0.05) return null; // Only show for slices > 5%

                      // Calculate position closer to the outer edge of the inner ring
                      const radius = outerRadius * 0.8; // Place label at 80% of the radius
                      const ex = cx + radius * Math.cos(-midAngle * RADIAN);
                      const ey = cy + radius * Math.sin(-midAngle * RADIAN);

                      return (
                        <text
                          x={ex}
                          y={ey}
                          fill="#FFFFFF" // White for better visibility inside dark slices
                          textAnchor="middle" // Still middle for general centering
                          dominantBaseline="central"
                          className="text-sm font-bold" // Increased font size
                        >
                          {`${name}`}
                        </text>
                      );
                    }}
                >
                    {innerData.map((entry) => (
                        <Cell key={`cell-inner-${entry.name}`} fill={categoryColorMap.get(entry.name) || '#CCCCCC'} />
                    ))}
                </Pie>
                {/* Outer Ring: Skills */}
                <Pie
                    data={outerData}
                    dataKey="value"
                    cx="50%"
                    cy="50%"
                    innerRadius={100} // Increased inner radius, creating a gap
                    outerRadius={140} // Increased outer radius
                    labelLine={false} // Keep label lines disabled
                    label={false} // Disable labels for outer pie
                >
                    {outerData.map((entry) => {
                        const categoryColor = categoryColorMap.get(entry.category) || '#CCCCCC';
                        // Use the same color for the skill as its category for consistency
                        const skillColor = categoryColor;
                        return <Cell key={`cell-outer-${entry.name}`} fill={skillColor} />;
                    })}
                </Pie>
                <Tooltip content={<CustomTooltip categoryColorMap={categoryColorMap} />} />
                <Legend content={<RenderCustomizedLegend categoryColorMap={categoryColorMap} />} payload={innerData.map(d => ({ value: d.name, payload: d }))} />
            </PieChart>
        </ResponsiveContainer>
    );
};
