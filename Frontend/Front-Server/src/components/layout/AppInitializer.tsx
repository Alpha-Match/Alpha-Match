'use client';

import { useEffect } from 'react';
import { useQuery } from '@apollo/client/react';
import { useAppDispatch } from '../../store/hooks';
import { setSkillCategories } from '../../store/features/search/searchSlice';
import { GET_SKILL_CATEGORIES } from '../../lib/graphql/queries';
import { TECH_STACKS_DEMO } from '../../constants/appConstants';

interface SkillCategoriesData {
  skillCategories: {
    category: string;
    skills: string[];
  }[];
}

// This component's sole purpose is to fetch initial data for the app
// and populate the Redux store. It renders no UI.
export const AppInitializer = () => {
  const dispatch = useAppDispatch();
  const { data, error } = useQuery<SkillCategoriesData>(GET_SKILL_CATEGORIES);

  useEffect(() => {
    if (data && data.skillCategories) {
      // On successful API call, flatten the structured data and populate the store
      const allSkills = data.skillCategories.flatMap(category => category.skills);
      dispatch(setSkillCategories(allSkills));
    } else if (error) {
      // If the API fails, log the error and use the fallback demo data
      console.error("Failed to fetch skill categories, using demo data.", error);
      dispatch(setSkillCategories(TECH_STACKS_DEMO));
    }
  }, [data, error, dispatch]);

  // This component does not render anything
  return null;
};
