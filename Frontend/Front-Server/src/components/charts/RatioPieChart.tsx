/**
 * @file RatioPieChart.tsx
 * @description 비율을 시각화하는 간단한 파이 차트 컴포넌트
 */
'use client';

import React, { useState, useEffect } from 'react';
import { PieChart, Pie, Cell, ResponsiveContainer, Label } from 'recharts';
import { useAppSelector } from '@/services/state/hooks';

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
  const [resolvedLabelColor, setResolvedLabelColor] = useState('currentColor');
  const [resolvedPanel2Color, setResolvedPanel2Color] = useState('currentColor');
  const theme = useAppSelector((state) => state.ui.theme); // Get theme for useEffect dependency

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

    const panel2Color = getCssVariableColor('--color-panel-2');
    setResolvedPanel2Color(panel2Color ? `rgb(${panel2Color})` : 'currentColor');

  }, [theme]); // Rerun when theme changes

  const data = [
    { name: 'filled', value: percentage },
    { name: 'empty', value: 100 - percentage },
  ];

  const COLORS = [color, resolvedPanel2Color];

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
                fill={resolvedLabelColor}
                className="text-2xl font-bold"
              />
            </Pie>
          </PieChart>
        </ResponsiveContainer>
      </div>
    </div>
  );
};