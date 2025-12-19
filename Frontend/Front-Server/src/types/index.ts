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
  description?: string;
  location?: string;
  salary?: string;
}

export interface SimulationResponse {
  vectorVisualization: SkillMatch[];
  matches: MatchItem[];
}
