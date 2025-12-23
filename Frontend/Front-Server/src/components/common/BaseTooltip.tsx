// Frontend/Front-Server/src/components/common/BaseTooltip.tsx
import React from 'react';
import chroma from 'chroma-js';

interface BaseTooltipProps {
    title: string;
    value?: string | number;
    icon?: React.ReactNode;
    color: string;
    textColor: string;
}

// A generic, reusable, styled tooltip content component.
const BaseTooltip: React.FC<BaseTooltipProps> = ({ title, value, icon, color, textColor }) => (
    <div
        style={{
            background: color,
            color: textColor,
            padding: '8px 12px',
            borderRadius: '8px',
            display: 'flex',
            alignItems: 'center',
            gap: '10px',
            border: `1px solid ${chroma(color).darken(0.5).hex()}`,
            boxShadow: '0 4px 12px rgba(0,0,0,0.2)',
        }}
    >
        {icon}
        <div className="flex flex-col">
            <span className="font-bold text-md">{title}</span>
            {value !== undefined && (
                <span className="text-sm opacity-90">Value: {value}</span>
            )}
        </div>
    </div>
);

export default BaseTooltip;
