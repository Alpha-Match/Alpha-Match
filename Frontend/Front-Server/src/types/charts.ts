// 차트 관련 타입
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

/**
 * PieData - Recharts 파이 차트 데이터 구조
 * innerData와 outerData에서 사용
 */
export interface PieData {
    name: string;
    value: number;
    [key: string]: any; // Allow other properties
}