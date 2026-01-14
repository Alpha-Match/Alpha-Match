// 대시보드 관련 타입
export interface DashboardSkill {
    skill: string;
    count: number;
}

export interface DashboardCategory {
    category: string;
    skills: DashboardSkill[];
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
 * CompanyJobCount - 회사별 채용 공고 수
 * API-Server의 CompanyJobCount 타입과 일치
 * dashboard_request.txt #1: "Company_name 기준 공고 많은 기업 Top 10"
 */
export interface CompanyJobCount {
    companyName: string;
    jobCount: number;
}

/**
 * SkillFrequency - 스킬 빈도수
 * API-Server의 SkillFrequency 타입과 일치
 * dashboard_request.txt #2: "검색된 전체 채용 공고/이력서에 관한 주요 요구 기술 Top 15"
 */
export interface SkillFrequency {
    skill: string;
    count: number;
    percentage: number;  // 0-100
}
