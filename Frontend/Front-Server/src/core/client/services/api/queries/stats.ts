import { gql } from '@apollo/client';

export const GET_SEARCH_STATISTICS = gql`
  query GetSearchStatistics($mode: UserMode!, $skills: [String!]!, $limit: Int) {
    searchStatistics(mode: $mode, skills: $skills, limit: $limit) {
      topSkills {
        skill
        count
        percentage
      }
      totalCount
    }
  }
`;