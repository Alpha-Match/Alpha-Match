import { gql } from '@apollo/client';

export const SEARCH_MATCHES_QUERY = gql`
  query SearchMatches(
    $mode: UserMode!
    $skills: [String!]!
    $experience: String!
    $limit: Int!
    $offset: Int!
  ) {
    searchMatches(
      mode: $mode
      skills: $skills
      experience: $experience
      limit: $limit
      offset: $offset
    ) {
      matches {
        id
        title
        company
        score
        skills
        experience
      }
      vectorVisualization {
        skill
        isCore
        x
        y
      }
    }
  }
`;

export const GET_RECRUIT_DETAIL = gql`
  query GetRecruitDetail($id: ID!) {
    getRecruit(id: $id) {
      id
      position
      companyName
      experienceYears
      primaryKeyword
      englishLevel
      skills
      description
      publishedAt
    }
  }
`;

export const GET_CANDIDATE_DETAIL = gql`
  query GetCandidateDetail($id: ID!) {
    getCandidate(id: $id) {
      id
      positionCategory
      experienceYears
      originalResume
      skills
      description
    }
  }
`;

export const GET_CATEGORY_DISTRIBUTION = gql`
  query GetCategoryDistribution($skills: [String!]!) {
    getCategoryDistribution(skills: $skills) {
      category
      percentage
      matchedSkills
      skillCount
    }
  }
`;

export const GET_SKILL_COMPETENCY_MATCH = gql`
  query GetSkillCompetencyMatch(
    $mode: UserMode!
    $targetId: ID!
    $searchedSkills: [String!]!
  ) {
    getSkillCompetencyMatch(
      mode: $mode
      targetId: $targetId
      searchedSkills: $searchedSkills
    ) {
      matchedSkills
      missingSkills
      extraSkills
      matchingPercentage
      competencyLevel
      totalTargetSkills
      totalSearchedSkills
    }
  }
`;
