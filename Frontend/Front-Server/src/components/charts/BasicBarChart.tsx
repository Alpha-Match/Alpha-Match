import React, { useState, useEffect } from 'react';
import { BarChart, Bar, XAxis, YAxis, Tooltip, ResponsiveContainer, Cell } from 'recharts';
import { useAppSelector } from '@/lib/client/services/state/hooks';
import { Skeleton } from '@/components/ui/Skeleton';

interface CategoryBarChartProps {
  title: string;
  data: { name: string; value: number }[];
  colorScale: (index: number) => string;
  loading?: boolean;
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

export const BasicBarChart: React.FC<CategoryBarChartProps> = ({ title, data, colorScale, loading }) => {
  const theme = useAppSelector((state) => state.ui.theme);
  const [resolvedTickColor, setResolvedTickColor] = useState('currentColor');

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

  if (loading) {
    return <Skeleton className="h-[350px]" />;
  }
  
  return (
    <div className="bg-panel-main p-6 rounded-lg shadow-lg border border-border/30 h-full">
      <h3 className="text-xl font-semibold text-text-primary mb-4">
        {title}
      </h3>
      <ResponsiveContainer width="100%" height={300}>
        <BarChart data={data} layout="vertical" margin={{ top: 5, right: 20, left: 30, bottom: 5 }}>
            <XAxis type="number" hide />
            <YAxis 
                dataKey="name" 
                type="category" 
                axisLine={false} 
                tickLine={false}
                tick={{ fill: resolvedTickColor, fontSize: 12 }}
                width={120} // Wider for longer category names
                interval={0}
            />
            <Tooltip content={<CustomTooltip />} cursor={{ fill: 'rgba(128, 128, 128, 0.1)' }} />
            <Bar dataKey="value" radius={[0, 4, 4, 0]}>
                {data.map((entry, index) => (
                    <Cell key={`cell-${entry.name}`} fill={colorScale(index)} />
                ))}
            </Bar>
        </BarChart>
      </ResponsiveContainer>
    </div>
  );
};
