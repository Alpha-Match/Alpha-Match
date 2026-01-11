// Frontend/Front-Server/src/components/SkillIcon.tsx
/**
 * @file SkillIcon.tsx
 * @description 기술 스택 이름에 따라 src/assets/icons/skills/에서 SVG 아이콘을 동적으로 가져와 렌더링합니다.
 *              Next.js에서 SVG import는 { src, width, height } 형태의 객체를 반환하므로, <img> 태그를 사용합니다.
 *              일치하는 아이콘이 없으면 아무것도 렌더링하지 않습니다.
 *              운영체제: Windows
 */
import React from 'react';
import SKILL_ICONS from '../../assets/icons/skills'; // 생성된 아이콘 인덱스 파일

interface SkillIconProps {
  skill: string;
  className?: string;
}

export const SkillIcon: React.FC<SkillIconProps> = ({ skill, className = 'w-5 h-5' }) => {
  // 스킬 이름을 소문자로 변환하여 아이콘 맵의 키와 일치시킵니다.
  const formattedSkillName = skill.toLowerCase();
  const iconData = SKILL_ICONS[formattedSkillName];

  // 일치하는 아이콘 데이터가 없거나, src 속성이 없으면 null을 반환합니다.
  if (!iconData || !iconData.src) {
    return null;
  }

  // <IconComponent /> 대신 <img> 태그를 사용하여 아이콘의 src 경로를 참조합니다.
  return <img src={iconData.src} alt={`${skill} icon`} className={className} />;
};