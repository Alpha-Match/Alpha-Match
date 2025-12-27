export enum UserMode {
  CANDIDATE = 'CANDIDATE',
  RECRUITER = 'RECRUITER',
}

export enum ExperienceLevel {
  JUNIOR = '0-2 Years',
  MID = '3-5 Years',
  SENIOR = '6-9 Years',
  LEAD = '10+ Years',
}

export interface Recruit {
  recruit_id: string;
  company_name: string;
  position: string;
  experience_years: number | null;
  primary_keyword: string | null;
  skills: string[];
  long_description: string;
  similarity?: number;
}

export interface Candidate {
  candidate_id: string;
  name: string;
  position_category: string;
  experience_years: number | null;
  skills: string[];
  resume_summary: string;
  similarity?: number;
}

export interface SkillMatch {
  skill: string;
  isCore: boolean;
  x: number;
  y: number;
}

export interface MatchItem {
  id: string;
  title: string;
  company: string; // Corresponds to 'subtitle' in the old interface
  score: number; // Corresponds to 'similarityScore' in the old interface
  skills: string[]; // Corresponds to 'tags' in the old interface
  experience?: number | null; // 경력 정보 추가
  description?: string;
  location?: string;
  salary?: string;
}

export interface SimulationResponse {
  vectorVisualization: SkillMatch[];
  matches: MatchItem[];
}

export interface DashboardSkill {
  skill: string;
  count: number;
}

export interface DashboardCategory {
  category: string;
  skills: DashboardSkill[];
}

export interface DashboardData {
  dashboardData: DashboardCategory[];
}

export interface DashboardVars {
  userMode: UserMode;
}