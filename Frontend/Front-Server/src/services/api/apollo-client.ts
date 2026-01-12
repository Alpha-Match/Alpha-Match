import { ApolloClient, InMemoryCache, HttpLink, ApolloLink, from } from "@apollo/client";
import { onError } from "@apollo/client/link/error";
import { CombinedGraphQLErrors, ServerError } from '@apollo/client/errors';
import type { Store } from '@reduxjs/toolkit';
import type { showNotification } from '../state/features/notification/notificationSlice';

const GRAPHQL_ENDPOINT = process.env.INTERNAL_GRAPHQL_ENDPOINT!;
const API_TIMEOUT_MS = 20 * 1000; // 20 seconds

// Redux store will be injected
let reduxStore: Store | null = null;
let dispatchNotification: typeof showNotification | null = null;

export const setApolloStore = (store: Store, notificationAction: typeof showNotification) => {
  reduxStore = store;
  dispatchNotification = notificationAction;
};

const httpLink = new HttpLink({
  uri: GRAPHQL_ENDPOINT,
  fetchOptions: {
    signal: (() => {
      const controller = new AbortController();
      setTimeout(() => controller.abort(), API_TIMEOUT_MS);
      return controller.signal;
    })(),
  },
});

/**
 * 쿼리별 사용자 친화적 에러 메시지 매핑
 */
const getQuerySpecificError = (operationName: string | undefined, originalMessage: string): string => {
  if (!operationName) return originalMessage;

  const errorMap: Record<string, string> = {
    'SearchMatches': '검색 중 오류가 발생했습니다. 선택한 스킬과 경력을 확인해주세요.',
    'GetSkillCategories': '스킬 목록을 불러오는 중 오류가 발생했습니다.',
    'GetDashboardData': '대시보드 데이터를 불러오는 중 오류가 발생했습니다.',
    'GetRecruitDetail': '채용 공고 상세 정보를 불러올 수 없습니다.',
    'GetCandidateDetail': '후보자 상세 정보를 불러올 수 없습니다.',
  };

  return errorMap[operationName] || originalMessage;
};

const errorLink = onError(({ operation, error }) => {
  let userMessage = "An unexpected error occurred.";
  const operationName = operation.operationName;

  if (CombinedGraphQLErrors.is(error)) {
    // GraphQL validation errors and execution errors
    console.error('[GraphQL error]:', {
      operation: operationName,
      errors: error.errors,
    });

    const rawMessage = error.errors.map(e => e.message).join(' ');
    userMessage = getQuerySpecificError(operationName, rawMessage);

  } else if (ServerError.is(error)) {
    // Server-side HTTP errors (e.g., 5xx)
    console.error(`[Server error]:`, {
      operation: operationName,
      message: error.message,
      statusCode: error.statusCode,
    });
    userMessage = getQuerySpecificError(
      operationName,
      'Server is not responding. Please try again later.'
    );

  } else if (error) {
    // Generic network errors, including AbortError for timeouts
    console.error(`[Network error]:`, {
      operation: operationName,
      name: error.name,
      message: error.message,
    });

    if (error.name === 'AbortError') {
      userMessage = `Request timed out after ${API_TIMEOUT_MS / 1000} seconds. Please try again.`;
    } else {
      userMessage = getQuerySpecificError(
        operationName,
        'Server connection failed. Please check your network.'
      );
    }
  }

  // Dispatch to Redux store if available
  if (reduxStore && dispatchNotification) {
    reduxStore.dispatch(dispatchNotification({
      message: userMessage,
      type: 'error'
    }));
  } else {
    // Fallback to Custom Event (for backward compatibility)
    if (typeof document !== 'undefined') {
      document.dispatchEvent(new CustomEvent('show-notification', {
        detail: { message: userMessage, type: 'error' }
      }));
    }
  }
});

/**
 * 캐싱 전략 설정
 */
const cacheConfig = new InMemoryCache({
  typePolicies: {
    Query: {
      fields: {
        // searchMatches: 무한 스크롤 지원 (병합 정책)
        searchMatches: {
          keyArgs: ['mode', 'skills', 'experience'], // 캐시 키
          merge(existing, incoming, { args }) {
            if (!existing || args?.offset === 0) {
              // 첫 로드 또는 새 검색
              return incoming;
            }

            // 무한 스크롤: 기존 데이터 + 새 데이터 병합
            return {
              ...incoming,
              matches: [
                ...(existing.matches || []),
                ...(incoming.matches || []),
              ],
              // vectorVisualization은 덮어쓰기 (마지막 상태 유지)
              vectorVisualization: incoming.vectorVisualization,
            };
          },
        },
        // skillCategories: 한 번 로드하면 변경 없음 (앱 초기화용)
        skillCategories: {
          merge: false,
        },
        // dashboardData: userMode별로 캐싱
        dashboardData: {
          keyArgs: ['userMode'],
          merge: false,
        },
        // Detail 쿼리: ID별로 캐싱
        getRecruit: {
          read(existing, { args, toReference }) {
            return existing || toReference({
              __typename: 'RecruitDetail',
              id: args?.id,
            });
          },
        },
        getCandidate: {
          read(existing, { args, toReference }) {
            return existing || toReference({
              __typename: 'CandidateDetail',
              id: args?.id,
            });
          },
        },
      },
    },
  },
});

// Export a factory function that creates a new client instance
export const makeClient = () => {
    return new ApolloClient({
        link: ApolloLink.from([errorLink, httpLink]),
        cache: cacheConfig,
        ssrMode: typeof window === 'undefined',
        defaultOptions: {
          watchQuery: {
            fetchPolicy: 'cache-and-network',
            errorPolicy: 'all',
          },
          query: {
            fetchPolicy: 'network-only',
            errorPolicy: 'all',
          },
        },
    });
};

