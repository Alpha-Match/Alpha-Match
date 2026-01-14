// Frontend/Front-Server/src/components/dashboard/GenericTreemap.tsx
import React, { useRef } from 'react';
import { ResponsiveContainer, Treemap } from 'recharts';
import Tippy, { useSingleton } from '@tippyjs/react'; // Import useSingleton
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
    /** Render Props */
    renderCellContent: (props: { name: string; value: number; width: number; height: number; textColor: string; }) => React.ReactNode;
    renderTooltipContent: (props: { name: string; value: number; }) => React.ReactNode;
    // For Tippy singleton
    singleton: ReturnType<typeof useSingleton>[1];
}

/* ------------------------------------------------------------------
 * 커스텀 Treemap 셀
 * ------------------------------------------------------------------ */
const CustomizedTreemapContent: React.FC<CustomizedTreemapContentProps> = (props) => {
    const {
        x, y, width, height, name, value,
        baseCategoryColor, maxSkillValue,
        renderCellContent, renderTooltipContent,
        singleton // Receive singleton target
    } = props;

    const gRef = useRef<SVGGElement>(null); // Use ref directly on the <g> element

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
            <Tippy
                content={renderTooltipContent({ name, value })}
                placement="top"
                inertia
                appendTo={document.body}
                reference={gRef.current} // Reference the <g> element
                singleton={singleton} // Pass singleton target
                // delay is managed by the singleton source
            />

            <g
                ref={gRef} // Attach ref to the <g> element
                style={{ cursor: 'pointer' }}
                onMouseEnter={(e) => {
                    if (gRef.current) gRef.current.style.stroke = hoverStrokeColor;
                }}
                onMouseLeave={(e) => {
                    if (gRef.current) gRef.current.style.stroke = 'rgb(var(--color-background))';
                }}
            >
                <rect
                    x={x}
                    y={y}
                    width={width}
                    height={height}
                    style={{
                        fill: calculatedFill,
                        stroke: 'rgb(var(--color-background))', // Default stroke
                        strokeWidth: 1,
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
export const GenericTreemap: React.FC<GenericTreemapProps> = ({ title, data, baseCategoryColor, renderCellContent, renderTooltipContent }) => {
    if (!data) return null;

    const maxSkillValue = Math.max(...data.map(s => s.value), 0);
    const sortedData = [...data].sort((a, b) => b.value - a.value);

    const [source, target] = useSingleton(); // Create singleton instance

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
                                baseCategoryColor={baseCategoryColor}
                                maxSkillValue={maxSkillValue}
                                renderCellContent={renderCellContent}
                                renderTooltipContent={renderTooltipContent}
                                singleton={target} // Pass singleton target to each Tippy
                            />
                        )}
                    />
                </ResponsiveContainer>
            </div>
            {/* The Tippy singleton instance that controls all tooltips */}
            <Tippy singleton={source} delay={[100, 0]} />
        </div>
    );
};
