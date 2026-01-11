/**
 * @file RatioPieChart.tsx
 * @description 비율을 시각화하는 간단한 파이 차트 컴포넌트
 */
'use client';

import React from 'react';
import { PieChart, Pie, Cell, ResponsiveContainer, Label } from 'recharts';

interface RatioPieChartProps {
  title: string;
  percentage: number;
  color: string;
  size?: number;
}

export const RatioPieChart: React.FC<RatioPieChartProps> = ({
  title,
  percentage,
  color,
  size = 160,
}) => {
  const data = [
    { name: 'filled', value: percentage },
    { name: 'empty', value: 100 - percentage },
  ];

  const COLORS = [color, 'var(--color-panel-2)'];

  return (
    <div className="flex flex-col items-center">
      <h4 className="text-md font-semibold text-text-secondary mb-2">{title}</h4>
      <div style={{ width: size, height: size }}>
        <ResponsiveContainer>
          <PieChart>
            <Pie
              data={data}
              cx="50%"
              cy="50%"
              innerRadius={'70%'}
              outerRadius={'100%'}
              fill="#8884d8"
              paddingAngle={0}
              dataKey="value"
              stroke="none"
            >
              {data.map((entry, index) => (
                <Cell key={`cell-${index}`} fill={COLORS[index % COLORS.length]} />
              ))}
               <Label
                value={`${percentage.toFixed(1)}%`}
                position="center"
                fill="var(--color-text-primary)"
                className="text-2xl font-bold"
              />
            </Pie>
          </PieChart>
        </ResponsiveContainer>
      </div>
    </div>
  );
};