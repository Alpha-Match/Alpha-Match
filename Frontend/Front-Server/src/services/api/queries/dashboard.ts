import { gql } from '@apollo/client';

export const GET_DASHBOARD_DATA = gql`
  query GetDashboardData($userMode: UserMode!) {
    dashboardData(userMode: $userMode) {
      category
      skills {
        skill
        count
      }
    }
  }
`;

export const GET_TOP_COMPANIES = gql`
  query GetTopCompanies($limit: Int) {
    topCompanies(limit: $limit) {
      companyName
      jobCount
    }
  }
`;
