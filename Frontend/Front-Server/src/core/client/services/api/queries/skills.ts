import { gql } from '@apollo/client';

export const GET_SKILL_CATEGORIES = gql`
  query GetSkillCategories {
    skillCategories {
      category
      skills
    }
  }
`;
