import { getSkillCategories, getDashboardData } from '../lib/server/api';
import { HomePageClient } from './_components/HomePage.client';
import { UserMode } from '../types';

/**
 * @file page.tsx
 * @description 메인 페이지 - Server Component
 *              서버에서 초기 데이터를 fetch하고 클라이언트 컴포넌트에 전달합니다.
 *              Next.js App Router의 Server Components 패턴을 활용합니다.
 * @version 2.1.0
 * @date 2025-12-30
 */
export default async function HomePage() {
  // 서버 사이드에서 병렬로 초기 데이터를 가져옵니다.
  const [
    initialSkillCategories,
    candidateDashboardData,
    recruiterDashboardData
  ] = await Promise.all([
    getSkillCategories(),
    getDashboardData(UserMode.CANDIDATE),
    getDashboardData(UserMode.RECRUITER)
  ]);

  const initialDashboardData = {
    [UserMode.CANDIDATE]: candidateDashboardData,
    [UserMode.RECRUITER]: recruiterDashboardData,
  };

  // 클라이언트 컴포넌트에 초기 데이터를 전달
  return (
    <HomePageClient
      initialSkillCategories={initialSkillCategories}
      initialDashboardData={initialDashboardData}
    />
  );
}
