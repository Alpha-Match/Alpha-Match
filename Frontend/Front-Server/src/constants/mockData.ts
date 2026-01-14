import { Recruit, Candidate, ExperienceLevel } from '../types';

/**
 * @file mockData.ts
 * @description Frontend 개발 및 테스트를 위한 Mock 데이터
 *              운영체제: Windows
 */

// 채용 공고 Mock 데이터
export const MOCK_RECRUITS: Recruit[] = [
  {
    recruit_id: 'a1b2c3d4-e5f6-7890-1234-567890abcdef',
    company_name: '알파독 주식회사',
    position: '시니어 백엔드 엔지니어 (Go/Rust)',
    experience_years: 8, // 경력 8년
    primary_keyword: 'Go',
    skills: ['Go', 'Rust', 'Kubernetes', 'AWS', 'PostgreSQL', 'Microservices'],
    long_description: `
### 담당 업무 (Responsibilities)
- Go 및 Rust를 사용하여 고성능 백엔드 서비스 설계 및 개발.
- AWS 클라우드 인프라 구축 및 유지보수.
- 컨테이너 오케스트레이션을 위한 Kubernetes 작업.
- 주니어 엔지니어 멘토링 및 코드 리뷰 주도.

### 자격 요건 (Qualifications)
- 백엔드 개발 경력 8년 이상.
- Go 또는 Rust에 대한 뛰어난 숙련도.
- 분산 시스템 및 마이크로 서비스 아키텍처에 대한 깊은 이해.
`,
    similarity: 0.92, // 유사도 점수 (Mock 데이터용)
  },
  {
    recruit_id: 'b2c3d4e5-f6a7-8901-2345-67890abcdef1',
    company_name: '베타캣 솔루션즈',
    position: '프론트엔드 개발자 (React)',
    experience_years: 3, // 경력 3년
    primary_keyword: 'React',
    skills: ['React', 'TypeScript', 'Next.js', 'GraphQL', 'Redux'],
    long_description: `
### 역할 소개 (About the Role)
저희는 열정적인 프론트엔드 개발자를 찾고 있습니다. React와 TypeScript를 사용하여 주요 제품의 아름답고 성능 좋은 사용자 인터페이스를 구축하는 역할을 담당합니다.

### 요구 사항 (Requirements)
- React 개발 경력 3년 이상.
- TypeScript 및 최신 프론트엔드 빌드 도구에 대한 확실한 이해.
- Redux 또는 Zustand와 같은 상태 관리 라이브러리 경험.
- GraphQL 경험 우대.
`,
    similarity: 0.88, // 유사도 점수 (Mock 데이터용)
  },
  {
    recruit_id: 'c3d4e5f6-a7b8-9012-3456-7890abcdef2',
    company_name: '감마레이 AI',
    position: 'AI/ML 엔지니어',
    experience_years: 5, // 경력 5년
    primary_keyword: 'Python',
    skills: ['Python', 'PyTorch', 'TensorFlow', 'FastAPI', 'Docker'],
    long_description: `
감마레이 AI의 AI/ML 엔지니어로서, 차세대 추천 엔진 구축의 선두에 서게 될 것입니다. 대규모 데이터셋과 최첨단 머신러닝 모델을 다루게 됩니다.

### 주요 업무 (What you'll do)
- 다양한 작업에 대한 ML 모델 연구 및 구현.
- FastAPI 및 Docker를 사용하여 확장 가능한 서비스로 모델 배포.
- 데이터 엔지니어링 팀과 협력하여 견고한 데이터 파이프라인 구축.
`,
    similarity: 0.85, // 유사도 점수 (Mock 데이터용)
  },
  {
    recruit_id: 'd4e5f6a7-b8c9-0123-4567-890abcdef3',
    company_name: '델타포스 DB',
    position: 'DevOps 엔지니어',
    experience_years: null, // 신입/주니어 포지션 (경력 무관)
    primary_keyword: 'Kubernetes',
    skills: ['Docker', 'Kubernetes', 'Terraform', 'CI/CD', 'Prometheus'],
    long_description: `
인프라 자동화 및 확장을 도울 의욕적인 DevOps 엔지니어를 찾고 있습니다. 클라우드 네이티브 기술에서 기술을 성장시키고자 하는 분에게 좋은 기회입니다. 사전 경험은 필수는 아니지만, 배우고자 하는 강한 의지가 중요합니다.

### 주요 기술 (Key Technologies)
- Docker & Kubernetes
- Infrastructure as Code를 위한 Terraform
- CI/CD를 위한 Jenkins / GitLab
`,
    similarity: 0.76, // 유사도 점수 (Mock 데이터용)
  },
];

// 지원자 Mock 데이터
export const MOCK_CANDIDATES: Candidate[] = [
    {
        candidate_id: 'cand-1',
        name: '김민준',
        position_category: 'Backend',
        experience_years: 5,
        skills: ['Java', 'Spring Boot', 'JPA', 'MySQL', 'Redis', 'Kafka'],
        resume_summary: '5년차 백엔드 개발자. 대용량 트래픽 처리 경험과 MSA 설계 및 구축 경험이 있습니다. Java와 Spring Framework에 능숙합니다.',
        similarity: 0.95,
    },
    {
        candidate_id: 'cand-2',
        name: '이서연',
        position_category: 'Frontend',
        experience_years: 2,
        skills: ['React', 'TypeScript', 'Redux', 'Jest', 'Webpack'],
        resume_summary: '2년차 프론트엔드 개발자. 사용자 경험 개선에 관심이 많으며, 반응형 웹 및 웹 접근성 표준을 준수하는 UI 개발에 자신 있습니다.',
        similarity: 0.91,
    },
    {
        candidate_id: 'cand-3',
        name: '박도윤',
        position_category: 'DevOps / Cloud',
        experience_years: 7,
        skills: ['AWS', 'Kubernetes', 'Docker', 'Terraform', 'Ansible', 'Prometheus'],
        resume_summary: 'CI/CD 파이프라인 구축 및 클라우드 인프라 자동화 전문가. AWS 환경에서의 서비스 운영 및 비용 최적화 경험이 풍부합니다.',
        similarity: 0.89,
    },
    {
        candidate_id: 'cand-4',
        name: '최지우',
        position_category: 'Machine Learning',
        experience_years: 4,
        skills: ['Python', 'TensorFlow', 'Keras', 'Scikit-learn', 'Pandas', 'Numpy'],
        resume_summary: '자연어 처리(NLP) 및 추천 시스템 모델링 경험을 보유한 4년차 머신러닝 엔지니어입니다. 최신 논문 구현 및 서비스 적용에 관심이 많습니다.',
        similarity: 0.82,
    }
];

// DefaultDashboard용 대시보드 Mock 데이터
export const DASHBOARD_MOCK_DATA = [
  {
    category: 'Backend',
    skills: [
      { skill: 'Java', count: 85 },
      { skill: 'Python', count: 70 },
      { skill: 'Go', count: 50 },
      { skill: 'Rust', count: 30 },
      { skill: 'Node.js', count: 65 },
      { skill: 'Spring Boot', count: 80 },
      { skill: 'Django', count: 45 },
      { skill: 'FastAPI', count: 55 },
      { skill: 'NestJS', count: 40 },
    ],
  },
  {
    category: 'Frontend',
    skills: [
      { skill: 'React', count: 95 },
      { skill: 'TypeScript', count: 88 },
      { skill: 'Vue', count: 40 },
      { skill: 'Next.js', count: 75 },
      { skill: 'Angular', count: 35 },
      { skill: 'Javascript', count: 85 },
    ],
  },
  {
    category: 'Machine Learning',
    skills: [
      { skill: 'Python', count: 110 },
      { skill: 'PyTorch', count: 90 },
      { skill: 'TensorFlow', count: 85 },
      { skill: 'Scikit-learn', count: 70 },
      { skill: 'Numpy', count: 95 },
      { skill: 'Pandas', count: 92 },
    ],
  },
  {
    category: 'DevOps / Cloud',
    skills: [
      { skill: 'Kubernetes', count: 80 },
      { skill: 'Docker', count: 90 },
      { skill: 'AWS', count: 78 },
      { skill: 'GCP', count: 60 },
      { skill: 'Terraform', count: 55 },
      { skill: 'Jenkins', count: 45 },
      { skill: 'GitLab CI', count: 48 },
    ],
  },
  {
    category: 'Database',
    skills: [
        { skill: 'PostgreSQL', count: 75 },
        { skill: 'MySQL', count: 70 },
        { skill: 'MongoDB', count: 65 },
        { skill: 'Redis', count: 80 },
        { skill: 'Oracle', count: 40 },
        { skill: 'Cassandra', count: 30 },
    ]
  },
  {
      category: 'Collaboration / Project Management',
      skills: [
          { skill: 'Jira', count: 85 },
          { skill: 'Confluence', count: 70 },
          { skill: 'Slack', count: 90 },
          { skill: 'Notion', count: 80 },
          { skill: 'Git', count: 95 },
          { skill: 'Github', count: 92 },
      ]
  }
];

// 검색 필터용 기술 스택 Mock 데이터
export const MOCK_TECH_STACKS = [
  "React", "TypeScript", "Node.js", "Python", "Django", "FastAPI",
  "Java", "Spring Boot", "Kotlin", "Swift", "Go", "Rust",
  "Docker", "Kubernetes", "AWS", "GCP", "PostgreSQL", "MongoDB",
  "Redis", "GraphQL", "Next.js", "Vue.js", "Angular"
];

// 검색 필터용 경력 레벨 Mock 데이터
export const MOCK_EXPERIENCE_LEVELS: ExperienceLevel[] = [
  ExperienceLevel.JUNIOR,
  ExperienceLevel.MID,
  ExperienceLevel.SENIOR,
  ExperienceLevel.LEAD,
];