// Frontend/Front-Server/src/components/common/BaseTooltip.tsx
import React from 'react';

interface BaseTooltipProps {
    title: string;
    value?: string | number;
    icon?: React.ReactNode;
}

/**
 * @description 재사용 가능한 스타일이 적용된 제네릭 툴팁 컨텐츠 컴포넌트
 */
const BaseTooltip: React.FC<BaseTooltipProps> = ({ title, value, icon }) => (
    <div className="bg-primary text-white p-2 px-3 rounded-lg flex items-center gap-2.5 border border-primary-light shadow-lg">
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
