/**
 * @file core/server/api.ts
 * @description Server-side API 함수들 (Server Components에서만 사용)
 *              클라이언트 번들에 포함되지 않으며, 서버에서만 실행됩니다.
 * @version 1.0.0
 * @date 2025-12-30
 */

import {SkillCategory} from '@/types';
import {GET_SKILL_CATEGORIES} from '@/core/client/services/api/queries/skills';
import {GET_DASHBOARD_DATA} from '@/core/client/services/api/queries/dashboard';

const GRAPHQL_ENDPOINT = process.env.NEXT_PUBLIC_GRAPHQL_ENDPOINT!;

/**
 * Server-side에서 스킬 카테고리를 가져옵니다.
 *
 * Next.js의 fetch는 기본적으로 캐싱됩니다:
 * - 개발 환경: 캐시 없음
 * - 프로덕션: 기본 캐시 (revalidate 옵션으로 제어 가능)
 */
export async function getSkillCategories(): Promise<SkillCategory[]> {
  try {
    const response = await fetch(GRAPHQL_ENDPOINT, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
      },
      body: JSON.stringify({
        query: GET_SKILL_CATEGORIES.loc?.source.body,
      }),
      // Next.js 15+ fetch 옵션
      next: {
        revalidate: 3600, // 1시간마다 재검증 (스킬 카테고리는 자주 변하지 않음)
      },
    });

    if (!response.ok) {
      throw new Error(`HTTP error! status: ${response.status}`);
    }

    const result = await response.json();

    if (result.errors) {
      console.error('[Server] GraphQL Errors:', result.errors);
      throw new Error(result.errors[0]?.message || 'GraphQL query failed');
    }

    return result.data?.skillCategories || [];
  } catch (error) {
    console.error('[Server] Failed to fetch skill categories:', error);
    // 서버 사이드 에러는 빈 배열 반환 (클라이언트에서 재시도 가능)
    return [];
  }
}

/**
 * Server-side에서 대시보드 데이터를 가져옵니다.
 *
 * @param userMode - CANDIDATE | RECRUITER
 */
export async function getDashboardData(userMode: string) {
  try {
    const response = await fetch(GRAPHQL_ENDPOINT, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
      },
      body: JSON.stringify({
        query: GET_DASHBOARD_DATA.loc?.source.body,
        variables: { userMode },
      }),
      next: {
        revalidate: 300, // 5분마다 재검증 (대시보드는 상대적으로 동적)
      },
    });

    if (!response.ok) {
      throw new Error(`HTTP error! status: ${response.status}`);
    }

    const result = await response.json();

    if (result.errors) {
      console.error('[Server] GraphQL Errors:', result.errors);
      throw new Error(result.errors[0]?.message || 'GraphQL query failed');
    }

    return result.data?.dashboardData || [];
  } catch (error) {
    console.error('[Server] Failed to fetch dashboard data:', error);
    return [];
  }
}
