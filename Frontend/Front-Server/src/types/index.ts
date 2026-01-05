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

/**
 * MatchItem - LIST 뷰용 매칭 결과
 * API-Server의 MatchItem 타입과 일치
 * Detail 정보는 별도 쿼리(getRecruit/getCandidate)로 조회
 */
export interface MatchItem {
  id: string;
  title: string;
  company: string;
  score: number;
  skills: string[];
  experience?: number | null;
}

/**
 * RecruitDetail - 채용 공고 상세 정보
 * API-Server의 RecruitDetail 타입과 일치
 */
export interface RecruitDetail {
  id: string;
  position: string;
  companyName: string;
  experienceYears?: number | null;
  primaryKeyword?: string | null;
  englishLevel?: string | null;
  skills: string[];
  description: string;
  publishedAt?: string | null;
}

/**
 * CandidateDetail - 후보자 상세 정보
 * API-Server의 CandidateDetail 타입과 일치
 */
export interface CandidateDetail {
  id: string;
  positionCategory: string;
  experienceYears?: number | null;
  originalResume?: string | null;
  skills: string[];
  description: string;
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

/**
 * SkillCategory - 스킬 카테고리 정보
 * API-Server의 SkillCategory 타입과 일치
 */
export interface SkillCategory {
  category: string;
  skills: string[];
}

/**
 * CategoryMatchDistribution - 카테고리별 매칭 분포 (Pie Chart 데이터)
 * API-Server의 CategoryMatchDistribution 타입과 일치
 * 예: [Java, Spring Boot, MySQL] → Backend 66%, Database 33%
 */
export interface CategoryMatchDistribution {
  category: string;
  percentage: number;
  matchedSkills: string[];
  skillCount: number;
}

/**
 * SkillCompetencyMatch - 역량 매칭도 분석 결과
 * API-Server의 SkillCompetencyMatch 타입과 일치
 * 검색 기술 vs 대상(공고/이력서) 기술 비교
 */
export interface SkillCompetencyMatch {
  matchedSkills: string[];      // 교집합 (공통 스킬)
  missingSkills: string[];      // 부족한 스킬 (target에만 있음)
  extraSkills: string[];        // 추가 스킬 (searched에만 있음)
  matchingPercentage: number;   // 매칭 비율 (0-100)
  competencyLevel: string;      // "High", "Medium", "Low"
  totalTargetSkills: number;    // 대상 전체 스킬 수
  totalSearchedSkills: number;  // 검색 전체 스킬 수
}