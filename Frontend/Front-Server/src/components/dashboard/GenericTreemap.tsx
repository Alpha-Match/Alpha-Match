// Frontend/Front-Server/src/components/dashboard/GenericTreemap.tsx
import React from 'react';
import { ResponsiveContainer, Treemap } from 'recharts';
import chroma from 'chroma-js';
import Tippy from '@tippyjs/react';

import { useAppDispatch, useAppSelector } from '../../store/hooks';
import { setActiveTooltip } from '../../store/features/ui/uiSlice';

// --- Sub-components ---

interface CustomizedTreemapContentProps {
    // Recharts props
    depth: number; x: number; y: number; width: number; height: number;
    name: string; value: number;
    // Style props
    baseCategoryColor: string;
    maxSkillValue: number;
    // Unique ID for global tooltip management
    id: string;
    // Render props for content and tooltip
    renderCellContent: (props: { name: string; value: number; width: number; height: number; textColor: string; }) => React.ReactNode;
    renderTooltipContent: (props: { name: string; value: number; }) => React.ReactNode;
}

const CustomizedTreemapContent: React.FC<CustomizedTreemapContentProps> = (props) => {
    const {
        id, x, y, width, height, name, value,
        baseCategoryColor, maxSkillValue,
        renderCellContent, renderTooltipContent
    } = props;

    const dispatch = useAppDispatch();
    const { activeTooltipId } = useAppSelector((state) => state.ui);

    // --- Derived State & Style ---
    const isVisible = activeTooltipId === id;
    const treemapColorScale = chroma.scale([
        baseCategoryColor,
        chroma(baseCategoryColor).brighten(2).hex()
    ]).domain([maxSkillValue, 0]);

    const calculatedFill = treemapColorScale(value);
    const hoverStrokeColor = chroma(baseCategoryColor).brighten(2).hex();
    const strokeTextColor = chroma(baseCategoryColor).luminance() > 0.5 ? '#333' : '#eee';

    return (
        <Tippy
            content={renderTooltipContent({ name, value })}
            duration={[100, 200]}
            placement="top"
            inertia={true}
            followCursor={true}
            offset={[0, 10]}
            hideOnClick={false}
            appendTo={document.body}
            visible={isVisible}
        >
            <g
                onMouseEnter={() => dispatch(setActiveTooltip(id))}
                onMouseLeave={() => dispatch(setActiveTooltip(null))}
                style={{ cursor: 'pointer' }}
            >
                <rect
                    x={x} y={y} width={width} height={height}
                    style={{
                        fill: calculatedFill.hex(),
                        stroke: isVisible ? hoverStrokeColor : strokeTextColor,
                        strokeWidth: isVisible ? 3 : 1,
                        filter: isVisible ? 'drop-shadow(0 0 8px rgba(160, 240, 237, 0.7))' : 'none',
                        transition: 'all 0.2s ease-out',
                    }}
                />
                <foreignObject x={x + 4} y={y + 4} width={width - 8} height={height - 8}>
                    {renderCellContent({ name, value, width, height, textColor: strokeTextColor })}
                </foreignObject>
            </g>
        </Tippy>
    );
};

// --- Props ---

interface GenericTreemapProps {
    title: string;
    data: { name: string; value: number; }[];
    baseCategoryColor: string;
    // Render Props
    renderCellContent: (props: { name: string; value: number; width: number; height: number; textColor: string; }) => React.ReactNode;
    renderTooltipContent: (props: { name: string; value: number; }) => React.ReactNode;
}

// --- Main Component ---

const GenericTreemap: React.FC<GenericTreemapProps> = ({ title, data, baseCategoryColor, renderCellContent, renderTooltipContent }) => {
    if (!data) return null;

    const maxSkillValue = Math.max(...data.map(s => s.value), 0);

    return (
        <div className="bg-slate-800/50 p-4 rounded-lg shadow-lg flex flex-col">
            <h3 className="text-xl font-semibold mb-3 text-gray-300">{title}</h3>
            <div className="flex-1 w-full h-full">
                <ResponsiveContainer width="100%" height={250}>
                    <Treemap
                        data={data}
                        dataKey="value"
                        aspectRatio={2}
                        stroke="#fff"
                        fill={baseCategoryColor}
                        content={(props) => (
                            <CustomizedTreemapContent
                                {...props}
                                id={`${title}-${props.name}`} // Unique ID for tooltip
                                baseCategoryColor={baseCategoryColor}
                                maxSkillValue={maxSkillValue}
                                renderCellContent={renderCellContent}
                                renderTooltipContent={renderTooltipContent}
                            />
                        )}
                    />
                </ResponsiveContainer>
            </div>
        </div>
    );
};

export default GenericTreemap;
