// Frontend/Front-Server/src/components/dashboard/GenericTreemap.tsx
import React, { useRef } from 'react';
import { ResponsiveContainer, Treemap } from 'recharts';
import Tippy from '@tippyjs/react';
import { useAppDispatch, useAppSelector } from '../../services/state/hooks';
import { setActiveTooltip } from '../../services/state/features/ui/uiSlice';
import chroma from 'chroma-js'; // chroma-js is still needed for the scale, which is fine.

/* ------------------------------------------------------------------
 * 서브 컴포넌트 Props
 * ------------------------------------------------------------------ */
interface CustomizedTreemapContentProps {
    /** Recharts에서 주입되는 기본 좌표/데이터 */
    depth: number; x: number; y: number; width: number; height: number;
    name: string; value: number;
    /** 색상 계산용 */
    baseCategoryColor: string;
    maxSkillValue: number;
    /** Redux tooltip 식별자 */
    id: string;
    /** Render Props */
    renderCellContent: (props: { name: string; value: number; width: number; height: number; textColor: string; }) => React.ReactNode;
    renderTooltipContent: (props: { name: string; value: number; }) => React.ReactNode;
}

/* ------------------------------------------------------------------
 * 커스텀 Treemap 셀
 * ------------------------------------------------------------------ */
const CustomizedTreemapContent: React.FC<CustomizedTreemapContentProps> = (props) => {
    const {
        id, x, y, width, height, name, value,
        baseCategoryColor, maxSkillValue,
        renderCellContent, renderTooltipContent
    } = props;

    const dispatch = useAppDispatch();
    const { activeTooltipId } = useAppSelector((state) => state.ui);

    const divRef = useRef<HTMLDivElement | null>(null);
    const isVisible = activeTooltipId === id;

    const treemapColorScale = chroma.scale([
        baseCategoryColor,
        chroma(baseCategoryColor).brighten(2).hex()
    ]).domain([maxSkillValue, 0]);

    const calculatedFill = treemapColorScale(value).hex();
    
    // Use CSS variables for theme-aware colors
    const textColor = 'rgb(var(--color-text-primary))';
    const hoverStrokeColor = 'rgb(var(--color-primary-light))';


    return (
        <>
            <foreignObject x={x} y={y} width={width} height={height}>
                <div ref={divRef} />
            </foreignObject>

            <Tippy
                content={renderTooltipContent({ name, value })}
                visible={isVisible}
                placement="top"
                inertia
                appendTo={document.body}
                reference={divRef.current}
            />

            <g
                onMouseEnter={() => dispatch(setActiveTooltip(id))}
                onMouseLeave={() => dispatch(setActiveTooltip(null))}
                style={{ cursor: 'pointer' }}
            >
                <rect
                    x={x}
                    y={y}
                    width={width}
                    height={height}
                    style={{
                        fill: calculatedFill,
                        stroke: isVisible ? hoverStrokeColor : 'rgb(var(--color-background))',
                        strokeWidth: isVisible ? 3 : 1,
                        transition: 'all 0.2s ease-out',
                    }}
                />

                <foreignObject
                    x={x + 4}
                    y={y + 4}
                    width={Math.max(0, width - 8)}
                    height={Math.max(0, height - 8)}
                >
                    {renderCellContent({
                        name,
                        value,
                        width,
                        height,
                        textColor: textColor,
                    })}
                </foreignObject>
            </g>
        </>
    );
};


/* ------------------------------------------------------------------
 * 메인 컴포넌트 Props
 * ------------------------------------------------------------------ */
interface GenericTreemapProps {
    title: string;
    data: { name: string; value: number; }[];
    baseCategoryColor: string;
    // Render Props
    renderCellContent: CustomizedTreemapContentProps['renderCellContent'];
    renderTooltipContent: CustomizedTreemapContentProps['renderTooltipContent'];
}

/* ------------------------------------------------------------------
 * GenericTreemap 컴포넌트
 * ------------------------------------------------------------------ */
const GenericTreemap: React.FC<GenericTreemapProps> = ({ title, data, baseCategoryColor, renderCellContent, renderTooltipContent }) => {
    if (!data) return null;

    const maxSkillValue = Math.max(...data.map(s => s.value), 0);
    const sortedData = [...data].sort((a, b) => b.value - a.value);

    return (
        <div className="bg-panel-main p-4 rounded-lg shadow-lg flex flex-col">
            <h3 className="text-xl font-semibold mb-3 text-text-primary">{title}</h3>
            <div className="flex-1 w-full h-full">
                <ResponsiveContainer width="100%" height={250}>
                    <Treemap
                        data={sortedData}
                        dataKey="value"
                        aspectRatio={1}
                        stroke="rgb(var(--color-text-primary))"
                        fill={baseCategoryColor}
                        content={(props) => (
                            <CustomizedTreemapContent
                                {...props}
                                id={`${title}-${props.name}`} // 툴팁을 위한 고유 ID
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
