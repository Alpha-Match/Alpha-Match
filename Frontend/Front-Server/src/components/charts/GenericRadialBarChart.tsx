import React, { useState, useEffect } from 'react';
import { RadialBarChart, RadialBar, Legend, ResponsiveContainer, Tooltip, PolarAngleAxis } from 'recharts';
import { LoadingSpinner } from '@/components/ui/LoadingSpinner';
import { useAppSelector } from '@/core/client/services/state/hooks';

interface RadialChartData {
    name: string;
    value: number;
    fill: string;
}

interface TopCompaniesRadialChartProps {
  title: string;
  data: RadialChartData[];
  loading?: boolean;
  error?: Error | null;
}

const CustomTooltip = ({ active, payload }: any) => {
    if (active && payload && payload.length) {
      const { name, value } = payload[0].payload;
      return (
        <div className="bg-panel-sidebar p-3 border border-border rounded-md shadow-lg">
          <p className="font-bold text-text-primary">{name}</p>
          <p className="text-sm text-text-secondary">{`Count: ${value.toLocaleString()}`}</p>
        </div>
      );
    }
    return null;
};

export const GenericRadialBarChart: React.FC<TopCompaniesRadialChartProps> = ({ title, data, loading, error }) => {
  const theme = useAppSelector((state) => state.ui.theme);
  const [textColor, setTextColor] = useState('currentColor');

  useEffect(() => {
      const getCssVariableColor = (variableName: string) => {
          if (typeof window !== 'undefined') {
              return window.getComputedStyle(document.documentElement).getPropertyValue(variableName).trim();
          }
          return 'currentColor';
      };
      const color = getCssVariableColor('--color-text-secondary');
      setTextColor(color ? `rgb(${color})` : 'currentColor');
  }, [theme]);

  if (loading) return <div className="h-[400px] flex justify-center items-center"><LoadingSpinner /></div>;
  if (error || !data) return <div>Error loading data.</div>;

  return (
    <div className="bg-panel-main p-6 rounded-lg shadow-lg h-full">
        <h3 className="text-xl font-semibold mb-4 text-text-secondary">{title}</h3>
        <ResponsiveContainer width="100%" height={350}>
            <RadialBarChart 
                cx="50%" 
                cy="50%" 
                innerRadius="10%" 
                outerRadius="80%" 
                barSize={10} 
                data={data}
                startAngle={90}
                endAngle={-270}
            >
                <PolarAngleAxis type="number" domain={[0, Math.max(...data.map(d => d.value))]} tick={false} />
                <RadialBar
                    background
                    dataKey="value"
                />
                <Legend 
                    iconSize={10} 
                    layout="vertical" 
                    verticalAlign="middle" 
                    align="right"
                    wrapperStyle={{color: textColor, fontSize: '12px'}}
                    formatter={(value) => <span title={value}>{value.length > 15 ? `${value.slice(0,15)}...` : value}</span>}
                />
                <Tooltip content={<CustomTooltip />} />
            </RadialBarChart>
        </ResponsiveContainer>
    </div>
  );
};
