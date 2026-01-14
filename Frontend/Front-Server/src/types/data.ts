// 핵심 데이터 모델 인터페이스
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
    resumeLang?: string | null; // New field
    moreinfo?: string | null;   // New field
    lookingFor?: string | null; // New field
    skills: string[];
    createdAt?: string | null;  // New field
    updatedAt?: string | null;  // New field
}

export interface SimulationResponse {
    vectorVisualization: SkillMatch[];
    matches: MatchItem[];
}

export interface SkillMatch {
    skill: string;
    isCore: boolean;
    x: number;
    y: number;
}
