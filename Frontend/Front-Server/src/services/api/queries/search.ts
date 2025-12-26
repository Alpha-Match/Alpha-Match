import { gql } from '@apollo/client';

export const SEARCH_MATCHES_QUERY = gql`
  query SearchMatches($mode: UserMode!, $skills: [String!], $experience: String) {
    searchMatches(mode: $mode, skills: $skills, experience: $experience) {
      matches {
        id
        title
        company
        score
        skills
        experience
        description
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
