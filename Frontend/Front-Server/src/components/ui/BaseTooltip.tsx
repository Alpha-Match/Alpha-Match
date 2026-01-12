import React from 'react';

export interface BaseTooltipProps {
    title: string;
    value?: string | number;
    icon?: React.ReactNode;
    color?: string; // 툴팁 배경 색상 (선택적)
}

/**
 * @description 재사용 가능한 스타일이 적용된 제네릭 툴팁 컨텐츠 컴포넌트
 */
export const BaseTooltip: React.FC<BaseTooltipProps> = ({ title, value, icon, color }) => (
    <div
        className="text-white p-2 px-3 rounded-lg flex items-center gap-2.5 border shadow-lg"
        style={color ? { backgroundColor: color, borderColor: color } : {}}
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