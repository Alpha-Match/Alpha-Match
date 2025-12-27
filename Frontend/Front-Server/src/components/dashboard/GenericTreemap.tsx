// Frontend/Front-Server/src/components/dashboard/GenericTreemap.tsx
import React, { useRef } from 'react';
import { ResponsiveContainer, Treemap } from 'recharts';
import chroma from 'chroma-js';
import Tippy from '@tippyjs/react';

import { useAppDispatch, useAppSelector } from '../../services/state/hooks';
import { setActiveTooltip } from '../../services/state/features/ui/uiSlice';


/* ------------------------------------------------------------------
 * Sub Component Props
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
 * Customized Treemap Cell
 * ------------------------------------------------------------------ */
const CustomizedTreemapContent: React.FC<CustomizedTreemapContentProps> = (props) => {
    const {
        id, x, y, width, height, name, value,
        baseCategoryColor, maxSkillValue,
        renderCellContent, renderTooltipContent
    } = props;

    const dispatch = useAppDispatch();
    const { activeTooltipId } = useAppSelector((state) => state.ui);

    /**
     * 🔑 핵심 포인트
     * - Tippy는 반드시 HTMLElement를 reference로 요구
     * - SVG <g> / <rect> 는 ref 대상이 아님
     * - foreignObject 내부 div를 tooltip 기준점으로 사용
     */
    const divRef = useRef<HTMLDivElement | null>(null);
    /** 현재 셀이 활성 tooltip인지 여부 */
    const isVisible = activeTooltipId === id;
    /* -----------------------------
     * Color 계산
     * ----------------------------- */
    const treemapColorScale = chroma.scale([
        baseCategoryColor,
        chroma(baseCategoryColor).brighten(2).hex()
    ]).domain([maxSkillValue, 0]);

    const calculatedFill = treemapColorScale(value);
    const hoverStrokeColor = chroma(baseCategoryColor).brighten(2).hex();
    const strokeTextColor = chroma(baseCategoryColor).luminance() > 0.5 ? '#333' : '#eee';

    return (
        <>
            {/* =========================================================
             * Tooltip 기준점 (HTML Element)
             * =========================================================
             * - foreignObject 안에 div를 두어 HTMLElement 확보
             * - 실제 화면에는 보이지 않지만 tooltip positioning 기준
             */}
            <foreignObject x={x} y={y} width={width} height={height}>
                <div ref={divRef} />
            </foreignObject>

            {/* =========================================================
             * Tippy Tooltip
             * =========================================================
             * ❗ children로 SVG를 감싸지 않는다
             * ❗ reference prop을 통해 명시적으로 HTMLElement 지정
             */}
            <Tippy
                content={renderTooltipContent({ name, value })}
                visible={isVisible}
                placement="top"
                inertia
                appendTo={document.body}
                reference={divRef.current}
            />

            {/* =========================================================
             * 실제 Treemap SVG 렌더링
             * ========================================================= */}
            <g
                onMouseEnter={() => dispatch(setActiveTooltip(id))}
                onMouseLeave={() => dispatch(setActiveTooltip(null))}
                style={{ cursor: 'pointer' }}
            >
                {/* Background Rect */}
                <rect
                    x={x}
                    y={y}
                    width={width}
                    height={height}
                    style={{
                        fill: calculatedFill.hex(),
                        stroke: isVisible ? hoverStrokeColor : strokeTextColor,
                        strokeWidth: isVisible ? 3 : 1,
                        filter: isVisible
                            ? 'drop-shadow(0 0 8px rgba(160,240,237,0.7))'
                            : 'none',
                        transition: 'all 0.2s ease-out',
                    }}
                />

                {/* Cell Content */}
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
                        textColor: strokeTextColor,
                    })}
                </foreignObject>
            </g>
        </>
    );
};


/* ------------------------------------------------------------------
 * Main Component Props
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
 * GenericTreemap Component
 * ------------------------------------------------------------------ */
const GenericTreemap: React.FC<GenericTreemapProps> = ({ title, data, baseCategoryColor, renderCellContent, renderTooltipContent }) => {
    if (!data) return null;

    /** color scale 기준 최대값 */
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
