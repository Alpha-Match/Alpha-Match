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
  const { isInitial } = useAppSelector((state) => state.search);
  const mode = useAppSelector((state) => state.ui.userMode);
  const isCandidate = mode === UserMode.CANDIDATE;
  
  const themeColors = isCandidate ? CANDIDATE_THEME_COLORS : RECRUITER_THEME_COLORS;
  const primaryThemeColor = themeColors[0];

  if (isInitial) {
    return (
      <div className="h-full flex flex-col items-center justify-center text-slate-400 p-8 text-center bg-slate-50/50">
        <div className="bg-white p-6 rounded-full shadow-sm mb-4">
          <Database className="w-16 h-16 text-slate-300" />
        </div>
        <h3 className="text-xl font-semibold text-slate-600 mb-2">Ready to Analyze</h3>
        <p className="max-w-md mx-auto">
          Select your criteria on the left to initialize the pgvector simulation. 
          We will visualize the semantic similarity between your profile and our database.
        </p>
      </div>
    );
  }

  return (
    <div className="h-full flex flex-col bg-slate-50 overflow-hidden">
      
      {/* Top: Visualization */}
      <div className="h-[40%] bg-white border-b border-slate-200 p-4 shadow-sm flex flex-col relative">
        <div className="absolute top-4 left-6 z-10">
          <div className="inline-flex items-center gap-2 px-3 py-1 bg-slate-100 rounded-full border border-slate-200">
            <BarChart3 className="w-4 h-4 text-slate-500" />
            <span className="text-xs font-bold text-slate-600 uppercase tracking-wide">Vector Similarity Analysis</span>
          </div>
        </div>
        
        <div className="flex-1 w-full mt-4">
          <ResponsiveContainer width="100%" height="100%">
            <RadarChart cx="50%" cy="50%" outerRadius="70%" data={vectorData}>
              <PolarGrid stroke="#e2e8f0" />
              <PolarAngleAxis dataKey="skill" tick={{ fill: '#64748b', fontSize: 11 }} />
              <PolarRadiusAxis angle={30} domain={[0, 100]} tick={false} axisLine={false} />
              <Tooltip 
                contentStyle={{ borderRadius: '8px', border: 'none', boxShadow: '0 4px 6px -1px rgb(0 0 0 / 0.1)' }}
                itemStyle={{ fontSize: '12px', fontWeight: 600 }}
              />
              <Radar
                name={isCandidate ? "Job Seeker Profile" : "Job Requirement"}
                dataKey="userValue"
                stroke={primaryThemeColor}
                strokeWidth={2}
                fill={primaryThemeColor}
                fillOpacity={0.3}
              />
              <Radar
                name="Market Match"
                dataKey="marketValue"
                stroke="#10b981"
                strokeWidth={2}
                fill="#10b981"
                fillOpacity={0.3}
              />
              <Legend iconType="circle" wrapperStyle={{ fontSize: '12px', paddingTop: '10px' }} />
            </RadarChart>
          </ResponsiveContainer>
        </div>
      </div>

      {/* Bottom: List Results */}
      <div className="flex-1 flex flex-col overflow-hidden bg-slate-50">
        <div className="px-6 py-3 bg-white border-b border-slate-200 flex justify-between items-center">
          <h3 className="font-bold text-slate-700 text-lg">
            Top 10 {isCandidate ? "Recommended Job Positions" : "Matched Candidate Profiles"}
          </h3>
          <span className="text-xs bg-emerald-100 text-emerald-700 px-2 py-1 rounded-md font-medium">
            Based on Cosine Distance
          </span>
        </div>

        <div className="flex-1 overflow-y-auto p-4 space-y-4">
          {matches.map((item) => (
            <div key={item.id} className="bg-white rounded-xl p-5 border border-slate-200 shadow-sm hover:shadow-md hover:border-slate-300 transition-all duration-200 group cursor-pointer relative overflow-hidden">
              
              {/* Similarity Score Indicator */}
              <div className="absolute right-0 top-0 bottom-0 w-1.5 bg-slate-100">
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
                  <h4 className="text-lg font-bold group-hover:text-slate-900 transition-colors text-slate-800" style={{'--hover-color': primaryThemeColor} as React.CSSProperties}
                    onMouseEnter={(e) => e.currentTarget.style.color = primaryThemeColor}
                    onMouseLeave={(e) => e.currentTarget.style.color = ''}
                  >
                    {item.title}
                  </h4>
                  <div className="flex items-center text-slate-500 text-sm mt-1 gap-2">
                    {isCandidate ? <Building className="w-3.5 h-3.5" /> : <User className="w-3.5 h-3.5" />}
                    <span className="font-medium">{item.company}</span>
                  </div>
                </div>
                
                <div className="text-right">
                  <div className="text-2xl font-black text-slate-800 tracking-tight">
                    {(item.score * 100).toFixed(0)}%
                  </div>
                  <div className="text-[10px] text-slate-400 uppercase font-bold tracking-wider">Match Score</div>
                </div>
              </div>

              <p className="text-slate-600 text-sm mt-3 leading-relaxed">
                {item.description}
              </p>

              {/* Tags */}
              <div className="flex flex-wrap gap-2 mt-4">
                {item.skills.map((tag: string, idx: number) => (
                  <span key={idx} className="px-2 py-1 bg-slate-100 text-slate-600 text-xs rounded font-medium border border-slate-200">
                    {tag}
                  </span>
                ))}
              </div>

              {/* Footer Meta */}
              <div className="flex items-center gap-4 mt-4 pt-3 border-t border-slate-100 text-xs text-slate-500">
                {item.location && (
                  <span className="flex items-center gap-1">
                    <MapPin className="w-3 h-3" /> {item.location}
                  </span>
                )}
                {item.salary && (
                  <span className="flex items-center gap-1">
                    <DollarSign className="w-3 h-3" /> {item.salary}
                  </span>
                )}
                <div className="ml-auto flex items-center gap-1 font-semibold text-slate-400 group-hover:text-slate-700">
                  View Details <ChevronRight className="w-3 h-3" />
                </div>
              </div>
            </div>
          ))}
        </div>
      </div>
    </div>
  );
};
