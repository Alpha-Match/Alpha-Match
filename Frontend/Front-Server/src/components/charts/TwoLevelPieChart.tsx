import React, {useEffect, useState} from 'react';
import {Cell, Legend, Pie, PieChart, ResponsiveContainer, Tooltip, LegendPayload} from 'recharts';
import {SkillIcon} from '@/components/ui/SkillIcon'; // Import SkillIcon
import {PieData} from '@/types';
import {useAppSelector} from "@/core/client/services/state/hooks"; // Import PieData

// Props for the component
interface TwoLevelPieChartProps {
    innerData: PieData[];
    outerData: PieData[];
    categoryColorMap: Map<string, string>;
}



interface TooltipPayloadItem {
  payload: PieData; 
  name?: string;
  value?: number;
  dataKey?: string;
  fill?: string;
  stroke?: string;
  color?: string;
}

interface CustomTooltipComponentProps {
  active?: boolean;
  payload?: TooltipPayloadItem[];
  categoryColorMap: Map<string, string>;
}

// --- Sub-component: Custom Tooltip ---
const CustomTooltip: React.FC<CustomTooltipComponentProps> = ({ active, payload, categoryColorMap }) => {
    if (active && payload && payload.length) {
        const data = payload[0].payload; 
        const category = data.category || data.name; 
        const color = categoryColorMap.get(category); 

        if (!color) return null; 

        return (
            <div className="bg-panel-main p-2 border border-border rounded-md shadow-lg flex items-center gap-2">
                <SkillIcon skill={data.name} className="w-5 h-5" />
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


interface CustomLegendPayloadItem {
  value: string; 
  color: string; 
  id?: string;
  type?: string;
}

interface CustomLegendComponentProps {
  payload?: LegendPayload[]; 
}

// Custom Legend formatter component
const RenderCustomizedLegend: React.FC<CustomLegendComponentProps> = (props) => {
  const { payload } = props;
  const tickColor = 'rgb(var(--color-text-secondary))'; 

  // Filter payload to show only categories (innerData)
  const categoryPayload = payload?.filter(entry => {
    // Recharts payload item has a nested 'payload' which is our original PieData
    const originalData = entry.payload as PieData;
    // Categories are assumed to be in innerData and do not have a 'category' field themselves,
    // while outerData items (skills) will have a 'category' field linking them to their parent category.
    return !originalData?.category;
  });

  return (
    <ul className="flex flex-wrap gap-x-4 gap-y-1 justify-center p-2">
      {categoryPayload?.map((entry, index: number) => {
        const itemLabel = entry.value;
        const color = entry.color; 

        return (
          <li key={`item-${index}`} className="flex items-center text-sm" style={{ color: tickColor }}>
            <div className="w-3 h-3 rounded-full mr-2" style={{ backgroundColor: color }}></div>
            {itemLabel}
          </li>
        );
      })}
    </ul>
  );
};

export const TwoLevelPieChart: React.FC<TwoLevelPieChartProps> = ({ innerData, outerData, categoryColorMap }) => {
    const [resolvedLabelColor, setResolvedLabelColor] = useState('currentColor');
    const [resolvedLegendItemColor, setResolvedLegendItemColor] = useState('currentColor');
    const theme = useAppSelector((state) => state.theme.theme);

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
                    label={({ name, percent = 0, x, y, cx, cy, midAngle = 0, outerRadius, fill = resolvedLabelColor }) => {
                      if (percent < 0.05) return null; // Only show for slices > 5%

                      // Calculate position closer to the outer edge of the inner ring
                      const radius = outerRadius * 0.8; // Place label at 80% of the radius
                      const ex = cx + radius * Math.cos(-midAngle * RADIAN);
                      const ey = cy + radius * Math.sin(-midAngle * RADIAN);

                      return (
                        <text
                          x={ex}
                          y={ey}
                          fill={resolvedLabelColor} // 테마에 맞는 색상으로 변경
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
                        <Cell key={`cell-inner-${entry.name}`} fill={categoryColorMap.get(entry.name)} />
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
                    label={({ name, percent = 0, x, y, cx, cy, midAngle = 0, innerRadius, outerRadius }) => {
                        // Recharts' label function receives props like { cx, cy, midAngle, outerRadius, percent, name, value }
                        const RADIAN = Math.PI / 180;
                        if (percent < 0.02) return null; // Only show for slices > 2% to avoid clutter

                        const iconSize = 20; // Size of the skill icon
                        const labelRadius = innerRadius + (outerRadius - innerRadius) * 0.5; // Midpoint of the outer ring
                        const iconX = cx + labelRadius * Math.cos(-midAngle * RADIAN);
                        const iconY = cy + labelRadius * Math.sin(-midAngle * RADIAN);

                        return (
                            <foreignObject
                                x={iconX - iconSize / 2}
                                y={iconY - iconSize / 2}
                                width={iconSize}
                                height={iconSize}
                                className="pointer-events-none" // Ensure events pass through
                            >
                                <div className="flex items-center justify-center h-full w-full">
                                    <SkillIcon skill={name!} className="w-full h-full" />
                                </div>
                            </foreignObject>
                        );
                    }}
                >
                    {outerData.map((entry) => {
                        const categoryColor = categoryColorMap.get(entry.category) || '#CCCCCC';
                        // Use the same color for the skill as its category for consistency
                        const skillColor = categoryColor;
                        return <Cell key={`cell-outer-${entry.name}`} fill={skillColor} />;
                    })}
                </Pie>
                <Tooltip content={<CustomTooltip categoryColorMap={categoryColorMap}  />} />
                <Legend content={<RenderCustomizedLegend />} />
            </PieChart>
        </ResponsiveContainer>
    );
};
