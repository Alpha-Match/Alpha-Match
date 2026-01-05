import React from 'react';
import { ResponsiveContainer, RadarChart, PolarGrid, PolarAngleAxis, PolarRadiusAxis, Radar, Legend, Tooltip } from 'recharts';
import { useAppSelector } from '../../services/state/hooks';
import { UserMode, MatchItem, SkillMatch } from '../../types';
import { MapPin, DollarSign, Building, User, ChevronRight, BarChart3, Database } from 'lucide-react';
import { CANDIDATE_THEME_COLORS, RECRUITER_THEME_COLORS } from '../../constants';

interface VisualizationPanelProps {
  matches: MatchItem[];
  vectorData: SkillMatch[];
}

export const VisualizationPanel: React.FC<VisualizationPanelProps> = ({
  matches,
  vectorData,
}) => {
  const mode = useAppSelector((state) => state.ui.userMode);
  const { isInitial } = useAppSelector((state) => state.search[mode]);
  const isCandidate = mode === UserMode.CANDIDATE;
  
  const themeColors = isCandidate ? CANDIDATE_THEME_COLORS : RECRUITER_THEME_COLORS;
  const primaryThemeColor = themeColors[0];

  if (isInitial) {
    return (
      <div className="h-full flex flex-col items-center justify-center text-text-tertiary p-8 text-center bg-panel-main">
        <div className="bg-background p-6 rounded-full shadow-sm mb-4">
          <Database className="w-16 h-16 text-text-tertiary" />
        </div>
        <h3 className="text-xl font-semibold text-text-secondary mb-2">분석 준비 완료</h3>
        <p className="max-w-md mx-auto">
          좌측에서 조건을 선택하여 pgvector 시뮬레이션을 초기화하세요.
          프로필과 데이터베이스 간의 의미론적 유사성을 시각화합니다.
        </p>
      </div>
    );
  }

  return (
    <div className="h-full flex flex-col bg-background overflow-hidden">
      
      {/* 상단: 시각화 */}
      <div className="h-[40%] bg-panel-main border-b border-border p-4 shadow-sm flex flex-col relative">
        <div className="absolute top-4 left-6 z-10">
          <div className="inline-flex items-center gap-2 px-3 py-1 bg-panel-2 rounded-full border border-border">
            <BarChart3 className="w-4 h-4 text-text-tertiary" />
            <span className="text-xs font-bold text-text-secondary uppercase tracking-wide">벡터 유사도 분석</span>
          </div>
        </div>
        
        <div className="flex-1 w-full mt-4">
          <ResponsiveContainer width="100%" height="100%">
            <RadarChart cx="50%" cy="50%" outerRadius="70%" data={vectorData}>
              <PolarGrid stroke="rgb(var(--color-border))" />
              <PolarAngleAxis dataKey="skill" tick={{ fill: 'rgb(var(--color-text-secondary))', fontSize: 11 }} />
              <PolarRadiusAxis angle={30} domain={[0, 100]} tick={false} axisLine={false} />
              <Tooltip 
                contentStyle={{ borderRadius: '8px', border: 'none', boxShadow: '0 4px 6px -1px rgb(0 0 0 / 0.1)' }}
                itemStyle={{ fontSize: '12px', fontWeight: 600, color: 'rgb(var(--color-text-primary))' }}
                wrapperStyle={{ backgroundColor: 'rgb(var(--color-panel-main))' }}
              />
              <Radar
                name={isCandidate ? "구직자 프로필" : "직무 요구사항"}
                dataKey="userValue"
                stroke={primaryThemeColor}
                strokeWidth={2}
                fill={primaryThemeColor}
                fillOpacity={0.3}
              />
              <Radar
                name="시장 일치도"
                dataKey="marketValue"
                stroke="#10b981" // Green color for market match, can be themed if needed
                strokeWidth={2}
                fill="#10b981"
                fillOpacity={0.3}
              />
              <Legend iconType="circle" wrapperStyle={{ fontSize: '12px', paddingTop: '10px', color: 'rgb(var(--color-text-secondary))' }} />
            </RadarChart>
          </ResponsiveContainer>
        </div>
      </div>

      {/* 하단: 결과 목록 */}
      <div className="flex-1 flex flex-col overflow-hidden bg-background">
        <div className="px-6 py-3 bg-panel-main border-b border-border flex justify-between items-center">
          <h3 className="font-bold text-text-primary text-lg">
            상위 10개 {isCandidate ? "추천 채용 공고" : "매칭된 후보자 프로필"}
          </h3>
          <span className="text-xs bg-emerald-100 text-emerald-700 px-2 py-1 rounded-md font-medium">
            코사인 유사도 기반
          </span>
        </div>

        <div className="flex-1 overflow-y-auto p-4 space-y-4 custom-scrollbar">
          {matches.map((item) => (
            <div key={item.id} className="bg-panel-main rounded-xl p-5 border border-border/50 shadow-sm hover:shadow-md hover:border-primary transition-all duration-200 group cursor-pointer relative overflow-hidden">
              
              {/* 유사도 점수 지표 */}
              <div className="absolute right-0 top-0 bottom-0 w-1.5 bg-panel-2">
                <div 
                  className="absolute bottom-0 w-full transition-all duration-1000 ease-out"
                  style={{ 
                    height: `${item.score * 100}%`,
                    backgroundColor: item.score > 0.9 ? '#10b981' : item.score > 0.85 ? primaryThemeColor : '#f59e0b'
                  }}
                />
              </div>

              <div className="flex justify-between items-start pr-4">
                <div>
                  <h4 className="text-lg font-bold group-hover:text-primary transition-colors text-text-primary"
                    onMouseEnter={(e) => e.currentTarget.style.color = primaryThemeColor}
                    onMouseLeave={(e) => e.currentTarget.style.color = ''}
                  >
                    {item.title}
                  </h4>
                  <div className="flex items-center text-text-secondary text-sm mt-1 gap-2">
                    {isCandidate ? <Building className="w-3.5 h-3.5" /> : <User className="w-3.5 h-3.5" />}
                    <span className="font-medium">{item.company}</span>
                  </div>
                </div>
                
                <div className="text-right">
                  <div className="text-2xl font-black text-text-primary tracking-tight">
                    {(item.score * 100).toFixed(0)}%
                  </div>
                  <div className="text-[10px] text-text-tertiary uppercase font-bold tracking-wider">매치 점수</div>
                </div>
              </div>

              {/* 태그 */}
              <div className="flex flex-wrap gap-2 mt-4">
                {item.skills.map((tag: string, idx: number) => (
                  <span key={idx} className="px-2 py-1 bg-panel-2 text-text-secondary text-xs rounded font-medium border border-border">
                    {tag}
                  </span>
                ))}
              </div>

              {/* 푸터 메타 */}
              <div className="flex items-center gap-4 mt-4 pt-3 border-t border-border text-xs text-text-tertiary">
                {item.experience !== null && item.experience !== undefined && (
                  <span className="flex items-center gap-1">
                    <User className="w-3 h-3" /> {item.experience}년 이상
                  </span>
                )}
                <div className="ml-auto flex items-center gap-1 font-semibold text-text-secondary group-hover:text-primary">
                  상세 보기 <ChevronRight className="w-3 h-3" />
                </div>
              </div>
            </div>
          ))}
        </div>
      </div>
    </div>
  );
};
